package com.comp5619.libraryreservationreviewhub.model.dto.waitingList;

import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import lombok.Data;

/** 条件查询/分页 */
@Data
public class WaitingListQueryDTO {

    private Long userId;
    private Long bookId;
    private WaitingStatusEnum status;
    private Boolean isNotified;

    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String orderBy; // eg "join_date desc"
}
