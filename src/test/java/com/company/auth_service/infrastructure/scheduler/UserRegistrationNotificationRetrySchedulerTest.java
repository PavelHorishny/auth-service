package com.company.auth_service.infrastructure.scheduler;

import com.company.auth_service.domain.UserNotificationRetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationNotificationRetrySchedulerTest {

    private static final int BATCH_SIZE = 50;

    @Mock
    private UserNotificationRetryService retryService;

    private UserRegistrationNotificationRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new UserRegistrationNotificationRetryScheduler(retryService);
        ReflectionTestUtils.setField(scheduler, "batchSize", BATCH_SIZE);
    }

    @Test
    void retryUnnotified_drainsBacklogUntilShortBatch() {
        when(retryService.retryBatch(BATCH_SIZE)).thenReturn(BATCH_SIZE, BATCH_SIZE, 7);

        scheduler.retryUnnotified();

        verify(retryService, times(3)).retryBatch(BATCH_SIZE);
    }

    @Test
    void retryUnnotified_doesNotLoopWhenNothingPending() {
        when(retryService.retryBatch(BATCH_SIZE)).thenReturn(0);

        scheduler.retryUnnotified();

        verify(retryService, times(1)).retryBatch(BATCH_SIZE);
    }
}