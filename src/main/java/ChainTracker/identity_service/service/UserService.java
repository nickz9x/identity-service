package ChainTracker.identity_service.service;

import ChainTracker.identity_service.dto.UserLoginDto;
import ChainTracker.identity_service.dto.UserRegisterDto;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;

    @Service
    @AllArgsConstructor
    public class UserService {
        private final Keycloak keycloak;
        private final String realm = "ChainTracker";



        public String createUser(UserRegisterDto dto){

            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            credentialRepresentation.setValue(dto.password());
            credentialRepresentation.setTemporary(false);

            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setUsername(dto.username());
            userRepresentation.setEmail(dto.email());
            userRepresentation.setEnabled(true);
            userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));
            Response response = keycloak.realm(realm).users().create(userRepresentation);

            if (response.getStatus() == 201) {
                // Sucesso! O Keycloak retorna a URL com o ID do usuário criado no header Location
                String path = response.getLocation().getPath();
                return path.substring(path.lastIndexOf("/") + 1); // Retorna o ID do Usuário
            } else {
                throw new RuntimeException("Erro ao criar usuário no Keycloak. Status: " + response.getStatus());
            }
        }

        public AccessTokenResponse login(UserLoginDto dto){
            Keycloak build = KeycloakBuilder.builder()
                    .username(dto.username())
                    .password(dto.senha())
                    .serverUrl("http://localhost:8081")
                    .realm(realm)
                    .grantType("password")
                    .clientId("identity-service")
                    .clientSecret("gfRTzEHqLtpke6zZPZKWfdji6WMmJtJS99zxt0NJ8Cl5HRBjDui1ElOUkGtQGjkHf9k2Ir9nsYzYqiDlHo57tp")
                    .build();
            return build.tokenManager().getAccessToken();

        }
    }
