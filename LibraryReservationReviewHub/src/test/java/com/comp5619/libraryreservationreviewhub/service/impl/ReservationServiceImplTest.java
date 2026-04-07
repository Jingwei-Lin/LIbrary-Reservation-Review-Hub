package com.comp5619.libraryreservationreviewhub.service.impl;

import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.mapper.ReservationMapper;
import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationUpdatePickupRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.Reservation;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.comp5619.libraryreservationreviewhub.model.vo.ReservationVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import com.comp5619.libraryreservationreviewhub.service.WaitingListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private BookService bookService;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private UserService userService;

    @Mock
    private WaitingListService waitingListService;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private User testUser;
    private Book testBook;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setStatus(1); // Active user

        testBook = new Book();
        testBook.setBookId(1L);
        testBook.setQuantity(1);
        testBook.setHeldQuantity(0);
        testBook.setAvailable(true);
        testBook.setTitle("Test Book");

        testReservation = new Reservation();
        testReservation.setReservationId(1L);
        testReservation.setUserId(1L);
        testReservation.setBookId(1L);
        testReservation.setReservationTime(new Date());
        testReservation.setPickupTime(new Date(System.currentTimeMillis() + 86400000));           // Tomorrow
        testReservation.setDueTime(new Date(System.currentTimeMillis() + 3 * 86400000L));         // 3 days from now
        testReservation.setReservationStatus("reserved");
        testReservation.setCreateTime(new Date());
        testReservation.setIsDelete(false);
    }

    @Test
    void createReservation_Success() {
        ReservationAddRequest req = new ReservationAddRequest();
        req.setPickupTime(testReservation.getPickupTime());
        req.setDueTime(testReservation.getDueTime());

        // Mock waiting list service calls
        when(bookService.getBookById(1L)).thenReturn(testBook);
        when(reservationMapper.selectActiveByBookId(1L)).thenReturn(Collections.emptyList());
        when(reservationMapper.insertReservation(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setReservationId(1L);
            return 1;
        });
        when(bookMapper.updateBook(any(Book.class))).thenReturn(1);

        // Mock the private helper method calls using spy
        ReservationServiceImpl spyService = spy(reservationService);

        // Mock the helper methods that are called internally
        doReturn(false).when(spyService).isUserNotifiedForBook(1L, 1L);
        doReturn(null).when(spyService).getNotifiedWaitingEntry(1L, 1L);

        Long reservationId = spyService.createReservation(1L, req, testUser);

        assertEquals(1L, reservationId);
        verify(bookMapper).updateBook(any(Book.class));
    }

//    @Test
//    void createReservation_BookNotAvailable_ThrowsException() {
//        ReservationAddRequest req = new ReservationAddRequest();
//        req.setPickupTime(new Date());
//        req.setDueTime(new Date(System.currentTimeMillis() + 86400000));
//
//        // Mock waiting list service calls
//        when(bookService.getBookById(1L)).thenReturn(testBook);
//        when(reservationMapper.selectActiveByBookId(1L)).thenReturn(Collections.emptyList());
//
//        // Mock the private helper method calls using spy
//        ReservationServiceImpl spyService = spy(reservationService);
//        doReturn(false).when(spyService).isUserNotifiedForBook(1L, 1L);
//
//        // Set book to be unavailable
//        testBook.setAvailable(false);
//
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> spyService.createReservation(1L, req, testUser));
//        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
//        assertEquals("No available copies", exception.getMessage());
//    }
//
//    @Test
//    void createReservation_UserHasActiveReservation_ThrowsException() {
//        ReservationAddRequest req = new ReservationAddRequest();
//        req.setPickupTime(new Date());
//        req.setDueTime(new Date(System.currentTimeMillis() + 86400000));
//
//        when(bookService.getBookById(1L)).thenReturn(testBook);
//        when(reservationMapper.selectActiveByBookId(1L)).thenReturn(List.of(testReservation));
//
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> reservationService.createReservation(1L, req, testUser));
//        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
//    }

    @Test
    void createReservation_InvalidPickupDate_ThrowsException() {
        ReservationAddRequest req = new ReservationAddRequest();
        req.setPickupTime(new Date(System.currentTimeMillis() - 86400000)); // Past date
        req.setDueTime(new Date(System.currentTimeMillis() + 86400000));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reservationService.createReservation(1L, req, testUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("Pickup date cannot be in the past", exception.getMessage());
    }

    @Test
    void createReservation_DueDateBeforePickup_ThrowsException() {
        ReservationAddRequest req = new ReservationAddRequest();
        req.setPickupTime(new Date(System.currentTimeMillis() + 86400000));
        req.setDueTime(new Date(System.currentTimeMillis()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reservationService.createReservation(1L, req, testUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("Due date must be after pickup date", exception.getMessage());
    }

//    @Test
//    void cancelReservation_Success() {
//        when(reservationMapper.selectById(1L)).thenReturn(testReservation);
//        when(userService.isAdmin(testUser)).thenReturn(false);
//        when(reservationMapper.cancelById(1L)).thenReturn(1);
//        when(bookService.getBookById(1L)).thenReturn(testBook);
//        when(bookMapper.updateBook(any(Book.class))).thenReturn(1);
//
//        // Mock the private helper method calls using spy
//        ReservationServiceImpl spyService = spy(reservationService);
//        doReturn(0L).when(spyService).getWaitingUserCount(1L);
//
//        boolean result = spyService.cancelReservation(1L, testUser);
//
//        assertTrue(result);
//        verify(bookMapper).updateBook(any(Book.class));
//    }

    @Test
    void cancelReservation_NotOwner_ThrowsException() {
        testReservation.setUserId(2L); // Different user
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reservationService.cancelReservation(1L, testUser));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updateReservationStatus_Success() {
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);
        when(userService.isAdmin(testUser)).thenReturn(true);
        when(reservationMapper.updateStatus(1L, "borrowed")).thenReturn(1);

        boolean result = reservationService.updateReservationStatus(1L, "borrowed", testUser);

        assertTrue(result);
    }

    @Test
    void updateReservationStatus_NotAdmin_ThrowsException() {
        when(userService.isAdmin(testUser)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reservationService.updateReservationStatus(1L, "borrowed", testUser));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getReservationById_Success() {
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);

        Reservation result = reservationService.getReservationById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getReservationId());
    }

    @Test
    void getReservationById_NotFound_ThrowsException() {
        when(reservationMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reservationService.getReservationById(1L));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
    }

    @Test
    void getReservationsByUser_Success() {
        when(reservationMapper.selectByUserId(1L)).thenReturn(List.of(testReservation));
        when(bookService.getBookById(1L)).thenReturn(testBook);

        List<ReservationVO> result = reservationService.getReservationsByUser(1L);

        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getBookTitle());
    }

    @Test
    void getAllReservations_Success() {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setCurrent(1);
        pageRequest.setPageSize(10);

        when(userService.isAdmin(testUser)).thenReturn(true);
        when(reservationMapper.selectAll()).thenReturn(List.of(testReservation));
        when(bookService.getBookById(1L)).thenReturn(testBook);

        PageResult<ReservationVO> result = reservationService.getAllReservations(pageRequest, testUser);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void getAllReservations_NotAdmin_ThrowsException() {
        PageRequest pageRequest = new PageRequest();

        when(userService.isAdmin(testUser)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reservationService.getAllReservations(pageRequest, testUser));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void hasActiveReservation_True() {
        when(reservationMapper.selectActiveByBookId(1L)).thenReturn(List.of(testReservation));

        boolean result = reservationService.hasActiveReservation(1L, 1L);

        assertTrue(result);
    }

    @Test
    void hasActiveReservation_False() {
        when(reservationMapper.selectActiveByBookId(1L)).thenReturn(Collections.emptyList());

        boolean result = reservationService.hasActiveReservation(1L, 1L);

        assertFalse(result);
    }

    @Test
    void updatePickupTime_Success() {
        ReservationUpdatePickupRequest req = new ReservationUpdatePickupRequest();
        req.setReservationId(1L);
        req.setPickupTime(new Date(System.currentTimeMillis() + 2 * 86400000)); // Future date

        when(reservationMapper.selectById(1L)).thenReturn(testReservation);
        when(reservationMapper.updatePickupTime(1L, req.getPickupTime())).thenReturn(1);

        boolean result = reservationService.updatePickupTime(req, testUser);

        assertTrue(result);
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(reservationMapper).updatePickupTime(eq(1L), captor.capture());
        assertEquals(req.getPickupTime(), captor.getValue());
    }

    @Test
    void updatePickupTime_NotOwnerOrAdmin_ThrowsException() {
        ReservationUpdatePickupRequest req = new ReservationUpdatePickupRequest();
        req.setReservationId(1L);
        req.setPickupTime(new Date());

        testReservation.setUserId(2L);
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);
        when(userService.isAdmin(testUser)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reservationService.updatePickupTime(req, testUser));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updatePickupTime_InvalidStatus_ThrowsException() {
        ReservationUpdatePickupRequest req = new ReservationUpdatePickupRequest();
        req.setReservationId(1L);
        req.setPickupTime(new Date());

        testReservation.setReservationStatus("borrowed");
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reservationService.updatePickupTime(req, testUser));
        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
    }

    @Test
    void updatePickupTime_PastDate_ThrowsException() {
        ReservationUpdatePickupRequest req = new ReservationUpdatePickupRequest();
        req.setReservationId(1L);
        req.setPickupTime(new Date(System.currentTimeMillis() - 86400000)); // Past

        when(reservationMapper.selectById(1L)).thenReturn(testReservation);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reservationService.updatePickupTime(req, testUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("New pickup date cannot be in the past", exception.getMessage());
    }

    @Test
    void updatePickupTime_AfterDueTime_ThrowsException() {
        ReservationUpdatePickupRequest req = new ReservationUpdatePickupRequest();
        req.setReservationId(1L);
        req.setPickupTime(new Date(System.currentTimeMillis() + 3 * 86400000)); // After due

        when(reservationMapper.selectById(1L)).thenReturn(testReservation);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> reservationService.updatePickupTime(req, testUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("New pickup time must be before due time", exception.getMessage());
    }
}
