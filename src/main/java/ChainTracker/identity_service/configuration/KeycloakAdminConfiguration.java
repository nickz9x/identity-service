package ChainTracker.identity_service.configuration;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfiguration {

    @Value("${security.client-secret}")
    private String clientSecret;

    @Bean
    public Keycloak keycloakAdmin(){
        return KeycloakBuilder.builder()
                .serverUrl("http://localhost:8081")
                .realm("ChainTracker")
                .clientId("identity-service")
                .clientSecret(clientSecret)
                .grantType("client_credentials")
                .build();
    }
}
