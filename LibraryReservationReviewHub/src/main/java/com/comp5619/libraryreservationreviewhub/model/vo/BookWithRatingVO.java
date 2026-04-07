package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookWithRatingVO {
    private Long bookId;
    private String title;
    private String author;
    private String genre;
    private String description;
    private String path;
    private Integer quantity;
    private Integer heldQuantity;
    private Boolean available;

    private BigDecimal avgRating; // 允许为 null
}
