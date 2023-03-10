package org.acme;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;

import javax.ws.rs.core.Application;

/**
 * Class that is just used to configure OpenAPI UI when you use the Swagger-UI
 */
@OpenAPIDefinition(
        info = @Info(
                title="Snapshot Archive API",
                version = "1.0.0",
                contact = @Contact(
                        name = "Management & Monitoring",
                        url = "http://rajant.com",
                        email = "techsupport@example.com"),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html"))
)
public class SnapshotArchiveApplication extends Application {
}
