package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.annotation.AuthCheck;
import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.model.vo.BookRatingVO;
import com.comp5619.libraryreservationreviewhub.model.vo.BookWithRatingVO;
import com.comp5619.libraryreservationreviewhub.service.BookRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/book-rating")
@RequiredArgsConstructor
public class BookRatingController {

    private final BookRatingService bookRatingService;

    /** 单本评分：GET /book-rating/{bookId} */
    @GetMapping("/{bookId}")
    public BaseResponse<BookRatingVO> getOne(@PathVariable Long bookId) {
        return ResultUtils.success(bookRatingService.getByBookId(bookId));
    }

    /** 批量评分：GET /book-rating?ids=1,2,3 */
    @GetMapping
    public BaseResponse<List<BookRatingVO>> getBatch(@RequestParam("ids") String ids) {
        List<Long> bookIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        return ResultUtils.success(bookRatingService.getByBookIds(bookIds));
    }

    /** 可选：管理员触发重算（正常不需要，触发器已自动维护） */
    @PostMapping("/{bookId}/recalc")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> recalc(@PathVariable Long bookId) {
        bookRatingService.recalc(bookId);
        return ResultUtils.success(true);
    }

    /** 前 10：仅返回 bookId + avgRating */
    @GetMapping("/top10")
    public BaseResponse<List<BookRatingVO>> top10() {
        return ResultUtils.success(bookRatingService.top10());
    }

    /** 前 10：返回完整书籍信息 + avgRating */
    @GetMapping("/top10-books")
    public BaseResponse<List<BookWithRatingVO>> top10Books() {
        return ResultUtils.success(bookRatingService.top10Books());
    }
}
