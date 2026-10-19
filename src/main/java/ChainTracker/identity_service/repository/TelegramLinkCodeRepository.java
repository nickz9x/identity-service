package ChainTracker.identity_service.repository;

import ChainTracker.identity_service.entity.TelegramLinkCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramLinkCodeRepository extends JpaRepository<TelegramLinkCode,Long> {
    Optional<TelegramLinkCode> findByCode(String code);
    void deleteByUserId(String userId);
}
