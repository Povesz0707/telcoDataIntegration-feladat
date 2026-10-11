package hu.telcodata.integration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI telcoDataIntegrationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Telco Data Integration API")
                        .version("v1")
                        .description("CSV és JSON termékadatforrások feltöltése, normalizálása és lekérdezése.")
                        .contact(new Contact()
                                .name("Telco Data Integration")));
    }
}
