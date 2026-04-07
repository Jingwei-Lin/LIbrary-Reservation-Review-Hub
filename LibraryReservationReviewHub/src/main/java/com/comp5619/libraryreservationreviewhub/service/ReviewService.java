package com.comp5619.libraryreservationreviewhub.service;

import java.util.List;

// import com.comp5619.libraryreservationreviewhub.model.dto.book.ReviewAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;

import com.comp5619.libraryreservationreviewhub.common.PageResult;

public interface ReviewService {
    /**
     * 添加review，必须是登录后的
     *
     * @param bookId
     * @param req
     * @param user
     * @return 添加result
     */
    long addReview(Long bookId, ReviewAddRequest req, User user);

    /**
     * 更新review
     * 
     * @param req
     * @param user
     * @return 更新result
     */
    boolean updateReview(ReviewUpdateRequest req, User user);

    /**
     * 根据id删除review
     * 
     * @param reviewId
     * @return 是否成功
     */
    boolean deleteReview(Long reviewId, User user);

    /**
     * 根据id获取review
     * 
     * @param reviewId
     * @return review对象
     */
    Review getReviewById(long reviewId);

    /**
     * 根据用户ID获取该用户的所有评论
     *
     * @param userId 用户ID
     * @return 评论VO列表
     */
    List<ReviewVO> getReviewsByUserId(Long userId);



    /**
     * 分页输出
     * 
     * @param req
     * @return pageResult
     */
    PageResult<ReviewVO> listByPage(ReviewQueryRequest req);

    /**
     * 管理员审核评论功能
     * 
     * @param id
     * @param status
     * @param operator
     * @return succ or no
     */
    boolean moderate(long id, String status, User operator);

    /**
     * 将单个review类型转换成VO
     * 
     * @param review
     * @return 转换后VO对象
     */
    ReviewVO getReviewVO(Review review);

    /**
     * 将一组review转换成一组VO
     * 
     * @param reviewList
     * @return 返回list<reviewVO>
     */
    List<ReviewVO> getReviewVOList(List<Review> reviewList);

    /**
     * 根据BookId返还一组reviews
     *
     * @param bookId
     * @return 返回list<reviewVO>
     */
    List<ReviewVO> getReviewsByBookId(long bookId);

    String getUserNameByReviewId(long reviewId);
}
