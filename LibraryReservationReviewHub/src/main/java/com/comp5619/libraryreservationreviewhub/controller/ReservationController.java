package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.annotation.AuthCheck;
import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationUpdatePickupRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Reservation;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReservationVO;
import com.comp5619.libraryreservationreviewhub.service.ReservationService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final UserService userService;

    @PostMapping("/create/{bookId}")
    public BaseResponse<Long> createReservation(@PathVariable Long bookId,
                                                @RequestBody ReservationAddRequest req,
                                                HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(reservationService.createReservation(bookId, req, user));
    }

    @DeleteMapping("/cancel/{reservationId}")
    public BaseResponse<Boolean> cancelReservation(@PathVariable Long reservationId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(reservationService.cancelReservation(reservationId, user));
    }

    @PutMapping("/status/{reservationId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateStatus(@PathVariable Long reservationId,
                                              @RequestParam String status,
                                              HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(reservationService.updateReservationStatus(reservationId, status, user));
    }

    @GetMapping("/{reservationId}")
    public BaseResponse<ReservationVO> getReservation(@PathVariable Long reservationId) {
        Reservation reservation = reservationService.getReservationById(reservationId);
        return ResultUtils.success(reservationService.toReservationVO(reservation));
    }

    @GetMapping("/user")
    public BaseResponse<List<ReservationVO>> getUserReservations(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(reservationService.getReservationsByUser(user.getId()));
    }

    @GetMapping("/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PageResult<ReservationVO>> getAllReservations(PageRequest pageRequest,
                                                                      HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(reservationService.getAllReservations(pageRequest, user));
    }

    @GetMapping("/check/{bookId}")
    public BaseResponse<Boolean> hasActiveReservation(@PathVariable Long bookId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(reservationService.hasActiveReservation(user.getId(), bookId));
    }

    @PutMapping("/pickup/{reservationId}")
    public BaseResponse<Boolean> updatePickupTime(@PathVariable Long reservationId,
                                                  @RequestBody ReservationUpdatePickupRequest req,
                                                  HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        req.setReservationId(reservationId); // Set reservationId from path variable
        return ResultUtils.success(reservationService.updatePickupTime(req, user));
    }
}
