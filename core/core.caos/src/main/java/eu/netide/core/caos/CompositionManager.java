package eu.netide.core.caos;

import eu.netide.core.api.IBackendManager;
import eu.netide.core.api.IShimManager;
import eu.netide.core.api.IShimMessageListener;
import eu.netide.core.caos.composition.*;
import eu.netide.lib.netip.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of ICompositionManager
 *
 * Created by timvi on 25.06.2015.
 */
public class CompositionManager implements ICompositionManager, IShimMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(CompositionManager.class);

    // Settings
    private static String previousCompositionSpecificationXml = "";
    private String compositionSpecificationXml = "";
    private int maxModuleWaitSeconds = 60;

    // Fields
    private IShimManager shimManager;
    private IBackendManager backendManager;
    private CompositionSpecification compositionSpecification = new CompositionSpecification();
    private final Semaphore csLock = new Semaphore(1);
    private boolean correctlyConfigured = false;


    public void Start() {
        logger.debug("CompositionManager started.");
    }

    public void Stop() {
        logger.debug("CompositionManager stopped.");
    }

    /**
     * Waits for required modules to connect to the backend
     *
     * @throws TimeoutException Occurs when after maxModuleWaitSeconds not all required modules are connected
     */
    private void Reconfigure() throws TimeoutException {
        // wait for required modules to connect for at most maxModuleWaitSeconds
        long modulesUnconnected = compositionSpecification.getModules().size();
        int waitCount = 0;
        do {
            logger.info("Waiting for required modules to connect, " + modulesUnconnected + " left...");
            modulesUnconnected = compositionSpecification.getModules().stream().filter(m -> backendManager.getModules().anyMatch(mn -> mn.equals(m.getId()))).count();
            if (modulesUnconnected > 0) {
                try {
                    Thread.sleep(1000);
                    waitCount++;
                    if (waitCount >= maxModuleWaitSeconds) {
                        throw new TimeoutException("Required modules did not connect.");
                    }
                } catch (InterruptedException e) {
                    logger.error("Interrupted while waiting for required modules to connect.", e);
                }
            }
        } while (modulesUnconnected > 0);
        correctlyConfigured = true;
    }

    @Override
    public void OnShimMessage(Message message, String originId) {
        logger.debug("CompositionManager received message from shim: " + new String(message.getPayload()));
        try {
            csLock.acquire(); // can only handle when not reconfiguring
            if (correctlyConfigured) {
                ExecutionFlowStatus status = new ExecutionFlowStatus(message);
                for (ExecutionFlowNode efn : compositionSpecification.getComposition()) {
                    ExecutionResult result = efn.Execute(status);
                    // TODO handle results
                }
            } else {
                logger.error("Could not handle incoming message due to configuration error.", message);
            }
        } catch (InterruptedException e) {
            logger.error("InterruptedException occurred while handling shim message.", e);
        } finally {
            csLock.release();
        }
    }

    public void setShimManager(IShimManager manager) {
        shimManager = manager;
    }

    public IShimManager getShimManager() {
        return shimManager;
    }

    public void setBackendManager(IBackendManager manager) {
        backendManager = manager;
    }

    public IBackendManager getBackendManager() {
        return backendManager;
    }

    public String getCompositionSpecificationXml() {
        return compositionSpecificationXml;
    }

    public void setCompositionSpecificationXml(String compositionSpecificationXml) {
        this.compositionSpecificationXml = compositionSpecificationXml;
        try {
            csLock.acquire();
            this.compositionSpecification = CompositionSpecificationLoader.Load(this.compositionSpecificationXml);
            logger.info("Accepted new CompositionSpecification '" + this.compositionSpecificationXml + "'.");
            // Accepted specification can be used as fallback
            previousCompositionSpecificationXml = this.compositionSpecificationXml;
            Reconfigure();
        } catch (InterruptedException | JAXBException ex) {
            if (compositionSpecificationXml.trim().isEmpty()) {
                // prevent stacktrace on empty specifications
                logger.error("Unable to read new CompositionSpecification. Provided XML was empty.");
            } else {
                logger.error("Unable to read new CompositionSpecification. Provided XML was '" + this.compositionSpecificationXml + "'.", ex);
            }
            logger.warn("Reusing previous specification '" + previousCompositionSpecificationXml + "'.");
            this.compositionSpecificationXml = previousCompositionSpecificationXml;
        } catch (TimeoutException ex) {
            logger.error("TimeoutException occurred.", ex);
        } finally {
            csLock.release();
        }
    }

    public int getMaxModuleWaitSeconds() {
        return this.maxModuleWaitSeconds;
    }

    public void setMaxModuleWaitSeconds(int value) {
        this.maxModuleWaitSeconds = value;
    }
}
