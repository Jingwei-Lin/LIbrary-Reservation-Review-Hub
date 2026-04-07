package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationUpdatePickupRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Reservation;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReservationVO;
import com.comp5619.libraryreservationreviewhub.service.ReservationService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReservationService reservationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReservationController reservationController;

    private User testUser;
    private Reservation testReservation;
    private ReservationVO testReservationVO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reservationController).build();

        testUser = new User();
        testUser.setId(1L);
        testUser.setStatus(1);

        testReservation = new Reservation();
        testReservation.setReservationId(1L);
        testReservation.setUserId(1L);
        testReservation.setBookId(1L);

        testReservationVO = new ReservationVO();
        testReservationVO.setReservationId(1L);
        testReservationVO.setUserId(1L);
        testReservationVO.setBookId(1L);
    }

    @Test
    void createReservation_Success() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(testUser);

        ReservationAddRequest req = new ReservationAddRequest();
        req.setPickupTime(new Date(System.currentTimeMillis() + 86400000L));
        req.setDueTime(new Date(System.currentTimeMillis() + 3 * 86400000L));

        // Serialize DTO to valid JSON using Jackson
        String jsonRequest = new ObjectMapper().writeValueAsString(req);

        when(reservationService.createReservation(eq(1L), any(ReservationAddRequest.class), eq(testUser)))
                .thenReturn(1L);

        mockMvc.perform(post("/reservation/create/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void cancelReservation_Success() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(testUser);

        when(reservationService.cancelReservation(1L, testUser)).thenReturn(true);

        mockMvc.perform(delete("/reservation/cancel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void updateStatus_Success() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(testUser);

        when(reservationService.updateReservationStatus(1L, "borrowed", testUser)).thenReturn(true);

        mockMvc.perform(put("/reservation/status/1")
                        .param("status", "borrowed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getReservation_Success() throws Exception {
        when(reservationService.getReservationById(1L)).thenReturn(testReservation);
        when(reservationService.toReservationVO(testReservation)).thenReturn(testReservationVO);

        mockMvc.perform(get("/reservation/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservationId").value(1));
    }

    @Test
    void getUserReservations_Success() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(testUser);

        when(reservationService.getReservationsByUser(1L)).thenReturn(List.of(testReservationVO));

        mockMvc.perform(get("/reservation/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reservationId").value(1));
    }

    @Test
    void getAllReservations_Success() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(testUser);

        PageResult<ReservationVO> pageResult = new PageResult<>(
                1L, 10L, 1L, List.of(testReservationVO)
        );
        pageResult.setCurrent(1);
        pageResult.setPageSize(10);
        pageResult.setTotal(1);
        pageResult.setRecords(List.of(testReservationVO));

        when(reservationService.getAllReservations(any(PageRequest.class), eq(testUser))).thenReturn(pageResult);

        mockMvc.perform(get("/reservation/all")
                        .param("current", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].reservationId").value(1));
    }

    @Test
    void hasActiveReservation_Success() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(testUser);

        when(reservationService.hasActiveReservation(testUser.getId(), 1L)).thenReturn(true);

        mockMvc.perform(get("/reservation/check/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void updatePickupTime_Success() throws Exception {
        ReservationUpdatePickupRequest req = new ReservationUpdatePickupRequest();
        req.setReservationId(1L);
        req.setPickupTime(new Date(System.currentTimeMillis() + 86400000L));

        // Serialize DTO to valid JSON using Jackson
        String jsonRequest = new ObjectMapper().writeValueAsString(req);

        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(testUser);

        when(reservationService.updatePickupTime(any(ReservationUpdatePickupRequest.class), eq(testUser)))
                .thenReturn(true);
        mockMvc.perform(put("/reservation/pickup/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }
}
