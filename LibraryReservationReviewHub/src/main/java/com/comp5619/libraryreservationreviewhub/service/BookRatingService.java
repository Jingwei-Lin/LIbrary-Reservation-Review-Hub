package com.comp5619.libraryreservationreviewhub.service;

import com.comp5619.libraryreservationreviewhub.model.vo.BookRatingVO;
import com.comp5619.libraryreservationreviewhub.model.vo.BookWithRatingVO;

import java.util.List;

public interface BookRatingService {

    BookRatingVO getByBookId(Long bookId);

    List<BookRatingVO> getByBookIds(List<Long> bookIds);

    /** Manually recalculate (generally not necessary, as the trigger will automatically maintain it) */
    void recalc(Long bookId);

    /** A. 只返回 bookId + avgRating */
    List<BookRatingVO> top10();

    /** B. 返回完整书籍信息 + avgRating */
    List<BookWithRatingVO> top10Books();
}