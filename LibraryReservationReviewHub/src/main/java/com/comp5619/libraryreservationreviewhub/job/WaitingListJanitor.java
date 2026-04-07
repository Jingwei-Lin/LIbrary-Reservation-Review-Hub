package com.comp5619.libraryreservationreviewhub.job;

import com.comp5619.libraryreservationreviewhub.service.WaitingListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WaitingListJanitor {

    private final WaitingListService waitingListService;

    // 每天 03:15 触发（
    @Scheduled(cron = "0 15 3 * * ?")
    public void cancelOverdue() {
        int rows = waitingListService.cancelStaleWaiting();
        log.info("WaitingListJanitor ran: {} rows cancelled.", rows);
    }
}
