package com.comp5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.mapper.BookRatingMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.BookRating;
import com.comp5619.libraryreservationreviewhub.model.vo.BookRatingVO;
import com.comp5619.libraryreservationreviewhub.model.vo.BookWithRatingVO;
import com.comp5619.libraryreservationreviewhub.service.BookRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookRatingServiceImpl implements BookRatingService {

    private final BookRatingMapper bookRatingMapper;
    private final BookMapper bookMapper;

    @Override
    public BookRatingVO getByBookId(Long bookId) {
        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "bookId 非法");
        BookRating br = bookRatingMapper.selectById(bookId);

        BookRatingVO vo = new BookRatingVO();
        if (br != null) {
            BeanUtils.copyProperties(br, vo);
        } else {
            vo.setBookId(bookId);
            vo.setAvgRating(null);
        }
        return vo;
    }

    @Override
    public List<BookRatingVO> getByBookIds(List<Long> bookIds) {
        if (CollUtil.isEmpty(bookIds)) return Collections.emptyList();

        // 过滤非法 & 去重，同时保留输入顺序以便返回时按入参顺序
        List<Long> ids = bookIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return Collections.emptyList();

        // MP 批量：主键就是 book_id
        List<BookRating> list = bookRatingMapper.selectBatchIds(ids);
        Map<Long, BookRating> map = list.stream()
                .collect(Collectors.toMap(BookRating::getBookId, x -> x));

        List<BookRatingVO> res = new ArrayList<>(ids.size());
        for (Long id : ids) {
            BookRating r = map.get(id);
            BookRatingVO vo = new BookRatingVO();
            if (r != null) {
                BeanUtils.copyProperties(r, vo);
            } else {
                vo.setBookId(id);
                vo.setAvgRating(null);
            }
            res.add(vo);
        }
        return res;
    }

    @Override
    public void recalc(Long bookId) {
        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "bookId 非法");
        bookRatingMapper.recalcOne(bookId);
    }

    /** A. 只返回 bookId + avgRating 的前 10 条 */
    @Override
    public List<BookRatingVO> top10() {
        LambdaQueryWrapper<BookRating> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(BookRating::getAvgRating)
                .orderByDesc(BookRating::getAvgRating)
                .last("LIMIT 10");

        List<BookRating> list = bookRatingMapper.selectList(qw);
        return list.stream().map(br -> {
            BookRatingVO vo = new BookRatingVO();
            BeanUtils.copyProperties(br, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /** B. 返回完整书籍信息 + avgRating 的前 10 条 */
    @Override
    public List<BookWithRatingVO> top10Books() {
        // 先取评分 top10
        LambdaQueryWrapper<BookRating> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(BookRating::getAvgRating)
                // 如希望把同分再按 book_id ASC 排序，可加 .orderByAsc(BookRating::getBookId)
                .orderByDesc(BookRating::getAvgRating)
                .last("LIMIT 10");

        List<BookRating> ratings = bookRatingMapper.selectList(qw);

        // 按顺序逐本取书（只有 10 本，性能 OK）
        List<BookWithRatingVO> res = new ArrayList<>(ratings.size());
        for (BookRating r : ratings) {
            Book book = bookMapper.selectById(r.getBookId());
            if (book == null) { // 数据不一致时兜底
                continue;
            }
            BookWithRatingVO vo = new BookWithRatingVO();
            vo.setBookId(book.getBookId());
            vo.setTitle(book.getTitle());
            vo.setAuthor(book.getAuthor());
            vo.setGenre(book.getGenre());
            vo.setDescription(book.getDescription());
            vo.setPath(book.getPath());
            vo.setQuantity(book.getQuantity());
            vo.setHeldQuantity(book.getHeldQuantity());
            vo.setAvailable(book.getAvailable());
            vo.setAvgRating(r.getAvgRating());
            res.add(vo);
        }
        return res;
    }
}
