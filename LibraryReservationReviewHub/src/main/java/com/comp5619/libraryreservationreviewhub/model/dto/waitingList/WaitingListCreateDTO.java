package com.comp5619.libraryreservationreviewhub.model.dto.waitingList;

import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 新增等待记录 */
@Data
public class WaitingListCreateDTO {

    @NotNull(message = "userId can not be null")
    private Long userId;

    @NotNull(message = "bookId can not be null")
    private Long bookId;

    private WaitingStatusEnum status;   // default WAITING
    private Boolean isNotified;         // default false
}
