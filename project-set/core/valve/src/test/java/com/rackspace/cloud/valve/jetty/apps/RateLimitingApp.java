package com.rackspace.cloud.valve.jetty.apps;

import com.rackspace.cloud.valve.jetty.servlet.BasicResponseServlet;
import com.rackspace.papi.components.datastore.DistributedDatastoreFilter;
import com.rackspace.papi.components.ratelimit.RateLimitingFilter;
import com.rackspace.papi.servlet.InitParameter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zinic
 */
public class RateLimitingApp {

    private final Server jettyServerReference;
    private final ServletContextHandler rootContext;

    public RateLimitingApp(int port) throws Exception {
        jettyServerReference = new Server(port);

        rootContext = buildRootContext(jettyServerReference);

        final EnumSet<DispatcherType> dispatchers = EnumSet.allOf(DispatcherType.class);

        rootContext.addServlet(BasicResponseServlet.class, "/*");
        rootContext.addFilter(DistributedDatastoreFilter.class, "/*", dispatchers);
        rootContext.addFilter(RateLimitingFilter.class, "/*", dispatchers);
    }

    private ServletContextHandler buildRootContext(Server serverReference) {
        final Map<String, String> initParams = new HashMap<String, String>();
        initParams.put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), "/home/zinic/installed/etc/powerapi");
        

        final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/");

        servletContext.getInitParams().putAll(initParams);

        servletContext.addEventListener(new ContextLoaderListener());

        return servletContext;
    }

    public void go() throws Exception {
        jettyServerReference.start();
    }

    public static void main(String[] args) throws Exception {
        new RateLimitingApp(8080).go();
    }
}