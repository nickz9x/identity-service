package ChainTracker.identity_service.configuration;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfiguration {
    @Bean
    public Keycloak keycloakAdmin(){
        return KeycloakBuilder.builder()
                .serverUrl("http://localhost:8081")
                .realm("ChainTracker")
                .clientId("identity-service")
                .clientSecret("V13fHqYbSPL5On8UzOdBinltpNqtIIQw8GW7slulDyvGkv75PVv8dWP7C8rMD5zKvv2CQS1hN4lroSAhwKYson")
                .grantType("client_credentials")
                .build();
    }
}
