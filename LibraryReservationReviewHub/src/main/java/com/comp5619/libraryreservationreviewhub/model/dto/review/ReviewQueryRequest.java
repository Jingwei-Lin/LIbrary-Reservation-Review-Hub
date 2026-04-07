package com.comp5619.libraryreservationreviewhub.model.dto.review;

import com.comp5619.libraryreservationreviewhub.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class ReviewQueryRequest extends PageRequest {
    private Long bookId;
    private Long userId;
    private Integer minRating;// 根据rating范围输出
    private Integer maxRating;
}
