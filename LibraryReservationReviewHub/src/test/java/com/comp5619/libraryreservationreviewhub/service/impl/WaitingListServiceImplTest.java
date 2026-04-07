package com.comp5619.libraryreservationreviewhub.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.mapper.WaitingListMapper;
import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListCreateDTO;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListQueryDTO;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListUpdateDTO;
import com.comp5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.comp5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.comp5619.libraryreservationreviewhub.model.vo.WaitingListVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WaitingListServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class WaitingListServiceImplTest {

    @Mock
    private WaitingListMapper waitingListMapper;

    @Mock
    private BookService bookService;

    @InjectMocks
    private WaitingListServiceImpl service;

    @BeforeEach
    void injectBaseMapper() {
        // ServiceImpl 父类里的 baseMapper 用反射塞入
        ReflectionTestUtils.setField(service, "baseMapper", waitingListMapper);
    }

    /* -------------------- create -------------------- */

    @Test
    void create_success() {
        WaitingListCreateDTO dto = new WaitingListCreateDTO();
        dto.setUserId(1L);
        dto.setBookId(100L);

        // insert 时顺便给实体设置主键
        doAnswer(inv -> {
            WaitingList e = inv.getArgument(0);
            e.setWaitId(10L);
            return 1;
        }).when(waitingListMapper).insert(any(WaitingList.class));

        Long id = service.create(dto);
        assertEquals(10L, id);
        verify(waitingListMapper, times(1)).insert(any(WaitingList.class));
    }

    /* -------------------- updateStatus -------------------- */

    @Test
    void updateStatus_success() {
        WaitingList db = new WaitingList();
        db.setWaitId(5L);
        db.setStatus(WaitingStatusEnum.WAITING);
        db.setIsNotified(false);

        when(waitingListMapper.selectById(5L)).thenReturn(db);
        when(waitingListMapper.updateById(any(WaitingList.class))).thenReturn(1);

        WaitingListUpdateDTO dto = new WaitingListUpdateDTO();
        dto.setWaitId(5L);
        dto.setStatus(WaitingStatusEnum.NOTIFIED);
        dto.setIsNotified(true);
        dto.setNotificationDate(LocalDateTime.of(2025, 1, 2, 12, 0, 0));

        boolean ok = service.updateStatus(dto);
        assertTrue(ok);

        ArgumentCaptor<WaitingList> cap = ArgumentCaptor.forClass(WaitingList.class);
        verify(waitingListMapper).updateById(cap.capture());
        WaitingList updated = cap.getValue();
        assertEquals(WaitingStatusEnum.NOTIFIED, updated.getStatus());
        assertEquals(true, updated.getIsNotified());
        assertEquals(LocalDateTime.of(2025,1,2,12,0,0), updated.getNotificationDate());
    }

    /* -------------------- deleteById -------------------- */

    @Test
    void deleteById_success() {
        when(waitingListMapper.deleteById(9L)).thenReturn(1);
        assertTrue(service.deleteById(9L));
        verify(waitingListMapper).deleteById(9L);
    }

    /* -------------------- getById -------------------- */

    @Test
    void getById_success() {
        WaitingList e = new WaitingList();
        e.setWaitId(7L);
        e.setUserId(1L);
        e.setBookId(100L);
        e.setStatus(WaitingStatusEnum.WAITING);
        when(waitingListMapper.selectById(7L)).thenReturn(e);

        var vo = service.getById(7L);
        assertEquals(7L, vo.getWaitId());
        assertEquals(1L, vo.getUserId());
        assertEquals(100L, vo.getBookId());
        assertEquals(WaitingStatusEnum.WAITING, vo.getStatus());
    }

    /* -------------------- listByQuery -------------------- */

    @Test
    void listByQuery_success() {
        WaitingList e1 = new WaitingList();
        e1.setWaitId(1L); e1.setUserId(1L); e1.setBookId(100L); e1.setStatus(WaitingStatusEnum.WAITING);
        WaitingList e2 = new WaitingList();
        e2.setWaitId(2L); e2.setUserId(1L); e2.setBookId(101L); e2.setStatus(WaitingStatusEnum.CANCELLED);

        when(waitingListMapper.selectList(any())).thenReturn(List.of(e1, e2));

        WaitingListQueryDTO q = new WaitingListQueryDTO();
        q.setUserId(1L);

        List<WaitingListVO> list = service.listByQuery(q);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getUserId());
        assertEquals(100L, list.get(0).getBookId());
    }

    /* -------------------- listByUserWithDetails -------------------- */

    @Test
    void listByUserWithDetails_success() {
        WaitingListVO vo1 = new WaitingListVO();
        vo1.setWaitId(11L); vo1.setUserId(1L); vo1.setBookId(100L); vo1.setStatus(WaitingStatusEnum.WAITING);
        WaitingListVO vo2 = new WaitingListVO();
        vo2.setWaitId(12L); vo2.setUserId(1L); vo2.setBookId(101L); vo2.setStatus(WaitingStatusEnum.NOTIFIED);

        when(waitingListMapper.listByUser(1L)).thenReturn(List.of(vo1, vo2));

        List<WaitingListVO> out = service.listByUserWithDetails(1L);
        assertEquals(2, out.size());
        assertEquals(11L, out.get(0).getWaitId());
    }

    /* -------------------- pageByQuery -------------------- */

    @Test
    void pageByQuery_success() {
        // total = 2
        when(waitingListMapper.selectCount(any())).thenReturn(2L);

        WaitingList e1 = new WaitingList();
        e1.setWaitId(1L); e1.setUserId(1L); e1.setBookId(100L); e1.setStatus(WaitingStatusEnum.WAITING);
        WaitingList e2 = new WaitingList();
        e2.setWaitId(2L); e2.setUserId(1L); e2.setBookId(101L); e2.setStatus(WaitingStatusEnum.WAITING);

        Page<WaitingList> mpPage = new Page<>(1, 10, 2);
        mpPage.setRecords(List.of(e1, e2));
        when(waitingListMapper.selectPage(any(), any())).thenReturn(mpPage);

        WaitingListQueryDTO q = new WaitingListQueryDTO();
        q.setUserId(1L);

        PageRequest pr = new PageRequest();
        pr.setCurrent(1);
        pr.setPageSize(10);
        pr.setSortField("wait_id"); // 在白名单里
        pr.setSortOrder("descend");

        PageResult<WaitingListVO> page = service.pageByQuery(q, pr);
        assertEquals(2L, page.getTotal());
        assertEquals(2, page.getRecords().size());
        assertEquals(1L, page.getRecords().get(0).getUserId());
    }

    /* -------------------- computeNotifyEligibilityForUser -------------------- */

    @Test
    void computeNotifyEligibility_noRows() {
        when(waitingListMapper.selectNotifyEligibilityForUser(1L)).thenReturn(List.of());

        List<NotifyEligibilityVO> out = service.computeNotifyEligibilityForUser(1L);
        assertTrue(out.isEmpty());
        verify(waitingListMapper, never()).batchNotifyByBooks(anyLong(), anyList());
    }

    @Test
    void computeNotifyEligibility_notEligible_whenHeldZeroEvenIfQuantityPositive() {
        // held = 0, quantity > 0，但 position<=held 不满足 -> 不可通知
        NotifyEligibilityVO vo = new NotifyEligibilityVO();
        vo.setBookId(100L);
        vo.setHeldQuantity(0);
        vo.setQuantity(3);
        vo.setPosition(1);
        vo.setNotifiedCount(0);

        when(waitingListMapper.selectNotifyEligibilityForUser(1L)).thenReturn(List.of(vo));

        List<NotifyEligibilityVO> out = service.computeNotifyEligibilityForUser(1L);
        assertEquals(1, out.size());
        assertEquals(Boolean.FALSE, out.get(0).getCanNotify());

        verify(waitingListMapper, never()).batchNotifyByBooks(anyLong(), anyList());
    }

    @Test
    void computeNotifyEligibility_canNotify_andBatchUpdateCalled() {
        // 满足条件：held=2, position=1, notifiedCount=0
        NotifyEligibilityVO vo = new NotifyEligibilityVO();
        vo.setBookId(200L);
        vo.setHeldQuantity(2);
        vo.setQuantity(0);
        vo.setPosition(1);
        vo.setNotifiedCount(0);

        when(waitingListMapper.selectNotifyEligibilityForUser(5L)).thenReturn(List.of(vo));
        when(waitingListMapper.batchNotifyByBooks(eq(5L), anyList())).thenReturn(1);

        List<NotifyEligibilityVO> out = service.computeNotifyEligibilityForUser(5L);
        assertEquals(1, out.size());
        assertEquals(Boolean.TRUE, out.get(0).getCanNotify());

        ArgumentCaptor<List<Long>> idsCap = ArgumentCaptor.forClass(List.class);
        verify(waitingListMapper).batchNotifyByBooks(eq(5L), idsCap.capture());
        assertEquals(List.of(200L), idsCap.getValue());
    }

    /* -------------------- cancelStaleWaiting -------------------- */

    @Test
    void cancelStaleWaiting_success() {
        when(waitingListMapper.cancelStaleWaiting()).thenReturn(3);
        int rows = service.cancelStaleWaiting();
        assertEquals(3, rows);
        verify(waitingListMapper).cancelStaleWaiting();
    }
}
