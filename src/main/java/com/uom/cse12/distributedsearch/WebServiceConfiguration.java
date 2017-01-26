package com.uom.cse12.distributedsearch;


import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

public class WebServiceConfiguration extends ResourceConfig {
    public WebServiceConfiguration() {
        // Now you can expect validation errors to be sent to the client.
        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        // @ValidateOnExecution annotations on subclasses won't cause errors.
        property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
        // Define the package which contains the service classes.
        packages(Command.WEB_SERVICE_PACKAGE);
    }
}
