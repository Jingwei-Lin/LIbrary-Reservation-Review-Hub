package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.annotation.AuthCheck;
import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.model.dto.book.BookAddRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final UserService userService;

    /* ============ 管理员操作 ============ */

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> add(@Valid @RequestBody BookAddRequest req, HttpServletRequest request) {
        User operator = userService.getLoginUser(request);
        return ResultUtils.success(bookService.addBook(req, operator));
    }

    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> delete(@PathVariable long id, HttpServletRequest request) {
        User operator = userService.getLoginUser(request);
        return ResultUtils.success(bookService.deleteBook(id, operator));
    }

    @PutMapping("/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> update(@PathVariable Long id,
                                        @RequestBody Book book,
                                        HttpServletRequest request) {
        // 注意：实体为 bookId 而不是 id
        book.setBookId(id.longValue());
        User operator = userService.getLoginUser(request);
        return ResultUtils.success(bookService.updateBook(book, operator));
    }

    @PutMapping("/{id}/restore")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> restore(@PathVariable long id, HttpServletRequest request) {
        User operator = userService.getLoginUser(request);
        return ResultUtils.success(bookService.restoreBook(id, operator));
    }

    /* ============ 查询 ============ */

    @GetMapping("/{id}")
    public BaseResponse<BookVO> get(@PathVariable long id) {
        return ResultUtils.success(bookService.getBookVO(bookService.getBookById(id)));
    }

    /**
     * 搜索：
     * GET /book/search?keyword=xxx&available=true&current=1&pageSize=10&sortField=title&sortOrder=asc
     */
    @GetMapping("/search")
    public BaseResponse<PageResult<BookVO>> search(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Boolean available,
                                                   PageRequest pageReq) {
        return ResultUtils.success(bookService.searchBooks(keyword, pageReq, available));
    }

    /**
     * 按类别查询：/book/genre/Fantasy
     */
    @GetMapping("/genre/{genre}")
    public BaseResponse<List<BookVO>> byGenre(@PathVariable String genre) {
        return ResultUtils.success(bookService.getBooksByGenre(genre));
    }
}
