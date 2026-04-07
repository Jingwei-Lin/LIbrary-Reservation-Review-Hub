package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.model.vo.BookRatingVO;
import com.comp5619.libraryreservationreviewhub.model.vo.BookWithRatingVO;
import com.comp5619.libraryreservationreviewhub.service.BookRatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookRatingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookRatingService bookRatingService;

    @InjectMocks
    private BookRatingController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /* ---------- GET /book-rating/{bookId} ---------- */

    @Test
    void getOne_success() throws Exception {
        BookRatingVO vo = new BookRatingVO();
        vo.setBookId(10L);
        vo.setAvgRating(new BigDecimal("4.2"));

        when(bookRatingService.getByBookId(10L)).thenReturn(vo);

        mockMvc.perform(get("/book-rating/10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.bookId").value(10))
                .andExpect(jsonPath("$.data.avgRating").value(4.2));

        verify(bookRatingService).getByBookId(10L);
    }

    /* ---------- GET /book-rating?ids=1,2,3 ---------- */

    @Test
    void getBatch_success_andParsesIds() throws Exception {
        BookRatingVO v1 = new BookRatingVO();
        v1.setBookId(7L);
        v1.setAvgRating(new BigDecimal("4.8"));

        BookRatingVO v2 = new BookRatingVO();
        v2.setBookId(8L);
        v2.setAvgRating(new BigDecimal("4.7"));

        when(bookRatingService.getByBookIds(anyList())).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/book-rating").param("ids", "7, 8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].bookId").value(7))
                .andExpect(jsonPath("$.data[0].avgRating").value(4.8))
                .andExpect(jsonPath("$.data[1].bookId").value(8))
                .andExpect(jsonPath("$.data[1].avgRating").value(4.7));

        // 校验参数解析是否为 [7,8]
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookRatingService).getByBookIds(captor.capture());
        assertEquals(List.of(7L, 8L), captor.getValue());
    }

    /* ---------- POST /book-rating/{bookId}/recalc ---------- */

    @Test
    void recalc_success_callsService() throws Exception {
        mockMvc.perform(post("/book-rating/15/recalc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(bookRatingService).recalc(15L);
    }

    /* ---------- GET /book-rating/top10 ---------- */

    @Test
    void top10_success() throws Exception {
        BookRatingVO v1 = new BookRatingVO();
        v1.setBookId(1L);
        v1.setAvgRating(new BigDecimal("4.9"));

        BookRatingVO v2 = new BookRatingVO();
        v2.setBookId(2L);
        v2.setAvgRating(new BigDecimal("4.6"));

        when(bookRatingService.top10()).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/book-rating/top10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].bookId").value(1))
                .andExpect(jsonPath("$.data[0].avgRating").value(4.9))
                .andExpect(jsonPath("$.data[1].bookId").value(2))
                .andExpect(jsonPath("$.data[1].avgRating").value(4.6));

        verify(bookRatingService).top10();
    }

    /* ---------- GET /book-rating/top10-books ---------- */

    @Test
    void top10Books_success() throws Exception {
        BookWithRatingVO b1 = new BookWithRatingVO();
        b1.setBookId(7L);
        b1.setTitle("Clean Code");
        b1.setAuthor("Robert C. Martin");
        b1.setGenre("SE");
        b1.setDescription("Classic.");
        b1.setPath("cleancode.jpg");
        b1.setQuantity(3);
        b1.setHeldQuantity(1);
        b1.setAvailable(true);
        b1.setAvgRating(new BigDecimal("4.9"));

        BookWithRatingVO b2 = new BookWithRatingVO();
        b2.setBookId(8L);
        b2.setTitle("Refactoring");
        b2.setAuthor("Martin Fowler");
        b2.setGenre("SE");
        b2.setDescription("Refactor techniques.");
        b2.setPath("refactoring.jpg");
        b2.setQuantity(2);
        b2.setHeldQuantity(0);
        b2.setAvailable(true);
        b2.setAvgRating(new BigDecimal("4.6"));

        when(bookRatingService.top10Books()).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/book-rating/top10-books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].bookId").value(7))
                .andExpect(jsonPath("$.data[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.data[0].author").value("Robert C. Martin"))
                .andExpect(jsonPath("$.data[0].avgRating").value(4.9))
                .andExpect(jsonPath("$.data[1].bookId").value(8))
                .andExpect(jsonPath("$.data[1].title").value("Refactoring"))
                .andExpect(jsonPath("$.data[1].author").value("Martin Fowler"))
                .andExpect(jsonPath("$.data[1].avgRating").value(4.6));

        verify(bookRatingService).top10Books();
    }
}
