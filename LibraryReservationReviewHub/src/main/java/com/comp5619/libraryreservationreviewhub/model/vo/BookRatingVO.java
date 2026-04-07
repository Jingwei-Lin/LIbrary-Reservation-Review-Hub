package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class BookRatingVO {
    private Long bookId;
    private BigDecimal avgRating;
}
