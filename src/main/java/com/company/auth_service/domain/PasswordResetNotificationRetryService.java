package com.company.auth_service.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetNotificationRetryService {
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final MailEventPublisher mailEventPublisher;

    @Transactional
    public int giveUpOnExpired(){
        int closed = tokenRepository.markExpiredNotified(Instant.now());
        if(closed>0){
            log.info("Gave up on {} password reset token(s) that expired before notification was sent", closed);
        }
        return closed;
    }

    @Transactional
    public int retryBatch(int batchSize){
        List<PendingPasswordResetNotification> batch = tokenRepository.lockPendingNotifications(batchSize);
        if(batch.isEmpty()){
            return 0;
        }
        Map<UUID, String> emails = userRepository.findEmailsByIds(
                batch.stream().map(PendingPasswordResetNotification::userId).distinct().toList()
        );
        List<UUID> done = new ArrayList<>(batch.size());
        for(PendingPasswordResetNotification pending : batch){
            String email = emails.get(pending.userId());
            if (email == null){
                log.warn("User {} of password reset token {} no longer exists, dropping its notification",pending.userId(),pending.id());
                done.add(pending.id());
                continue;
            }
            try{
                mailEventPublisher.publish(PasswordResetMailEventFactory.from(pending,email));
                done.add(pending.id());
            }catch (Exception e){
                log.warn("Retry publish failed for password reset token {}, stopping this batch", pending.id(),e);
                break;
            }
        }
        tokenRepository.markNotified(done);
        return done.size();
    }
}
