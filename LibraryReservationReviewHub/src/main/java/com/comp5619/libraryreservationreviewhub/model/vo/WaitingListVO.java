package com.comp5619.libraryreservationreviewhub.model.vo;

import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/** return view object */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingListVO {

    private Long waitId;
    private Long userId;
    private Long bookId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime notificationDate;

    private Boolean isNotified;

    /** waiting / notified / cancelled / fulfilled */
    private WaitingStatusEnum status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String userName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String bookTitle;
}
