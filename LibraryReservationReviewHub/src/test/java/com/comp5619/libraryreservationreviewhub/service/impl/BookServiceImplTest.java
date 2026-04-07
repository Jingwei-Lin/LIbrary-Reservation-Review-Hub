package com.comp5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.model.dto.book.BookAddRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private User adminUser;
    private User normalUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(99L);
        adminUser.setIsAdmin(true);

        normalUser = new User();
        normalUser.setId(1L);
        normalUser.setIsAdmin(false);

        testBook = new Book();
        testBook.setBookId(1L);
        testBook.setTitle("Clean Code");
        testBook.setAuthor("Robert Martin");
        testBook.setGenre("Programming");
        testBook.setQuantity(5);
        testBook.setAvailable(true);
        testBook.setHeldQuantity(2);
        testBook.setPath("/covers/clean.jpg");
        testBook.setDescription("Best practices");
    }

    // =====================================================================
    // 1. addBook()
    // =====================================================================

    @Test
    void addBook_Success() {
        BookAddRequest req = new BookAddRequest();
        req.setTitle("Clean Code");
        req.setAuthor("Robert Martin");
        req.setGenre("Programming");
        req.setQuantity(5);
        req.setAvailable(true);

        when(userService.isAdmin(adminUser)).thenReturn(true);
        when(bookMapper.countByTag(any(), any(), any())).thenReturn(0L);
        when(bookMapper.insertBook(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setBookId(1L);
            return 1;
        });

        Long id = bookService.addBook(req, adminUser);

        assertEquals(1L, id);
        verify(bookMapper).insertBook(argThat(b ->
                "Clean Code".equals(b.getTitle()) &&
                        "Robert Martin".equals(b.getAuthor()) &&
                        "Programming".equals(b.getGenre()) &&
                        b.getQuantity() == 5
        ));
    }

    @Test
    void addBook_NotAdmin_ThrowsException() {
        BookAddRequest req = new BookAddRequest();
        req.setTitle("Test");

        when(userService.isAdmin(normalUser)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.addBook(req, normalUser));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), ex.getCode());
    }

    @Test
    void addBook_BlankFields_ThrowsException() {
        BookAddRequest req = new BookAddRequest();
        req.setTitle(" ");
        req.setAuthor("Author");
        req.setGenre("Genre");

        when(userService.isAdmin(adminUser)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.addBook(req, adminUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        assertEquals("Book information cannot be left blank.", ex.getMessage());
    }

    @Test
    void addBook_Duplicate_ThrowsException() {
        BookAddRequest req = new BookAddRequest();
        req.setTitle("Clean Code");
        req.setAuthor("Robert Martin");
        req.setGenre("Programming");

        when(userService.isAdmin(adminUser)).thenReturn(true);
        when(bookMapper.countByTag(any(), any(), any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.addBook(req, adminUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
        assertEquals("The book already exists.", ex.getMessage());
    }

    // =====================================================================
    // 2. deleteBook()
    // =====================================================================

    @Test
    void deleteBook_Success() {
        when(userService.isAdmin(adminUser)).thenReturn(true);
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.deleteById(1L)).thenReturn(1);

        assertTrue(bookService.deleteBook(1L, adminUser));
    }

    @Test
    void deleteBook_NotAdmin_ThrowsException() {
        when(userService.isAdmin(normalUser)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.deleteBook(1L, normalUser));
        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), ex.getCode());
    }

    @Test
    void deleteBook_NotFound_ThrowsException() {
        when(userService.isAdmin(adminUser)).thenReturn(true);
        when(bookMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.deleteBook(999L, adminUser));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), ex.getCode());
    }

    // =====================================================================
    // 3. updateBook()
    // =====================================================================

    @Test
    void updateBook_Success() {
        Book update = new Book();
        update.setBookId(1L);
        update.setTitle("Clean Code Updated");

        when(userService.isAdmin(adminUser)).thenReturn(true);
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.updateBook(any(Book.class))).thenReturn(1);

        assertTrue(bookService.updateBook(update, adminUser));
    }

    @Test
    void updateBook_NotFound_ThrowsException() {
        Book update = new Book();
        update.setBookId(999L);

        when(userService.isAdmin(adminUser)).thenReturn(true);
        when(bookMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.updateBook(update, adminUser));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), ex.getCode());
    }

    // =====================================================================
    // 4. getBookById()
    // =====================================================================

    @Test
    void getBookById_Success() {
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        Book result = bookService.getBookById(1L);
        assertEquals("Clean Code", result.getTitle());
    }

    @Test
    void getBookById_NotFound_ThrowsException() {
        when(bookMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.getBookById(999L));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), ex.getCode());
    }

    // =====================================================================
    // 5. getBookVO() & getBookVOList()
    // =====================================================================

    @Test
    void getBookVO_ConvertsCorrectly() {
        BookVO vo = bookService.getBookVO(testBook);
        assertEquals(1L, vo.getBookId());
        assertEquals("Clean Code", vo.getTitle());
        assertEquals(2, vo.getHeldQuantity());
    }

    @Test
    void getBookVOList_Empty_ReturnsEmpty() {
        assertTrue(CollUtil.isEmpty(bookService.getBookVOList(Collections.emptyList())));
    }

    // =====================================================================
    // 6. searchBooks()
    // =====================================================================

    @Test
    void searchBooks_Success_WithKeyword() {
        PageRequest pageReq = new PageRequest();
        pageReq.setCurrent(1);
        pageReq.setPageSize(10);
        pageReq.setSortField("title");
        pageReq.setSortOrder("ascend");

        when(bookMapper.countByKeyword("code", true)).thenReturn(1L);
        when(bookMapper.searchByKeywordWithOrder(eq("code"), eq(true), eq(0L), eq(10L), contains("title ASC")))
                .thenReturn(List.of(testBook));

        PageResult<BookVO> result = bookService.searchBooks("code", pageReq, true);

        assertEquals(1, result.getTotal());
        assertEquals("Clean Code", result.getRecords().get(0).getTitle());
    }

    @Test
    void searchBooks_NoResults_ReturnsEmpty() {
        PageRequest pageReq = new PageRequest();
        pageReq.setCurrent(1);
        pageReq.setPageSize(10);

        when(bookMapper.countByKeyword("xyz", null)).thenReturn(0L);

        PageResult<BookVO> result = bookService.searchBooks("xyz", pageReq, null);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void searchBooks_SortWhitelist_PreventsSQLInjection() {
        PageRequest pageReq = new PageRequest();
        pageReq.setSortField("1=1; DROP TABLE book");

        when(bookMapper.countByKeyword("", true)).thenReturn(1L);
        when(bookMapper.searchByKeywordWithOrder(eq(""), eq(true), eq(0L), eq(10L), contains("book_id DESC")))
                .thenReturn(List.of(testBook));

        PageResult<BookVO> result = bookService.searchBooks("", pageReq, true);
        assertEquals(1, result.getTotal());
    }

    // =====================================================================
    // 7. getBooksByGenre()
    // =====================================================================

    @Test
    void getBooksByGenre_Success() {
        when(bookMapper.selectByGenre("Programming")).thenReturn(List.of(testBook));

        List<BookVO> result = bookService.getBooksByGenre("Programming");
        assertEquals(1, result.size());
        assertEquals("Clean Code", result.get(0).getTitle());
    }

    @Test
    void getBooksByGenre_Blank_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookService.getBooksByGenre(" "));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    // =====================================================================
    // 8. isBookAvailable()
    // =====================================================================

    @Test
    void isBookAvailable_True() {
        testBook.setQuantity(3);
        testBook.setAvailable(true);
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        assertTrue(bookService.isBookAvailable(1L));
    }

    @Test
    void isBookAvailable_False_QuantityZero() {
        testBook.setQuantity(0);
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        assertFalse(bookService.isBookAvailable(1L));
    }

    @Test
    void isBookAvailable_False_NotAvailable() {
        testBook.setAvailable(false);
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        assertFalse(bookService.isBookAvailable(1L));
    }

    // =====================================================================
    // 9. incrementHeldQuantity() & decrementHeldQuantity()
    // =====================================================================

    @Test
    void incrementHeldQuantity_Success() {
        when(bookMapper.incrementHeldQuantity(1L)).thenReturn(1);
        assertTrue(bookService.incrementHeldQuantity(1L));
    }

    @Test
    void decrementHeldQuantity_Success() {
        when(bookMapper.decrementHeldQuantity(1L)).thenReturn(1);
        assertTrue(bookService.decrementHeldQuantity(1L));
    }

    // =====================================================================
    // 10. restoreBook()
    // =====================================================================

    @Test
    void restoreBook_Success() {
        testBook.setAvailable(false);
        when(userService.isAdmin(adminUser)).thenReturn(true);
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.restoreById(1L)).thenReturn(1);

        assertTrue(bookService.restoreBook(1L, adminUser));
    }

    @Test
    void restoreBook_AlreadyAvailable_Idempotent() {
        testBook.setAvailable(true);
        when(userService.isAdmin(adminUser)).thenReturn(true);
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        assertTrue(bookService.restoreBook(1L, adminUser));
        verify(bookMapper, never()).restoreById(anyLong());
    }

    // =====================================================================
    // 11. getRandomBooks()
    // =====================================================================

    @Test
    void getRandomBooks_ReturnsLimitedList() {
        PageRequest pr = new PageRequest();
        pr.setCurrent(1);
        pr.setPageSize(100);

        when(bookMapper.countByKeyword("", true)).thenReturn(5L);
        when(bookMapper.searchByKeywordWithOrder(eq(""), eq(true), eq(0L), eq(100L), any()))
                .thenReturn(List.of(testBook));

        List<BookVO> result = bookService.getRandomBooks(3);

        assertEquals(1, result.size()); // Only 1 book in mock
    }

    @Test
    void getRandomBooks_Empty_ReturnsEmpty() {
        PageRequest pr = new PageRequest();
        pr.setCurrent(1);
        pr.setPageSize(100);

        when(bookMapper.countByKeyword("", true)).thenReturn(0L);

        assertTrue(bookService.getRandomBooks(5).isEmpty());
    }
}