package com.comp5619.libraryreservationreviewhub.model.dto.review;

import java.io.Serializable;

import lombok.Data;

@Data
public class ReviewAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long bookId;
    private Integer rating;
    private String comment;
}
