package com.comp5619.libraryreservationreviewhub.model.dto.reservation;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ReservationUpdatePickupRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long reservationId;
    private Date pickupTime;
}