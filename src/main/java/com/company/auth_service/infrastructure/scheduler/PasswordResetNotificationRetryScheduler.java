package com.company.auth_service.infrastructure.scheduler;

import com.company.auth_service.domain.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetNotificationRetryScheduler {

    private final PasswordResetNotificationRetryService retryService;

    @Value("${auth.password-reset.retry-batch-size:100}")
    private int batchSize;

    private final PasswordResetTokenRepository tokenRepository;
    private final MailEventPublisher mailEventPublisher;
    private final PasswordResetTokenIssuer tokenIssuer;

    @Scheduled(fixedDelayString = "${auth.password-reset.retry-interval-ms:60000}")
    public void retryUnnotified() {
        retryService.giveUpOnExpired();

        int processed;
        do{
            processed = retryService.retryBatch(batchSize);
        }while (processed>0&&processed==batchSize);
    }
}
