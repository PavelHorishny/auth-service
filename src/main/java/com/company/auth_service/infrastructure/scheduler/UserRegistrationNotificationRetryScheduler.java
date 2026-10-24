package com.company.auth_service.infrastructure.scheduler;

import com.company.auth_service.domain.MailEventPublisher;
import com.company.auth_service.domain.User;
import com.company.auth_service.domain.UserRepository;
import com.company.auth_service.domain.UserRegistrationIssuer;
import com.company.auth_service.domain.UserRegistrationMailEventFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationNotificationRetryScheduler {

    private final UserRepository userRepository;
    private final MailEventPublisher mailEventPublisher;
    private final UserRegistrationIssuer userRegistrationIssuer;

    @Scheduled(fixedDelayString = "${auth.registration.retry-interval-ms:60000}")
    public void retryUnnotified() {
        for (User user : userRepository.findAllByNotifiedFalse()) {
            try {
                mailEventPublisher.publish(UserRegistrationMailEventFactory.from(user));
                userRegistrationIssuer.markNotified(user);
                //TODO Czy warto rozdzielic logike tak jak w auth-service?
            } catch (Exception e) {
                log.warn("Retry publish failed for new-user event (userId={}), will retry again later",
                        user.getId(), e);
            }
        }
    }
}
