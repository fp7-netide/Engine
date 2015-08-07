package eu.netide.core.management;

import org.apache.karaf.config.core.ConfigMBean;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by timvi on 06.08.2015.
 */
public class ManagementHandler {

    public void Start() throws IOException, MBeanException, MalformedObjectNameException {
        System.out.println("ManagementHandler started.");
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1099/karaf-root");
        Map<String, String[]> environment = new HashMap<>();
        String[] creds = {"karaf", "karaf"};
        environment.put(JMXConnector.CREDENTIALS, creds);
        JMXConnector connector = JMXConnectorFactory.connect(url, environment);
        MBeanServerConnection mbeanServer = connector.getMBeanServerConnection();
        ObjectName systemMBean = new ObjectName("org.apache.karaf:type=config,name=root");
        ConfigMBean bean = (ConfigMBean) JMX.newMBeanProxy(mbeanServer, systemMBean, ConfigMBean.class);
        bean.setProperty("eu.netide.core.shimconnectivity", "port", "12345");
        System.out.println("ManagementHandler set port to 12345!");
        connector.close();
    }

    public void Stop() {
        System.out.println("ManagementHandler stopped.");
    }
}
