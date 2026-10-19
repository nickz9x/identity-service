package ChainTracker.identity_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_telegram_links")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserTelegramLink {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId; // UUID do Keycloak (claim 'sub')

    @Column(name = "telegram_chat_id", unique = true, nullable = false)
    private Long telegramChatId;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt = LocalDateTime.now();
}