package ChainTracker.identity_service.configuration;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.security.SecureRandom;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http){
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> {
            // Pagina de erro interna do Spring. Sem liberar, QUALQUER erro da
            // aplicacao vira 401 e a causa real fica escondida atras dele.
            auth.requestMatchers("/error").permitAll();
            // Cadastro e login acontecem antes de existir token.
            auth.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll();
            // Copia das rotas usadas pelo bot do Telegram, que nao carrega JWT.
            // As originais (/api/auth/**) continuam exigindo autenticacao.
            auth.requestMatchers("/api/bot/**").permitAll();
            auth.anyRequest().authenticated();
        });
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public SecureRandom secureRandom(){
        return new SecureRandom();
    }
}
