package ChainTracker.identity_service.service;

import ChainTracker.identity_service.dto.KafkaLinkedUser;
import ChainTracker.identity_service.dto.LinkResponseDto;
import ChainTracker.identity_service.dto.TelegramRegisterDto;
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

    @Value("${keycloak.url:http://keycloak:8080}")
        private String keycloakUrl;



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
                String path = response.getLocation().getPath();
                return path.substring(path.lastIndexOf("/") + 1); // Retorna o ID do Usuário
            }
            if (response.getStatus() == HttpStatus.CONFLICT.value()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "nome de usuário ou e-mail já cadastrado");
            }
            if (response.getStatus() == HttpStatus.BAD_REQUEST.value()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dados de cadastro inválidos");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "erro ao criar usuário no Keycloak. Status: " + response.getStatus());
        }

        public boolean registerFromTelegram(TelegramRegisterDto dto){
            if (dto == null || dto.chatId() == null
                    || dto.username() == null || dto.username().isBlank()
                    || dto.email() == null || dto.email().isBlank()
                    || dto.password() == null || dto.password().length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dados de cadastro inválidos");
            }
            if (userTelegramLinkRepository.findByTelegramChatId(dto.chatId()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "este chat já está vinculado a uma conta");
            }

            String userId = createUser(new UserRegisterDto(
                    dto.username().trim(), dto.email().trim(), dto.password()));

            try {
                UserTelegramLink userTelegramLink = new UserTelegramLink();
                userTelegramLink.setUserId(userId);
                userTelegramLink.setTelegramChatId(dto.chatId());
                userTelegramLink.setLinkedAt(LocalDateTime.now());
                userTelegramLinkRepository.save(userTelegramLink);
                kafkaTemplate.send("linked.users", new KafkaLinkedUser(userId, dto.chatId()));
                return true;
            } catch (RuntimeException e) {
                keycloak.realm(realm).users().get(userId).remove();
                throw e;
            }
        }

        public AccessTokenResponse login(UserLoginDto dto) {
            try {
                Keycloak build = KeycloakBuilder.builder()
                        .serverUrl(keycloakUrl)
                        .realm(realm)
                        .clientId("identity-service")
                        .clientSecret(clientSecret)
                        .grantType(OAuth2Constants.PASSWORD)
                        .username(dto.username())
                        .password(dto.password())
                        .authorization(null)
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
