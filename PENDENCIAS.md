# Pendências — identity-service

Atualizado: 2026-09-25

---

## Contexto rápido

Serviço de identidade do **ChainTracker**. Não guarda usuário nem senha: ele é uma
casca fina sobre o Keycloak. Recebe o registro/login, repassa para o Keycloak via
`keycloak-admin-client`, e devolve o token.

**Stack:** Spring Boot 4.1.1, Java 21, PostgreSQL, Keycloak, Maven.

**Porta:** 8080. O Keycloak está em 8081.

**O que ele expõe:**

| Método | Rota | O que devolve |
|---|---|---|
| POST | `/api/auth/register` | O **id** do usuário criado, como texto puro |
| POST | `/api/auth/login` | O `AccessTokenResponse` do Keycloak |

**Papel no sistema:** é o **emissor do `sub`**. O UUID que o `register` devolve é
o mesmo claim `sub` do JWT, e é ele que o `portfolioService` grava em
`owner_id`. Não existe FK entre os serviços — a ligação é esse valor.

---

## 🔴 Bloqueadores

### 1. Client secret do Keycloak commitado no git

**Onde:** `src/main/resources/application.yaml`, propriedade
`security.client-secret`, no fim do arquivo.

**Situação:** o `.gitignore` do projeto cobre `target/`, `.idea`, etc., mas
**não cobre `application.yaml`** — o arquivo é rastreado. O valor está no
histórico nos commits `87b6279`, `d124220` e `6bb01fb`, e o valor **novo** (a
rotação mais recente) entrou no commit seguinte.

Os repositórios são privados, o que limita o alcance, mas quem já clonou tem os
dois valores — e a rotação não resolveu nada, só trocou qual segredo está exposto.

**Fix:**

1. Trocar por variável de ambiente:
   `security.client-secret: ${KEYCLOAK_CLIENT_SECRET}`
2. Registrar a variável na config da IDE (Run → Edit Configurations → Environment
   variables) e cobrir `.env` no `.gitignore`.
3. **Rotacionar de novo no Keycloak**, e desta vez não commitar o valor.

Tirar do arquivo não apaga o histórico. Reescrever com `git filter-repo` só
compensa se o repositório virar público — e mesmo assim exige que todo mundo
reclone.

---

## 🟡 Importante

### 2. Nenhuma validação de entrada

`UserRegisterDto` e `UserLoginDto` são records sem nenhuma anotação, e o
`UserController` não usa `@Valid`:

```java
public record UserRegisterDto(String username,String email,String password) {}
public record UserLoginDto(String username,String password) {}
```

Um `POST /api/auth/register` com corpo `{}` passa direto: `username`, `email` e
`password` chegam `null` no `UserService.createUser`, que monta um
`UserRepresentation` nulo e manda pro Keycloak. O Keycloak recusa (400), mas o
erro que chega no cliente é genérico — o motivo real fica no log do Keycloak.

**Fix:** `@NotBlank` nos campos e `@Valid` no controller, como já é feito no
`portfolioService`. Vale também validar formato de e-mail.

### 3. Erros do Keycloak viram 500 sem explicação

`UserService.createUser:52` lança `RuntimeException("Erro ao criar usuário...")`
quando o status não é 201. Isso vira HTTP 500 sem corpo útil.

`UserService.login:70-75` captura `BadRequestException`, imprime o corpo do erro
do Keycloak no `System.err` e **relança a mesma exceção** — ou seja, o cliente
recebe 500 e a informação boa ficou só no log do servidor.

O certo é o cliente receber 401 em credencial inválida e 409 em usuário
duplicado — não 500 nos dois casos.

**Fix:** um `@RestControllerAdvice` mapeando as exceções do Keycloak para status
HTTP adequados, e `log.error` no lugar de `System.err`.

### 4. `Response` do Keycloak não é fechado

`UserService.createUser:45` recebe um `jakarta.ws.rs.core.Response` e nunca chama
`response.close()`. Em caminho de erro, o `getStatus()` também não fecha. É
recurso HTTP vazando — o cliente do Keycloak não fecha por você.

**Fix:** `try (Response response = ...) { ... }`.

### 5. Config do Keycloak repetida e hardcoded

Os mesmos três valores aparecem em dois arquivos, escritos à mão:

| Valor | `UserService.login` | `KeycloakAdminConfiguration` |
|---|---|---|
| `serverUrl` | `:59` | `:18` |
| `realm` | `:60` | `:19` |
| `clientId` | `:61` | `:20` |

Trocar o realm significa editar dois arquivos e rezar para não esquecer um. O
projeto **já usa `@Value`** (para o `client-secret` nos dois), então é só estender:
puxar `keycloak.server-url`, `keycloak.realm` e `keycloak.client-id` para o
`application.yaml`.

### 6. `POST /api/auth/refresh` não existe

O serviço só tem `/register` e `/login`. Sem refresh, o front precisa pedir senha
de novo a cada 5 minutos (o access token dura 300s por padrão no Keycloak) — ou
alguém vai acabar aumentando o lifespan do access token, que é o caminho errado.

**Fix:** um endpoint que chame o Keycloak com `grant_type=refresh_token`. Vale
também um `/logout` (o Keycloak tem `logout` no protocolo, e sem ele o refresh
token continua válido até expirar).

---

## 🔵 Limpeza

### 7. Imports órfãos em `UserService`

- `:6` — `lombok.AllArgsConstructor`, mas a classe usa `@RequiredArgsConstructor`
- `:14` — `org.springframework.beans.factory.annotation.Autowired`, não usado

### 8. `createUser` devolve o id como texto puro

`UserController.createUser` devolve `ResponseEntity<String>` — o UUID cru no
corpo. O `portfolioService` usa `PortfolioResponseDto` para isso. Um
`UserResponseDto` deixaria o contrato explícito e permitiria devolver `username`
e `email` junto.

O caminho para extrair o id (`path.substring(path.lastIndexOf("/") + 1)`) depende
do formato da URL que o Keycloak devolve no header `Location`. Funciona, mas é
frágil a mudança de versão do Keycloak — e não há teste cobrindo.

### 9. `realm` é campo de instância

`UserService:26` — `private final String realm = "ChainTracker";`. Como já tem
inicializador, o Lombok **não** o inclui no construtor do
`@RequiredArgsConstructor` (correto), mas a constante mora dentro do service.
Junto com o item 5, o lugar dela é a configuração.

### 10. `SecurityConfiguration` sem `AuthenticationEntryPoint`

A config libera `/api/auth/**` e `/error` e exige autenticação no resto. Sem um
entry point customizado, um request sem token recebe 401 com corpo vazio — o
cliente não sabe se falta token, se ele expirou ou se o realm está errado.

---

## 🧪 DX e testes

### 11. `@PreAuthorize` e `@PermitAll` são decorativos

`UserController:6` importa `jakarta.annotation.security.PermitAll` e `:10` importa
`org.springframework.security.access.prepost.PreAuthorize`, mas **nenhuma das duas
anotações é usada** no arquivo. Quem libera as rotas é o `SecurityConfiguration`.
Ou aplica as anotações (e liga `@EnableMethodSecurity`), ou remove os imports.

### 12. Não existe teste de verdade

Só o `IdentityServiceApplicationTests` gerado pelo Initializr. Nada cobre a
extração do id no `createUser` nem o mapeamento de erro do `login` — que são
exatamente os pontos frágeis dos itens 3 e 8.

---

## ✅ Já resolvido (não refazer)

- `SecurityConfiguration` — faltava `@Bean` no `filterChain`, o que fazia o Spring
  ignorar a config inteira e cair na segurança padrão.
- `/error` liberado no `SecurityConfiguration:23`. Sem isso, **qualquer** erro da
  aplicação virava 401 e a causa real ficava escondida atrás dele.
- `jwk-set-uri` explícito no `application.yaml`, junto com o `issuer-uri`.
- Client secret externo via `@Value("${security.client-secret}")` nos dois pontos
  que precisam dele — o valor está no arquivo, mas o código não o tem hardcoded.
