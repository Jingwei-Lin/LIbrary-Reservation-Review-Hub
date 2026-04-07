package com.comp5619.libraryreservationreviewhub.tools;


import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.vo.BookRatingVO;
import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
import com.comp5619.libraryreservationreviewhub.service.BookRatingService;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BookSearchTool
 *
 * 用途：
 *  - 向 AI Agent 暴露可调用的书籍搜索与推荐接口。
 *  - 所有方法均标注为 @Tool，可被 AI 模型通过自然语言调用。
 */
@Component
@Slf4j
public class BookSearchTool {

    @Autowired
    private BookService bookService;
    @Autowired
    private BookRatingService bookRatingService;


    /**
     * 搜索书籍
     * @param keyword 关键词（书名、作者、类别）
     * @param available 是否只显示可借
     * @param current 页码
     * @param pageSize 每页数量
     * @return PageResult<BookVO>
     */
    @Tool(
            name = "search_books",
            description = "Search for books by title, author name, or genre keyword. " +
                    "Use this when the user provides a keyword, name, or topic."
    )
    public PageResult<BookVO> searchBooks(String keyword, Boolean available, int current, int pageSize) {
        log.info("ToolCall: search_books, keyword={}", keyword);
        PageRequest pageRequest = new PageRequest();
        pageRequest.setCurrent(current);
        pageRequest.setPageSize(pageSize);
        return bookService.searchBooks(keyword, pageRequest, available);
    }

    /**
     * 根据类别获取书籍列表
     */
    @Tool(
            name = "get_books_by_genre",
            description = "List all books that belong to a specific genre, such as 'Fantasy' or 'Romance'. " +
                    "Use only when the user explicitly mentions a genre name."
    )
    public List<BookVO> getBooksByGenre(String genre) {
        log.info("ToolCall: get_books_by_genre, genre={}", genre);
        return bookService.getBooksByGenre(genre);
    }

    /**
     * 获取热门书籍
     */
    @Tool(
            name = "get_random_books",
            description = "Return top 10 book recommendations from the entire library catalog. " +
                    "Use this when the user asks for general, non-specific book recommendations " +
                    "such as 'recommend me some books' or 'popular books' without giving any genre or author."
    )
    public List<BookRatingVO> getRandomBooks() {

        List<BookRatingVO> books = bookRatingService.top10();
        log.info("ToolCall: get_top_books "+books);
        return books;
    }



    /**
     * 根据 ID 获取书籍详情
     */
    @Tool(name = "get_book_by_id", description = "Get detailed information for a book by ID")
    public BookVO getBookById(Long id) {
        log.info("ToolCall: get_book_by_id, id={}", id);
        Book book = bookService.getBookById(id);
        return bookService.getBookVO(book);
    }

    /**
     * 检查某本书是否可借
     */
    @Tool(name = "check_book_availability", description = "Check whether a specific book is available for borrowing")
    public boolean isBookAvailable(Long id) {
        log.info("ToolCall: check_book_availability, id={}", id);
        return bookService.isBookAvailable(id);
    }
}