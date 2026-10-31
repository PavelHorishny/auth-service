package com.company.auth_service.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationRetryService {
    private final UserRepository userRepository;
    private final MailEventPublisher mailEventPublisher;

    @Transactional
    public int retryBatch(int batchSize){
        List<PendingUserNotification> batch = userRepository.lockPendingNotifications(batchSize);
        List<UUID> delivered = new ArrayList<>(batch.size());
        for(PendingUserNotification pending : batch){
            try{
                mailEventPublisher.publish(UserRegistrationMailEventFactory.from(pending));
                delivered.add(pending.id());
            }catch (Exception e){
                log.warn("Retry publish failed for new-user event (userId={}), stopping this batch", pending.id(),e);
                break;
            }
        }
        userRepository.markNotified(delivered);
        return delivered.size();
    }
}
