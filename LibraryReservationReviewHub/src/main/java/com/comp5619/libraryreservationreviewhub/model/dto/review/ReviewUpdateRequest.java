package com.comp5619.libraryreservationreviewhub.model.dto.review;

import java.io.Serializable;

import lombok.Data;

@Data
public class ReviewUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private long reviewId;
    private Integer rating;
    private String comment;
}
