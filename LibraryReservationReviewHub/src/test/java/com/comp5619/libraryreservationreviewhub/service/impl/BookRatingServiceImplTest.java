package com.comp5619.libraryreservationreviewhub.service.impl;

import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.mapper.BookRatingMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.BookRating;
import com.comp5619.libraryreservationreviewhub.model.vo.BookRatingVO;
import com.comp5619.libraryreservationreviewhub.model.vo.BookWithRatingVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookRatingServiceImplTest {

    @Mock
    private BookRatingMapper bookRatingMapper;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookRatingServiceImpl service;

    /** 工具：用 compareTo 比较 BigDecimal，避免 4.2 vs 4.20 刻度问题 */
    private static void assertBdEq(String expected, BigDecimal actual) {
        assertNotNull(actual, "actual BigDecimal is null");
        assertEquals(0, actual.compareTo(new BigDecimal(expected)),
                () -> "expected " + expected + " but was " + actual);
    }

    /* ========== getByBookId ========== */

    @Test
    void getByBookId_success_found() {
        BookRating br = new BookRating();
        br.setBookId(10L);
        br.setAvgRating(new BigDecimal("4.2"));

        when(bookRatingMapper.selectById(10L)).thenReturn(br);

        BookRatingVO vo = service.getByBookId(10L);
        assertEquals(10L, vo.getBookId());
        assertBdEq("4.2", vo.getAvgRating());
    }

    @Test
    void getByBookId_success_notFound_returnsNullRating() {
        when(bookRatingMapper.selectById(99L)).thenReturn(null);

        BookRatingVO vo = service.getByBookId(99L);
        assertEquals(99L, vo.getBookId());
        assertNull(vo.getAvgRating());
    }

    @Test
    void getByBookId_invalid_throws() {
        BusinessException ex1 = assertThrows(BusinessException.class, () -> service.getByBookId(null));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex1.getCode());

        BusinessException ex2 = assertThrows(BusinessException.class, () -> service.getByBookId(0L));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex2.getCode());
    }

    /* ========== getByBookIds ========== */

    @Test
    void getByBookIds_emptyInput_returnsEmpty() {
        assertTrue(service.getByBookIds(Collections.emptyList()).isEmpty());
    }

    @Test
    void getByBookIds_filtersInvalid_andPreservesOrder_andFillsNullWhenMissing() {
        // 入参包含 null、<=0、重复
        List<Long> input = Arrays.asList(null, -1L, 0L, 3L, 3L, 5L);

        // mapper 只返回 3 的评分，5 不存在 -> 5 应该映射为 avgRating=null
        BookRating br3 = new BookRating();
        br3.setBookId(3L);
        br3.setAvgRating(new BigDecimal("4.8"));
        when(bookRatingMapper.selectBatchIds(Arrays.asList(3L, 5L))).thenReturn(List.of(br3));

        List<BookRatingVO> out = service.getByBookIds(input);
        assertEquals(2, out.size());
        assertEquals(3L, out.get(0).getBookId());
        assertBdEq("4.8", out.get(0).getAvgRating());
        assertEquals(5L, out.get(1).getBookId());
        assertNull(out.get(1).getAvgRating());
    }

    /* ========== recalc ========== */

    @Test
    void recalc_success_callsMapper() {
        service.recalc(7L);
        verify(bookRatingMapper, times(1)).recalcOne(7L);
    }

    @Test
    void recalc_invalid_throws() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.recalc(0L));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    /* ========== top10 ========== */

    @Test
    void top10_success_mapsVO() {
        BookRating b1 = new BookRating(); b1.setBookId(1L); b1.setAvgRating(new BigDecimal("4.9"));
        BookRating b2 = new BookRating(); b2.setBookId(2L); b2.setAvgRating(new BigDecimal("4.7"));

        when(bookRatingMapper.selectList(any())).thenReturn(List.of(b1, b2));

        List<BookRatingVO> list = service.top10();
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getBookId());
        assertBdEq("4.9", list.get(0).getAvgRating());
        assertEquals(2L, list.get(1).getBookId());
        assertBdEq("4.7", list.get(1).getAvgRating());

        verify(bookRatingMapper, times(1)).selectList(any());
    }

    /* ========== top10Books ========== */

    @Test
    void top10Books_success_mapsFullBookInfo_andKeepsOrder() {
        // ratings
        BookRating r1 = new BookRating(); r1.setBookId(7L); r1.setAvgRating(new BigDecimal("4.9"));
        BookRating r2 = new BookRating(); r2.setBookId(8L); r2.setAvgRating(new BigDecimal("4.6"));
        when(bookRatingMapper.selectList(any())).thenReturn(List.of(r1, r2));

        // books
        Book b7 = new Book();
        b7.setBookId(7L); b7.setTitle("Clean Code"); b7.setAuthor("Robert C. Martin");
        b7.setGenre("SE"); b7.setDescription("Classic.");
        b7.setPath("cleancode.jpg"); b7.setQuantity(3); b7.setHeldQuantity(1); b7.setAvailable(true);

        Book b8 = new Book();
        b8.setBookId(8L); b8.setTitle("Refactoring"); b8.setAuthor("Martin Fowler");
        b8.setGenre("SE"); b8.setDescription("Refactor techniques.");
        b8.setPath("refactoring.jpg"); b8.setQuantity(2); b8.setHeldQuantity(0); b8.setAvailable(true);

        when(bookMapper.selectById(7L)).thenReturn(b7);
        when(bookMapper.selectById(8L)).thenReturn(b8);

        List<BookWithRatingVO> out = service.top10Books();
        assertEquals(2, out.size());

        BookWithRatingVO vo1 = out.get(0);
        assertEquals(7L, vo1.getBookId());
        assertEquals("Clean Code", vo1.getTitle());
        assertEquals("Robert C. Martin", vo1.getAuthor());
        assertEquals("SE", vo1.getGenre());
        assertEquals("Classic.", vo1.getDescription());
        assertEquals("cleancode.jpg", vo1.getPath());
        assertEquals(3, vo1.getQuantity());
        assertEquals(1, vo1.getHeldQuantity());
        assertTrue(vo1.getAvailable());
        assertBdEq("4.9", vo1.getAvgRating());

        BookWithRatingVO vo2 = out.get(1);
        assertEquals(8L, vo2.getBookId());
        assertEquals("Refactoring", vo2.getTitle());
        assertEquals("Martin Fowler", vo2.getAuthor());
        assertBdEq("4.6", vo2.getAvgRating());
    }

    @Test
    void top10Books_skipsWhenBookMissing() {
        BookRating r1 = new BookRating(); r1.setBookId(100L); r1.setAvgRating(new BigDecimal("4.5"));
        BookRating r2 = new BookRating(); r2.setBookId(200L); r2.setAvgRating(new BigDecimal("4.4"));
        when(bookRatingMapper.selectList(any())).thenReturn(List.of(r1, r2));

        // 第一本书不存在（mapper 返回 null），第二本存在
        when(bookMapper.selectById(100L)).thenReturn(null);

        Book b200 = new Book();
        b200.setBookId(200L); b200.setTitle("Some Book"); b200.setAuthor("Some Author");
        when(bookMapper.selectById(200L)).thenReturn(b200);

        List<BookWithRatingVO> out = service.top10Books();
        assertEquals(1, out.size());
        assertEquals(200L, out.get(0).getBookId());
        assertEquals("Some Book", out.get(0).getTitle());
        assertBdEq("4.4", out.get(0).getAvgRating());
    }
}
