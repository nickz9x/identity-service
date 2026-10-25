package ChainTracker.identity_service.dto;

public record TelegramRegisterDto(String username, String email, String password, Long chatId) {
}
