package ChainTracker.identity_service.repository;

import ChainTracker.identity_service.entity.UserTelegramLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTelegramLinkRepository extends JpaRepository<UserTelegramLink,String> {
    Optional<UserTelegramLink> findByTelegramChatId(Long telegramChatId);

    Optional<UserTelegramLink> findByUserId(String userId);
}
