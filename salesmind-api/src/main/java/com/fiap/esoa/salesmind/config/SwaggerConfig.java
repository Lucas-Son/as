
package com.fiap.esoa.salesmind.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

/**
 * Configuration class for Swagger/OpenAPI documentation
 */
public class SwaggerConfig {

    private static final String API_VERSION = "1.0.0";
    private static final String API_TITLE = "SalesMind API";
    private static final String API_DESCRIPTION = "REST API for SalesMind application";

    /**
     * Creates and configures the OpenAPI specification
     * 
     * @param serverUrl The base URL of the API server
     * @return Configured OpenAPI object
     */
    public static OpenAPI createOpenAPI(String serverUrl) {
        Server server = new Server();
        server.setUrl(serverUrl);
        server.setDescription("SalesMind API Server");

        Contact contact = new Contact();
        contact.setEmail("salesmind@fiap.com");
        contact.setName("SalesMind Team");

        License license = new License();
        license.setName("MIT License");
        license.setUrl("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title(API_TITLE)
                .version(API_VERSION)
                .description(API_DESCRIPTION)
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }

    /**
     * Get the API title
     */
    public static String getApiTitle() {
        return API_TITLE;
    }

    /**
     * Get the API version
     */
    public static String getApiVersion() {
        return API_VERSION;
    }
}
