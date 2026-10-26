package ChainTracker.identity_service.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI identityServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChainTracker - identity-service")
                        .description("""
                                Cadastro, autenticação e vínculo de contas do ChainTracker.

                                /api/auth/register e /api/auth/login são públicos. As demais rotas
                                /api/auth exigem JWT. As rotas /api/bot/auth exigem um token de serviço
                                com o escopo bot.

                                Use o botão Authorize com um Bearer token do Keycloak para testar as rotas
                                protegidas.
                                """)
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT emitido pelo Keycloak. Para /api/bot/**, o token precisa ter o escopo bot.")));
    }
}
