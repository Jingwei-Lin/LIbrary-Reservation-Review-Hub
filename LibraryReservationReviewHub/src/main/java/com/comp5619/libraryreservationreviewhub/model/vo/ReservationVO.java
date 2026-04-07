package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class ReservationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long reservationId;
    private Long userId;
    private Long bookId;
    private String bookTitle; // From Book entity
    private Date reservationTime;
    private Date pickupTime;
    private Date dueTime;
    private Date returnTime;
    private String reservationStatus;


}