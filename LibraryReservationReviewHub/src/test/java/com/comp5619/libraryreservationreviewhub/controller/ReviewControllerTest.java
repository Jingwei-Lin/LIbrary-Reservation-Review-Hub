package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;
import com.comp5619.libraryreservationreviewhub.service.ReviewService;
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
class ReviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReviewService reviewService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReviewController reviewController;

    private User testUser;
    private User testAdmin;
    private User otherUser;
    private Review testReview;
    private ReviewVO testReviewVO;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController).build();

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Jason");
        testUser.setLastName("Wang");

        testAdmin = new User();
        testAdmin.setId(2L);

        otherUser = new User();
        otherUser.setId(3L);

        testReview = new Review();
        testReview.setReviewId(1L);
        testReview.setUserId(1L);
        testReview.setBookId(1L);
        testReview.setRating(5);
        testReview.setComment("Great book!");
        testReview.setReviewDate(new Date());

        testReviewVO = new ReviewVO();
        testReviewVO.setReviewId(1L);
        testReviewVO.setUserId(1L);
        testReviewVO.setBookId(1L);
        testReviewVO.setRating(5);
        testReviewVO.setComment("Great book!");
        testReviewVO.setReviewDate(new java.sql.Date(System.currentTimeMillis()));
        testReviewVO.setUserName("Jason Wang");
    }

    @Test
    void getReviewsByBookId_Success() throws Exception {
        when(reviewService.getReviewsByBookId(1L)).thenReturn(List.of(testReviewVO));

        mockMvc.perform(get("/review/book/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].reviewId").value(1))
                .andExpect(jsonPath("$.data[0].userName").value("Jason Wang"));
    }

    @Test
    void addReview_Success() throws Exception {
        ReviewAddRequest req = new ReviewAddRequest();
        req.setRating(5);
        req.setComment("Great book!");

        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(testUser);
        when(reviewService.addReview(eq(1L), any(ReviewAddRequest.class), eq(testUser))).thenReturn(1L);

        String json = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/review/add/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void deleteReview_Success_AsOwner() throws Exception {
        when(userService.getLoginUser(any())).thenReturn(testUser);
        when(reviewService.deleteReview(1L, testUser)).thenReturn(true);

        mockMvc.perform(delete("/review/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void deleteReview_Success_AsAdmin() throws Exception {
        when(userService.getLoginUser(any())).thenReturn(testAdmin);
        when(reviewService.deleteReview(1L, testAdmin)).thenReturn(true);

        mockMvc.perform(delete("/review/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void updateReview_Success() throws Exception {
        ReviewUpdateRequest req = new ReviewUpdateRequest();
        req.setReviewId(1L);
        req.setRating(4);
        req.setComment("Updated review");

        when(userService.getLoginUser(any())).thenReturn(testUser);
        when(reviewService.updateReview(any(ReviewUpdateRequest.class), eq(testUser))).thenReturn(true);

        String json = objectMapper.writeValueAsString(req);

        mockMvc.perform(put("/review/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }
    @Test
    void getUserNameByReviewId_Success() throws Exception {
        when(reviewService.getUserNameByReviewId(1L)).thenReturn("Jason Wang");

        mockMvc.perform(get("/review/username/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("Jason Wang"));
    }

    @Test
    void getReviewById_Success() throws Exception {
        when(reviewService.getReviewById(1L)).thenReturn(testReview);
        when(reviewService.getReviewVO(testReview)).thenReturn(testReviewVO);

        mockMvc.perform(get("/review/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.reviewId").value(1))
                .andExpect(jsonPath("$.data.userName").value("Jason Wang"));
    }


//    @Test
//    void addReview_InvalidBookId_ReturnsError() throws Exception {
//        mockMvc.perform(post("/review/add/0")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(ErrorCode.PARAMS_ERROR.getCode()));
//    }
//
//    @Test
//    void deleteReview_NotAuthorized_ReturnsError() throws Exception {
//        when(userService.getLoginUser(any())).thenReturn(otherUser);
//        when(reviewService.deleteReview(1L, otherUser)).thenThrow(
//                new com.comp5619.libraryreservationreviewhub.exception.BusinessException(ErrorCode.NO_AUTH_ERROR)
//        );
//
//        mockMvc.perform(delete("/review/delete/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(ErrorCode.NO_AUTH_ERROR.getCode()));
//    }
//
//    @Test
//    void updateReview_InvalidReviewId_ReturnsError() throws Exception {
//        ReviewUpdateRequest req = new ReviewUpdateRequest();
//        req.setReviewId(0L);
//
//        String json = objectMapper.writeValueAsString(req);
//
//        mockMvc.perform(put("/review/update")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(json))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(ErrorCode.PARAMS_ERROR.getCode()))
//                .andExpect(jsonPath("$.message").value("Invalid review ID"));
//    }


}