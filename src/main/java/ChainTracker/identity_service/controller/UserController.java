package ChainTracker.identity_service.controller;

import ChainTracker.identity_service.dto.LinkResponseDto;
import ChainTracker.identity_service.dto.TelegramLinkDto;
import ChainTracker.identity_service.dto.UserLoginDto;
import ChainTracker.identity_service.dto.UserRegisterDto;
import ChainTracker.identity_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Cadastro, login e vínculo de contas com o Telegram.")
@RestController
@RequestMapping("api/auth")
@AllArgsConstructor
public class UserController {
    private UserService service;

    @Operation(
            summary = "Cadastra um usuário",
            description = "Cria o usuário no Keycloak com username, e-mail e senha e devolve o ID gerado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário criado; corpo com o ID do Keycloak"),
            @ApiResponse(responseCode = "400", description = "Dados de cadastro inválidos"),
            @ApiResponse(responseCode = "409", description = "Username ou e-mail já cadastrado"),
            @ApiResponse(responseCode = "502", description = "Falha ao criar o usuário no Keycloak")
    })
    @PostMapping("/register")
    public ResponseEntity<String> createUser(@RequestBody UserRegisterDto dto){
        return ResponseEntity.ok(service.createUser(dto));
    }

    @Operation(
            summary = "Autentica um usuário",
            description = "Solicita ao Keycloak um token pelo grant de senha e devolve o AccessTokenResponse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token retornado"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@RequestBody UserLoginDto dto){
       return ResponseEntity.ok(service.login(dto));
    }

    @Operation(
            summary = "Gera um código de vínculo com o Telegram",
            description = "Gera um código de uso único, válido por 5 minutos, para o usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código gerado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/telegram")
    public ResponseEntity<LinkResponseDto> generateCode(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(service.generateLinkCode(jwt.getSubject()));
    }

    @Operation(
            summary = "Vincula um chat do Telegram",
            description = "Valida o código informado e cria o vínculo entre o usuário e o chat."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vínculo criado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "409", description = "Usuário ou chat já vinculado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/link")
    public boolean linkUser(@RequestBody TelegramLinkDto dto){
       return service.linkUser(dto.chatId(), dto.code());
    }
}
