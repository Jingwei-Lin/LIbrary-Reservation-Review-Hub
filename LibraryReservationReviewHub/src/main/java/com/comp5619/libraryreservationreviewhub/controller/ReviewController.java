package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;
import com.comp5619.libraryreservationreviewhub.service.ReviewService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import com.comp5619.libraryreservationreviewhub.annotation.AuthCheck;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    /**
     * Get all reviews for a specific book
     */
    @GetMapping("/book/{bookId}")
    public BaseResponse<List<ReviewVO>> get(@PathVariable long bookId) {
        return ResultUtils.success(reviewService.getReviewsByBookId(bookId));
    }

    /**
     * Add a new review for a specific book
     */
    @PostMapping("/add/{bookId}")
    public BaseResponse<Long> addReview(@PathVariable Long bookId,
                                        @RequestBody ReviewAddRequest req,
                                        HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(reviewService.addReview(bookId, req, user));
    }

    /**
     * Delete a specific review
     */
    @DeleteMapping("/delete/{reviewId}")
    public BaseResponse<Boolean> deleteReview(@PathVariable Long reviewId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(reviewService.deleteReview(reviewId, user));
    }

    /**
     * Update a specific review
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updateReview(@RequestBody ReviewUpdateRequest req, HttpServletRequest request) {
        ThrowUtils.throwIf(req == null|| req.getReviewId() <= 0, ErrorCode.PARAMS_ERROR, "Invalid review ID");
        User user = userService.getLoginUser(request);
        return ResultUtils.success(reviewService.updateReview(req, user));
    }

    /**
     * Get a specific review by ID
     */
    @GetMapping("/{reviewId}")
    public BaseResponse<ReviewVO> getReviewById(@PathVariable long reviewId) {
        ThrowUtils.throwIf(reviewId <= 0, ErrorCode.PARAMS_ERROR, "Invalid review ID");
        Review review = reviewService.getReviewById(reviewId);
        return ResultUtils.success(reviewService.getReviewVO(review));
    }

    /**
     * Get the username for a specific review
     */
    @GetMapping("/username/{reviewId}")
    public BaseResponse<String> getUserNameByReviewId(@PathVariable long reviewId) {
        ThrowUtils.throwIf(reviewId <= 0, ErrorCode.PARAMS_ERROR, "Invalid review ID");
        return ResultUtils.success(reviewService.getUserNameByReviewId(reviewId));
    }

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/all")
    public BaseResponse<PageResult<ReviewVO>> listAll(ReviewQueryRequest req) {
        return ResultUtils.success(reviewService.listByPage(req));
    }
}