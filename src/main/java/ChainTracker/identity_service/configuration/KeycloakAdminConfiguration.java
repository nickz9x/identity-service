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
                .clientSecret("gfRTzEHqLtpke6zZPZKWfdji6WMmJtJS99zxt0NJ8Cl5HRBjDui1ElOUkGtQGjkHf9k2Ir9nsYzYqiDlHo57tp")
                .grantType("client_credentials")
                .build();
    }
}
