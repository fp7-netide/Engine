package eu.netide.core.management;

import eu.netide.core.api.IConnectorListener;
import eu.netide.core.api.IManagementConnector;
import eu.netide.core.api.IManagementHandler;
import eu.netide.core.api.IManagementMessageListener;
import eu.netide.lib.netip.*;
import org.apache.karaf.config.core.ConfigMBean;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Management handler.
 *
 * Created by timvi on 06.08.2015.
 */
public class ManagementHandler implements IConnectorListener, IManagementHandler {

    private static final Logger logger = LoggerFactory.getLogger(ManagementHandler.class);

    private IManagementConnector connector;
    private List<IManagementMessageListener> managementMessageListeners;
    private Semaphore listenerLock = new Semaphore(1);

    public void Start() throws IOException, MBeanException, MalformedObjectNameException {
        logger.info("ManagementHandler started.");
    }

    public void Stop() {
        logger.info("ManagementHandler stopped.");
    }

    @Override
    public void OnDataReceived(byte[] data, String originId) {
        Message message = NetIPConverter.parseConcreteMessage(data);
        try {
            listenerLock.acquire();
            for (IManagementMessageListener listener : managementMessageListeners) {
                listener.OnManagementMessage((ManagementMessage) message);
            }
        } catch (InterruptedException e) {
            logger.error("", e);
        } finally {
            listenerLock.release();
        }
        // TODO handle configuration changes properly
        try {
            JSONObject mm = new JSONObject(new String(message.getPayload()));
            String command = mm.getString("command");
            JSONObject parameters = mm.optJSONObject("parameters");

            switch (command) {
                case "ping":
                    handlePing(parameters, originId, message);
                    return;
                case "set-configvalue":
                    handleSetConfigValue(parameters, originId, message);
                    return;
                case "get-configvalue":
                    handleGetConfigValue(parameters, originId, message);
                    return;
                default:
                    // return error
                    JSONObject err = new JSONObject();
                    err = err.put("command", "error-result");
                    err = err.put("parameters", new JSONObject()
                            .put("reason", "unknown-command")
                            .put("origin", "command-handling")
                            .put("unknown-command", command));
                    sendMessage(err.toString(), originId, message);
                    return;
            }


        } catch (JSONException ex) {
            logger.error("", ex);
            JSONObject err = new JSONObject()
                    .put("command", "error-result")
                    .put("parameters", new JSONObject()
                            .put("reason", "exception")
                            .put("origin", "command-handling")
                            .put("exception-type", ex.getClass().getName())
                            .put("exception-message", ex.getMessage())
                            .put("exception-string", ex.toString()));
            sendMessage(err.toString(), originId, message);
        }
    }


    private void handlePing(JSONObject parameters, String originId, Message originalMessage) {
        JSONObject pong = new JSONObject().put("command", "pong");
        sendMessage(pong.toString(), originId, originalMessage);
    }

    private void handleSetConfigValue(JSONObject parameters, String originId, Message originalMessage) {
        String pid = parameters.optString("pid");
        String key = parameters.optString("key");
        String value = parameters.optString("value");
        if (pid == null || key == null || value == null) {
            JSONObject err = new JSONObject()
                    .put("command", "error-result")
                    .put("parameters", new JSONObject()
                            .put("reason", "missing-parameters")
                            .put("origin", "set-configvalue")
                            .put("missing-parameter", (pid == null ? "pid" : (key == null ? "key" : "value"))));
            sendMessage(err.toString(), originId, originalMessage);
        }
        try {
            setConfigurationValue(pid, key, value);
        } catch (IOException | MalformedObjectNameException | MBeanException ex) {
            logger.error("Exception in handleSetConfigValue", ex);
            JSONObject err = new JSONObject()
                    .put("command", "error-result")
                    .put("parameters", new JSONObject()
                            .put("reason", "exception")
                            .put("origin", "set-configvalue")
                            .put("exception-type", ex.getClass().getName())
                            .put("exception-message", ex.getMessage())
                            .put("exception-string", ex.toString()));
            sendMessage(err.toString(), originId, originalMessage);
        }
    }

    private boolean setConfigurationValue(String pid, String key, String value) throws IOException, MalformedObjectNameException, MBeanException {
        JMXConnector connector = null;
        try {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1099/karaf-root");
            Map<String, String[]> environment = new HashMap<>();
            String[] creds = {"karaf", "karaf"};
            environment.put(JMXConnector.CREDENTIALS, creds);
            connector = JMXConnectorFactory.connect(url, environment);
            MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();
            ObjectName systemMBean = new ObjectName("org.apache.karaf:type=config,name=root");
            ConfigMBean bean = JMX.newMBeanProxy(mbeanServer, systemMBean, ConfigMBean.class);
            bean.setProperty(pid, key, value);
            logger.info(String.format("Set '%s' on '%s' to value '%s'", key, pid, value));
            connector.close();
            return true;
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }
    }

    private String getConfigurationValue(String pid, String key) throws IOException, MalformedObjectNameException, MBeanException {
        JMXConnector connector = null;
        try {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1099/karaf-root");
            Map<String, String[]> environment = new HashMap<>();
            String[] creds = {"karaf", "karaf"};
            environment.put(JMXConnector.CREDENTIALS, creds);
            connector = JMXConnectorFactory.connect(url, environment);
            MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();
            ObjectName systemMBean = new ObjectName("org.apache.karaf:type=config,name=root");
            ConfigMBean bean = JMX.newMBeanProxy(mbeanServer, systemMBean, ConfigMBean.class);
            String value = bean.listProperties(pid).get(key);
            connector.close();
            return value;
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }
    }

    private void handleGetConfigValue(JSONObject parameters, String originId, Message originalMessage) {
        String pid = parameters.optString("pid");
        String key = parameters.optString("key");
        if (pid == null || key == null) {
            JSONObject err = new JSONObject()
                    .put("command", "error-result")
                    .put("parameters", new JSONObject()
                            .put("reason", "missing-parameters")
                            .put("origin", "get-configvalue")
                            .put("missing-parameter", (pid == null ? "pid" : "key")));
            sendMessage(err.toString(), originId, originalMessage);
        }
        try {
            String configValue = getConfigurationValue(pid, key);
            JSONObject reply = new JSONObject()
                    .put("command", "get-configvalue-result")
                    .put("parameters", new JSONObject()
                            .put("pid", pid)
                            .put("key", key)
                            .put("value", configValue));
            sendMessage(reply.toString(), originId, originalMessage);
        } catch (IOException | MalformedObjectNameException | MBeanException ex) {
            logger.error("", ex);
            JSONObject err = new JSONObject()
                    .put("command", "error-result")
                    .put("parameters", new JSONObject()
                            .put("reason", "exception")
                            .put("origin", "get-configvalue")
                            .put("exception-type", ex.getClass().getName())
                            .put("exception-message", ex.getMessage())
                            .put("exception-string", ex.toString()));
            sendMessage(err.toString(), originId, originalMessage);
        }
    }

    private void sendMessage(String json, String originId, Message originalMessage) {
        ManagementMessage message = new ManagementMessage();
        message.setPayloadString(json);
        MessageHeader h = NetIPUtils.StubHeaderFromPayload(message.getPayload());
        h.setMessageType(MessageType.MANAGEMENT);
        h.setModuleId(originalMessage.getHeader().getModuleId());
        h.setTransactionId(originalMessage.getHeader().getTransactionId());
        h.setDatapathId(originalMessage.getHeader().getDatapathId());
        message.setHeader(h);
        connector.SendData(message.toByteRepresentation(), originId);
    }

    public void setConnector(IManagementConnector connector) {
        this.connector = connector;
        connector.RegisterManagementListener(this);
    }

    public IManagementConnector getConnector() {
        return connector;
    }

    public List<IManagementMessageListener> getManagementMessageListeners() {
        return this.managementMessageListeners;
    }

    public void setManagementMessageListeners(List<IManagementMessageListener> managementMessageListeners) throws InterruptedException {
        listenerLock.acquire();
        this.managementMessageListeners = managementMessageListeners == null ? new ArrayList<>() : managementMessageListeners;
        listenerLock.release();
    }
}
