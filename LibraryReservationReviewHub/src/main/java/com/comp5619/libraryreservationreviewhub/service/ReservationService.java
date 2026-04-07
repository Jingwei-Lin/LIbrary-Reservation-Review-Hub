package com.comp5619.libraryreservationreviewhub.service;

import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationUpdatePickupRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Reservation;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReservationVO;

import java.util.List;

public interface ReservationService {
    Long createReservation(Long bookId, ReservationAddRequest req,User user);
    boolean cancelReservation(Long reservationId, User user);
    boolean updateReservationStatus(Long reservationId, String status, User user);
    Reservation getReservationById(Long id);
    List<ReservationVO> getReservationsByUser(Long userId);
    PageResult<ReservationVO> getAllReservations(PageRequest pageRequest, User operator);

    ReservationVO toReservationVO(Reservation reservation);

    boolean hasActiveReservation(Long userId, Long bookId);

    boolean updatePickupTime(ReservationUpdatePickupRequest req, User user);
}
