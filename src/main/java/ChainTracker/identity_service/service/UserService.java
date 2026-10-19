package ChainTracker.identity_service.service;

import ChainTracker.identity_service.dto.KafkaLinkedUser;
import ChainTracker.identity_service.dto.LinkResponseDto;
import ChainTracker.identity_service.dto.UserLoginDto;
import ChainTracker.identity_service.dto.UserRegisterDto;
import ChainTracker.identity_service.entity.TelegramLinkCode;
import ChainTracker.identity_service.entity.UserTelegramLink;
import ChainTracker.identity_service.repository.TelegramLinkCodeRepository;
import ChainTracker.identity_service.repository.UserTelegramLinkRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.IllegalFormatCodePointException;

@Service
    @RequiredArgsConstructor
    public class UserService {

        private final Keycloak keycloak;

        private final String realm = "ChainTracker";
        private final TelegramLinkCodeRepository telegramLinkCodeRepository;
        private final UserTelegramLinkRepository userTelegramLinkRepository;
        private final SecureRandom secureRandom;
        private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
        private final KafkaTemplate<String,Object> kafkaTemplate;

    @Value("${security.client-secret}")
        private String clientSecret;



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

        public AccessTokenResponse login(UserLoginDto dto) {
            try {
                Keycloak build = KeycloakBuilder.builder()
                        .serverUrl("http://localhost:8081")
                        .realm(realm) // Certifique-se de que o nome está idêntico ao do Keycloak
                        .clientId("identity-service")
                        .clientSecret(clientSecret) // Obrigatório já que Client Authentication está ON
                        .grantType(OAuth2Constants.PASSWORD) // Força o fluxo de senha correto
                        .username(dto.username())
                        .password(dto.password())
                        .authorization(null) // Evita buscas administrativas automáticas que geram 400/403
                        .build();

                return build.tokenManager().getAccessToken();
            } catch (jakarta.ws.rs.BadRequestException e) {
                // Captura o erro e tenta ler o que o Keycloak respondeu no corpo do HTTP
                String errorResponseBody = e.getResponse().readEntity(String.class);
                System.err.println("Erro detalhado do Keycloak: " + errorResponseBody);
                throw e;
            }
        }

        public LinkResponseDto generateLinkCode(String userId){
            TelegramLinkCode telegramLinkCode = new TelegramLinkCode();
            telegramLinkCode.setUserId(userId);
            byte[] randomBytes = new byte[4];
            secureRandom.nextBytes(randomBytes);
            String token = base64Encoder.encodeToString(randomBytes);
            telegramLinkCode.setCode(token);
            telegramLinkCode.setCreatedAt(LocalDateTime.now());
            telegramLinkCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            telegramLinkCodeRepository.save(telegramLinkCode);
            return new LinkResponseDto(token);
        }


        public boolean linkUser(Long chatId,String token){
            TelegramLinkCode telegramLinkCode = telegramLinkCodeRepository.findByCode(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
            if (userTelegramLinkRepository.findByUserId(telegramLinkCode.getUserId()).isPresent()){
                throw new ResponseStatusException(HttpStatus.CONFLICT,"this user alread have a linked telegram");
            }
            if (userTelegramLinkRepository.findByTelegramChatId(chatId).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "this account is alread linked");
            }
            if (LocalDateTime.now().isBefore(telegramLinkCode.getExpiresAt())){
                UserTelegramLink userTelegramLink = new UserTelegramLink();
                userTelegramLink.setUserId(telegramLinkCode.getUserId());
                userTelegramLink.setTelegramChatId(chatId);
                userTelegramLink.setLinkedAt(LocalDateTime.now());
                userTelegramLinkRepository.save(userTelegramLink);
                telegramLinkCodeRepository.delete(telegramLinkCode);
                kafkaTemplate.send("linked.users",new KafkaLinkedUser(userTelegramLink.getUserId(),chatId)).whenComplete((stringObjectSendResult, throwable) -> System.out.println("enviado"));
                return true;
            }
            else {
                return false;
            }
        }
    }
