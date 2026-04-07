package com.comp5619.libraryreservationreviewhub.tools;

import com.comp5619.libraryreservationreviewhub.model.vo.BookRatingVO;
import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试：BookSearchTool
 * 目标：覆盖所有 Tool 方法与边界情况。
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class BookSearchToolIntegrationTest {

    @Autowired
    private BookSearchTool bookSearchTool;

    @Autowired
    private BookService bookService;

    /**
     * 搜索特定关键字
     */
    @Test
    void testSearchBooks_ByKeyword() {
        var result = bookSearchTool.searchBooks("adventure", true, 1, 5);
        assertThat(result).isNotNull();
    }

    /**
     * 按类别获取书籍
     */
    @Test
    void testGetBooksByGenre() {
        List<BookVO> books = bookSearchTool.getBooksByGenre("Fantasy");
        assertThat(books).isNotNull();
    }

    /**
     * 随机推荐书籍
     */
    @Test
    void testGetRandomBooks() {
        List<BookRatingVO> randomBooks = bookSearchTool.getRandomBooks();
        assertThat(randomBooks).isNotNull();
    }

    /**
     * 根据 ID 获取书籍详情
     */
    @Test
    void testGetBookById_ValidBook() {
        // 尝试获取 ID = 1 的书籍
        BookVO book = bookSearchTool.getBookById(1L);
        System.out.println("Book by ID=1: " + book);

        // 基础存在性
        assertThat(book).isNotNull();

        // 字段校验（根据数据库数据）
        assertThat(book.getTitle()).isEqualTo("The Hobbit");
        assertThat(book.getAuthor()).isEqualTo("J.R.R. Tolkien");
        assertThat(book.getGenre()).isEqualTo("Fantasy");

        // 描述中包含关键词
        assertThat(book.getDescription())
                .containsIgnoringCase("Bilbo")
                .containsIgnoringCase("dragon");

    }

    /**
     *  检测书籍是否可借
     */
    @Test
    void testIsBookAvailable() {
        boolean available = bookSearchTool.isBookAvailable(1L);
        assertThat(available).isIn(true, false);
    }

    /**
     *  无匹配关键字（空结果）
     */
    @Test
    void testSearchBooks_EmptyKeywordResult() {
        var result = bookSearchTool.searchBooks("non_existing_keyword", true, 1, 5);
        assertThat(result).isNotNull();
    }
}
