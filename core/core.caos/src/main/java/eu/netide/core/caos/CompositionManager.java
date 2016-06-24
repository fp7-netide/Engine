package eu.netide.core.caos;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.core.caos.composition.CompositionSpecification;
import eu.netide.core.caos.composition.CompositionSpecificationLoader;
import eu.netide.core.caos.composition.ExecutionFlowStatus;
import eu.netide.core.caos.composition.Module;
import eu.netide.core.caos.execution.FlowExecutors;
import eu.netide.lib.netip.Message;
import eu.netide.lib.netip.OpenFlowMessage;
import org.projectfloodlight.openflow.protocol.OFEchoReply;
import org.projectfloodlight.openflow.protocol.OFEchoRequest;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFFeaturesRequest;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Implementation of ICompositionManager
 * <p>
 * Created by timvi on 25.06.2015.
 */
public class CompositionManager implements ICompositionManager {

    private static final Logger logger = LoggerFactory.getLogger(CompositionManager.class);
    private ExecutorService pool = Executors.newSingleThreadExecutor();

    // Settings
    private static String previousCompositionSpecificationXml = "";
    private String compositionSpecificationXml = "";
    private int maxModuleWaitSeconds = 600;
    private boolean bypassUnsupportedMessages = true;

    // Fields
    private IShimManager shimManager;
    private IBackendManager backendManager;
    private CompositionSpecification compositionSpecification = new CompositionSpecification();
    private final Semaphore csLock = new Semaphore(1);
    private volatile boolean correctlyConfigured = false;
    private Future<?> reconfigurationFuture;
    private static ExecutionFlowStatus lastStatus = null;
    private String compositionNotReadyReason = "No composition file loaded";

    /**
     * Called by blueprint on startup.
     */
    public void Start() {
        logger.debug("CompositionManager started.");
    }

    /**
     * Called by blueprint on shutdown.
     */
    public void Stop() {
        logger.debug("CompositionManager stopped.");
    }

    /**
     * Waits for required modules to connect to the backend
     *
     * @throws TimeoutException Occurs when after maxModuleWaitSeconds not all required modules are connected
     */
    private void ReconfigureAsync() throws TimeoutException {
        reconfigurationFuture = pool.submit(() -> {
            try {
                compositionNotReadyReason = "Composition loaded. Still waiting for modules to connect";
                // wait for required modules to connect for at most maxModuleWaitSeconds
                List<Module> modulesUnconnected = compositionSpecification.getModules();
                logger.info("Waiting for required modules to connect, " + modulesUnconnected + " left...");
                int waitCount = 0;
                do {
                    modulesUnconnected = compositionSpecification.getModules().stream().filter(m -> !backendManager.getModules().anyMatch(mn -> mn.equals(m.getId()))).collect(Collectors.toList());
                    if (modulesUnconnected.size() > 0) {
                        try {
                            Thread.sleep(2000);
                            waitCount++;
                            if (waitCount % 10 == 0) {
                                logger.info((maxModuleWaitSeconds - waitCount) + " seconds remaining for " + modulesUnconnected + " module(s) to connect...");
                                compositionNotReadyReason = "Waiting for modules to connect. Missing" + modulesUnconnected.stream().map(Object::toString).collect(Collectors.joining(","));
                            }
                            if (waitCount >= maxModuleWaitSeconds) {
                                compositionNotReadyReason = "Required modules did not connect. Missing" + modulesUnconnected.stream().map(Object::toString).collect(Collectors.joining(","));
                                throw new TimeoutException(compositionNotReadyReason);
                            }
                        } catch (InterruptedException e) {
                            compositionNotReadyReason = "Interrupted while waiting for required modules to connect.";
                            logger.error("Interrupted while waiting for required modules to connect.", e);
                            return;
                        } catch (TimeoutException e) {
                            compositionNotReadyReason = "Reconfiguration unsuccessful: timed out with " + modulesUnconnected.size() + " unconnected modules: " +
                                    modulesUnconnected.stream().map(Object::toString).collect(Collectors.joining(","));
                            logger.error(compositionNotReadyReason, e);
                            return;
                        }
                    }
                } while (modulesUnconnected.size() > 0 && !Thread.interrupted());
                correctlyConfigured = true;
                logger.info("All required modules connected. Reconfiguration successful.");
            } catch (Exception ex) {
                compositionNotReadyReason = "Error while reconfiguring.";
                logger.error("Error while reconfiguring.", ex);
                correctlyConfigured = false;
            }
        });
    }

    public List<Message> processShimMessage(Message message, String originId) {
        logger.info("CompositionManager received message from shim: " + message.toString());
        try {
            int compQueueLen = csLock.getQueueLength();
            if (compQueueLen > 10) {
                logger.warn(String.format("%d outstanding compositions", compQueueLen));
            } else {
                logger.debug(String.format("%d outstanding compositions", compQueueLen));
            }

            ExecutionFlowStatus status = new ExecutionFlowStatus(message);

            if (compQueueLen >= 1) {
                for (Module module : compositionSpecification.getModules()) {
                    if (!module.getFenceSupport()) {
                        int mId = getBackendManager().getModuleId(module.getId());
                        getBackendManager().markModuleAllOutstandingRequestsAsFinished(mId);
                    }
                }
            }
            csLock.acquire(); // can only handle when not reconfiguring
            if (correctlyConfigured) {
                status = FlowExecutors.SEQUENTIAL.executeFlow(status, compositionSpecification.getComposition().stream(), shimManager, backendManager);

                final int[] numMessages = {0};
                status.getResultMessages().forEach((aLong, messages) -> numMessages[0] += messages.size());
                logger.info("Composition finished {} messages for {} switches", numMessages, status.getResultMessages().size());

                List<Message> results = new ArrayList<Message>();
                status.getResultMessages().values().forEach(results::addAll);
                return results;
            } else {
                logger.error("Could not handle incoming message due to configuration error {}: {}", compositionNotReadyReason, message);
            }
        } catch (UnsupportedOperationException e) {
            if (bypassUnsupportedMessages) {
                boolean warn=true;
                if (message instanceof OpenFlowMessage) {
                    OFMessage openFlowMessage = ((OpenFlowMessage) message).getOfMessage();
                    if (openFlowMessage instanceof OFEchoRequest ||
                            openFlowMessage instanceof OFEchoReply)
                        warn = false;
                    else if (openFlowMessage instanceof OFFeaturesReply ||
                            openFlowMessage instanceof OFFeaturesRequest)
                        warn =false;
                }

                if (warn)
                    logger.warn("Received unsupported message for composition, attempting to relay instead.", e);

                try {
                    if (message.getHeader().getModuleId() == 0) {
                        logger.info("Message ID is 0 relaying to ALL backends");
                        backendManager.sendMessageAllBackends(message);
                    } else {
                        backendManager.sendMessage(message);
                    }
                } catch (Throwable ex) {
                    logger.error("Could not relay unsupported message.", ex);
                }
            } else {
                logger.error("Received unsupported message for composition, bypass is not activated.", e);
            }
        } catch (Throwable e) {
            logger.error("An exception occurred while handling shim message.", e);
        } finally {
            csLock.release();
        }
        return null;
    }

    /**
     * Sets the shim manager.
     *
     * @param manager the manager
     */
    public void setShimManager(IShimManager manager) {
        shimManager = manager;
        logger.info("ShimManager set.");
    }

    /**
     * Gets the shim manager.
     *
     * @return the shim manager
     */
    public IShimManager getShimManager() {
        return shimManager;
    }

    /**
     * Sets the backend manager.
     *
     * @param manager the manager
     */
    public void setBackendManager(IBackendManager manager) {
        backendManager = manager;
        logger.info("BackendManager set.");
    }

    /**
     * Gets the backend manager.
     *
     * @return the backend manager
     */
    public IBackendManager getBackendManager() {
        return backendManager;
    }

    /**
     * Gets the current composition specification as XML string.
     *
     * @return composition specification xml
     */
    public String getCompositionSpecificationXml() {
        return compositionSpecificationXml;
    }

    /**
     * Sets the composition specification and triggers a reconfiguration if it was loaded successfully.
     *
     * @param compositionSpecificationXml The composition specification as XML string.
     */
    public void setCompositionSpecificationXml(String compositionSpecificationXml) {
        this.compositionSpecificationXml = compositionSpecificationXml;
        try {
            csLock.acquire();
            correctlyConfigured = false;
            this.compositionSpecification = CompositionSpecificationLoader.Load(this.compositionSpecificationXml);
            logger.info("Accepted new CompositionSpecification '" + this.compositionSpecificationXml + "'.");
            // Accepted specification can be used as fallback
            previousCompositionSpecificationXml = this.compositionSpecificationXml;
            if (reconfigurationFuture != null) {
                // abort running reconfiguration
                reconfigurationFuture.cancel(true);
            }
            ReconfigureAsync();
        } catch (InterruptedException | JAXBException ex) {
            if (compositionSpecificationXml.trim().isEmpty()) {
                // prevent stacktrace on empty specifications
                logger.error("Unable to read new CompositionSpecification. Provided XML was empty.");
            } else {
                logger.error("Unable to read new CompositionSpecification. Provided XML was '" + this.compositionSpecificationXml + "'.", ex);
            }
            logger.warn("Reusing previous specification '" + previousCompositionSpecificationXml + "'.");
            this.compositionSpecificationXml = previousCompositionSpecificationXml;
            if (reconfigurationFuture == null) {
                correctlyConfigured = true;
            }
        } catch (TimeoutException ex) {
            logger.error("TimeoutException occurred.", ex);
        } finally {
            csLock.release();
            logger.info("Set composition finished.");
        }
    }

    /**
     * Gets the maximum wait time for module connections in seconds.
     *
     * @return The maximum wait time in seconds.
     */
    public int getMaxModuleWaitSeconds() {
        return this.maxModuleWaitSeconds;
    }

    /**
     * Sets the maximum wait time for module connections in seconds.
     *
     * @param value The new maximum wait time in seconds.
     */
    public void setMaxModuleWaitSeconds(int value) {
        this.maxModuleWaitSeconds = value;
    }

    /**
     * Gets relay unsupported messages.
     *
     * @return the relay unsupported messages
     */
    public boolean getBypassUnsupportedMessages() {
        return this.bypassUnsupportedMessages;
    }

    /**
     * Sets relay unsupported messages.
     *
     * @param bypassUnsupportedMessages the relay unsupported messages
     */
    public void setBypassUnsupportedMessages(boolean bypassUnsupportedMessages) {
        this.bypassUnsupportedMessages = bypassUnsupportedMessages;
        logger.info("Unsupported message bypass " + (bypassUnsupportedMessages ? "activated." : "deactivated."));
    }
}
