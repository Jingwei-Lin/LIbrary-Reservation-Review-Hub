package com.comp5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.mapper.ReviewMapper;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private UserService userService;

    @Mock
    private BookService bookService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private User testAdmin;
    private Review testReview;
    private ReviewVO testReviewVO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Jason");
        testUser.setLastName("Wang");

        testAdmin = new User();
        testAdmin.setId(2L);
        testAdmin.setIsAdmin(true);

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
    void addReview_Success() {
        ReviewAddRequest req = new ReviewAddRequest();
        req.setRating(5);
        req.setComment("Great book!");
        req.setBookId(1L);

        when(bookService.isBookAvailable(1L)).thenReturn(true);
        when(reviewMapper.insertReview(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setReviewId(1L);
            return 1;
        });

        long reviewId = reviewService.addReview(1L, req, testUser);

        assertEquals(1L, reviewId);
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insertReview(captor.capture());
        Review captured = captor.getValue();
        assertEquals(1L, captured.getUserId());
        assertEquals(1L, captured.getBookId());
        assertEquals(5, captured.getRating());
        assertEquals("Great book!", captured.getComment());
        assertNotNull(captured.getReviewDate());
    }

    @Test
    void addReview_InvalidBookId_ThrowsException() {
        ReviewAddRequest req = new ReviewAddRequest();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.addReview(null, req, testUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void addReview_UserNotLoggedIn_ThrowsException() {
        ReviewAddRequest req = new ReviewAddRequest();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.addReview(1L, req, null));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void addReview_BookNotAvailable_ThrowsException() {
        ReviewAddRequest req = new ReviewAddRequest();
        req.setRating(5);
        req.setComment("Great book!");

        when(bookService.isBookAvailable(1L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.addReview(1L, req, testUser));
        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
    }

    @Test
    void addReview_InsertFailed_ThrowsException() {
        ReviewAddRequest req = new ReviewAddRequest();
        req.setRating(5);
        req.setComment("Great book!");

        when(bookService.isBookAvailable(1L)).thenReturn(true);
        when(reviewMapper.insertReview(any(Review.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.addReview(1L, req, testUser));
        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
    }

    @Test
    void deleteReview_Success_AsOwner() {
        when(reviewMapper.selectById(1L)).thenReturn(testReview);
        when(reviewMapper.deleteById(1L)).thenReturn(1);

        boolean result = reviewService.deleteReview(1L, testUser);

        assertTrue(result);
        verify(reviewMapper).deleteById(1L);
    }

    @Test
    void deleteReview_Success_AsAdmin() {
        testReview.setUserId(3L); // Different user
        when(reviewMapper.selectById(1L)).thenReturn(testReview);
        when(userService.isAdmin(testAdmin)).thenReturn(true);
        when(reviewMapper.deleteById(1L)).thenReturn(1);

        boolean result = reviewService.deleteReview(1L, testAdmin);

        assertTrue(result);
        verify(reviewMapper).deleteById(1L);
    }

    @Test
    void deleteReview_InvalidReviewId_ThrowsException() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.deleteReview(null, testUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void deleteReview_NotFound_ThrowsException() {
        when(reviewMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.deleteReview(1L, testUser));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
    }

    @Test
    void deleteReview_NotAuthorized_ThrowsException() {
        testReview.setUserId(3L);
        when(reviewMapper.selectById(1L)).thenReturn(testReview);
        when(userService.isAdmin(testUser)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.deleteReview(1L, testUser));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void deleteReview_DeleteFailed_ThrowsException() {
        when(reviewMapper.selectById(1L)).thenReturn(testReview);
        when(reviewMapper.deleteById(1L)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.deleteReview(1L, testUser));
        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getReviewVO_Success() {
        when(userService.getById(1L)).thenReturn(testUser);

        ReviewVO vo = reviewService.getReviewVO(testReview);

        assertNotNull(vo);
        assertEquals(1L, vo.getReviewId());
        assertEquals("Jason Wang", vo.getUserName());
    }

    @Test
    void getReviewVO_NullReview_ReturnsNull() {
        ReviewVO vo = reviewService.getReviewVO(null);
        assertNull(vo);
    }

    @Test
    void getReviewVOList_Success() {
        List<Review> reviews = Collections.singletonList(testReview);
        when(userService.getById(1L)).thenReturn(testUser);

        List<ReviewVO> vos = reviewService.getReviewVOList(reviews);

        assertEquals(1, vos.size());
        assertEquals("Jason Wang", vos.get(0).getUserName());
    }

    @Test
    void getReviewVOList_EmptyList_ReturnsEmpty() {
        List<ReviewVO> vos = reviewService.getReviewVOList(CollUtil.newArrayList());
        assertTrue(vos.isEmpty());
    }

    @Test
    void getReviewsByBookId_Success() {
        when(reviewMapper.selectByBookId(1L)).thenReturn(Collections.singletonList(testReview));
        when(userService.getById(1L)).thenReturn(testUser);

        List<ReviewVO> vos = reviewService.getReviewsByBookId(1L);

        assertEquals(1, vos.size());
    }

    @Test
    void getReviewsByBookId_InvalidId_ThrowsException() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.getReviewsByBookId(0L));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getReviewsByUserId_Success() {
        when(reviewMapper.selectByUserId(1L)).thenReturn(Collections.singletonList(testReview));
        when(userService.getById(1L)).thenReturn(testUser);

        List<ReviewVO> vos = reviewService.getReviewsByUserId(1L);

        assertEquals(1, vos.size());
    }

    @Test
    void getReviewsByUserId_InvalidId_ThrowsException() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.getReviewsByUserId(null));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateReview_Success_AsOwner() {
        ReviewUpdateRequest req = new ReviewUpdateRequest();
        req.setReviewId(1L);
        req.setRating(4);
        req.setComment("Good book");

        when(reviewMapper.selectById(1L)).thenReturn(testReview);
        when(reviewMapper.updateReview(any(Review.class))).thenReturn(1);

        boolean result = reviewService.updateReview(req, testUser);

        assertTrue(result);
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).updateReview(captor.capture());
        Review captured = captor.getValue();
        assertEquals(1L, captured.getReviewId());
        assertEquals(4, captured.getRating());
        assertEquals("Good book", captured.getComment());
        assertNotNull(captured.getReviewDate());
    }

    @Test
    void updateReview_Success_AsAdmin() {
        ReviewUpdateRequest req = new ReviewUpdateRequest();
        req.setReviewId(1L);
        req.setRating(4);
        req.setComment("Good book");

        testReview.setUserId(3L);
        when(reviewMapper.selectById(1L)).thenReturn(testReview);
        when(userService.isAdmin(testAdmin)).thenReturn(true);
        when(reviewMapper.updateReview(any(Review.class))).thenReturn(1);

        boolean result = reviewService.updateReview(req, testAdmin);

        assertTrue(result);
    }

    @Test
    void updateReview_InvalidReviewId_ThrowsException() {
        ReviewUpdateRequest req = new ReviewUpdateRequest();
        req.setReviewId(0L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.updateReview(req, testUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateReview_InvalidRating_ThrowsException() {
        ReviewUpdateRequest req = new ReviewUpdateRequest();
        req.setReviewId(1L);
        req.setRating(6);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.updateReview(req, testUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateReview_EmptyComment_ThrowsException() {
        ReviewUpdateRequest req = new ReviewUpdateRequest();
        req.setReviewId(1L);
        req.setRating(5);
        req.setComment("");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.updateReview(req, testUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateReview_NotFound_ThrowsException() {
        ReviewUpdateRequest req = new ReviewUpdateRequest();
        req.setReviewId(1L);
        req.setRating(5);
        req.setComment("Test");

        when(reviewMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.updateReview(req, testUser));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateReview_NotAuthorized_ThrowsException() {
        ReviewUpdateRequest req = new ReviewUpdateRequest();
        req.setReviewId(1L);
        req.setRating(5);
        req.setComment("Test");

        testReview.setUserId(3L);
        when(reviewMapper.selectById(1L)).thenReturn(testReview);
        when(userService.isAdmin(testUser)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.updateReview(req, testUser));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateReview_UpdateFailed_ThrowsException() {
        ReviewUpdateRequest req = new ReviewUpdateRequest();
        req.setReviewId(1L);
        req.setRating(5);
        req.setComment("Test");

        when(reviewMapper.selectById(1L)).thenReturn(testReview);
        when(reviewMapper.updateReview(any(Review.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.updateReview(req, testUser));
        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getReviewById_Success() {
        when(reviewMapper.selectById(1L)).thenReturn(testReview);

        Review review = reviewService.getReviewById(1L);

        assertEquals(testReview, review);
    }

    @Test
    void getReviewById_InvalidId_ThrowsException() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.getReviewById(0L));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getReviewById_NotFound_ThrowsException() {
        when(reviewMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.getReviewById(1L));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
    }

    @Test
    void listByPage_Success() {
        ReviewQueryRequest req = new ReviewQueryRequest();
        req.setCurrent(1);
        req.setPageSize(10);
        req.setBookId(1L);
        req.setMinRating(4);
        req.setMaxRating(5);

        when(reviewMapper.countAllByQuery(1L, null, 4, 5)).thenReturn(1L);
        when(reviewMapper.selectAllByQuery(1L, null, 4, 5, 0L, 10)).thenReturn(Collections.singletonList(testReview));
        when(userService.getById(1L)).thenReturn(testUser);

        PageResult<ReviewVO> result = reviewService.listByPage(req);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("Jason Wang", result.getRecords().get(0).getUserName());
    }

    @Test
    void listByPage_DefaultParams() {
        when(reviewMapper.countAllByQuery(null, null, null, null)).thenReturn(0L);

        PageResult<ReviewVO> result = reviewService.listByPage(null);

        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void moderate_Success() {
        boolean result = reviewService.moderate(1L, "approved", testAdmin);
        assertTrue(result);
    }

    @Test
    void getUserNameByReviewId_Success() {
        when(reviewMapper.selectById(1L)).thenReturn(testReview);
        when(userService.getById(1L)).thenReturn(testUser);

        String userName = reviewService.getUserNameByReviewId(1L);

        assertEquals("Jason Wang", userName);
    }

    @Test
    void getUserNameByReviewId_InvalidId_ThrowsException() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.getUserNameByReviewId(0L));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getUserNameByReviewId_NotFound_ThrowsException() {
        when(reviewMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reviewService.getUserNameByReviewId(1L));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getUserNameByReviewId_UserNotFound_ReturnsUnknown() {
        when(reviewMapper.selectById(1L)).thenReturn(testReview);
        when(userService.getById(1L)).thenReturn(null);

        String userName = reviewService.getUserNameByReviewId(1L);

        assertEquals("Unknown User", userName);
    }
}