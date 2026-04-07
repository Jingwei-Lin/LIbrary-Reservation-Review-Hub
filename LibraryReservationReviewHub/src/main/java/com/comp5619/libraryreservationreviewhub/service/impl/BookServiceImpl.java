package com.comp5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.model.dto.book.BookAddRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final UserService userService;
    private final BookMapper bookMapper;

    /* ---------- 分页与排序拼装（带字段白名单） ---------- */
    private record PageCalc(long offset, long limit, String orderBySql) {}

    private PageCalc calc(PageRequest pr, Set<String> sortWhitelist) {
        int current = Math.max(1, pr.getCurrent());
        int size = Math.min(Math.max(1, pr.getPageSize()), 101);
        long offset = (long) (current - 1) * size;

        // 默认按主键倒序
        String orderBySql = "ORDER BY book_id DESC";
        if (StrUtil.isNotBlank(pr.getSortField()) && sortWhitelist.contains(pr.getSortField())) {
            String dir = "descend".equalsIgnoreCase(pr.getSortOrder()) ? "DESC" : "ASC";
            orderBySql = "ORDER BY " + pr.getSortField() + " " + dir;
        }
        return new PageCalc(offset, size, orderBySql);
    }

    /* ===================== 图书 ===================== */

    @Override
    public Long addBook(BookAddRequest req, User operator) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(operator == null || !userService.isAdmin(operator), ErrorCode.NO_AUTH_ERROR);

        String title = StrUtil.trim(req.getTitle());
        String author = StrUtil.trim(req.getAuthor());
        String genre = StrUtil.trim(req.getGenre());

        ThrowUtils.throwIf(StrUtil.hasBlank(title, author, genre), ErrorCode.PARAMS_ERROR, "Book information cannot be left blank.");

        long cnt = bookMapper.countByTag(title, author, genre);
        ThrowUtils.throwIf(cnt > 0, ErrorCode.PARAMS_ERROR, "The book already exists.");

        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setGenre(genre);

        b.setDescription(StrUtil.emptyToNull(StrUtil.trim(req.getDescription())));
        b.setPath(StrUtil.emptyToNull(StrUtil.trim(req.getPath())));

        b.setQuantity(req.getQuantity() == null ? 0 : req.getQuantity());
        b.setAvailable(req.getAvailable() == null ? Boolean.TRUE : req.getAvailable());

        int rows = bookMapper.insertBook(b);
        ThrowUtils.throwIf(rows <= 0 || b.getBookId() == null, ErrorCode.OPERATION_ERROR);
        return b.getBookId().longValue();
    }

    @Override
    public boolean deleteBook(Long id, User operator) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(operator == null || !userService.isAdmin(operator), ErrorCode.NO_AUTH_ERROR);

        Book exist = bookMapper.selectById((long) id);
        ThrowUtils.throwIf(exist == null, ErrorCode.NOT_FOUND_ERROR);

        return bookMapper.deleteById((long) id) > 0;
    }

    @Override
    public boolean updateBook(Book book, User operator) {
        ThrowUtils.throwIf(book == null || book.getBookId() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(operator == null || !userService.isAdmin(operator), ErrorCode.NO_AUTH_ERROR);

        Book old = bookMapper.selectById(book.getBookId());
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR);

        return bookMapper.updateBook(book) > 0;
    }

    @Override
    public Book getBookById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Book b = bookMapper.selectById((Long) id);
        ThrowUtils.throwIf(b == null, ErrorCode.NOT_FOUND_ERROR);
        return b;
    }

    @Override
    public BookVO getBookVO(Book book) {
        if (book == null) return null;
        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);
        vo.setBookId(book.getBookId());
        vo.setTitle(book.getTitle());
        vo.setAuthor(book.getAuthor());
        vo.setGenre(book.getGenre());
        vo.setQuantity(book.getQuantity());
        vo.setHeldQuantity(book.getHeldQuantity());
        vo.setAvailable(book.getAvailable());
        vo.setPath(book.getPath());
        vo.setDescription(book.getDescription());
        return vo;
    }

    @Override
    public List<BookVO> getBookVOList(List<Book> books) {
        if (CollUtil.isEmpty(books)) return CollUtil.newArrayList();
        return books.stream().map(this::getBookVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<BookVO> searchBooks(String keyword, PageRequest pageReq, Boolean available) {
        Set<String> whitelist = Set.of("book_id", "title", "author", "genre", "quantity", "available" , "description");
        PageCalc p = calc(pageReq, whitelist);

        String kw = StrUtil.emptyToDefault(keyword, "");
        long total = bookMapper.countByKeyword(kw, available);
        List<Book> list = total == 0
                ? List.of()
                : bookMapper.searchByKeywordWithOrder(kw, available, p.offset(), p.limit(), p.orderBySql());

        return new PageResult<>(pageReq.getCurrent(), pageReq.getPageSize(), total, getBookVOList(list));
    }

    @Override
    public List<BookVO> getBooksByGenre(String genre) {
        ThrowUtils.throwIf(StrUtil.isBlank(genre), ErrorCode.PARAMS_ERROR, "genre can not be null");
        List<Book> list = bookMapper.selectByGenre(genre.trim());
        return getBookVOList(list);
    }

    @Override
    public boolean isBookAvailable(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Book b = bookMapper.selectById(id.longValue());
        return b != null && Boolean.TRUE.equals(b.getAvailable())
                && b.getQuantity() != null && b.getQuantity() > 0;
    }

    @Override
    public boolean incrementHeldQuantity(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        int rows = bookMapper.incrementHeldQuantity(id);
        return rows > 0;
    }

    @Override
    public boolean decrementHeldQuantity(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        int rows = bookMapper.decrementHeldQuantity(id);
        return rows > 0;
    }

    @Override
    public boolean restoreBook(Long id, User operator) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(operator == null || !userService.isAdmin(operator), ErrorCode.NO_AUTH_ERROR);

        Book exist = bookMapper.selectById((long) id);
        ThrowUtils.throwIf(exist == null, ErrorCode.NOT_FOUND_ERROR);

        // 已经是可用状态则视为幂等成功
        if (Boolean.TRUE.equals(exist.getAvailable())) {
            return true;
        }
        return bookMapper.restoreById((long) id) > 0;
    }

    @Override
    public List<BookVO> getRandomBooks(int limit) {
        // 默认取 10 本
        int realLimit = Math.min(Math.max(limit, 1), 20);
        log.info("Fetching {} random books as top books (no score field, random selection)", realLimit);

        // 先查出部分书籍（防止全表取出性能太差）
        PageRequest pr = new PageRequest();
        pr.setCurrent(1);
        pr.setPageSize(100); // 取前100本进行随机
        PageResult<BookVO> page = searchBooks("", pr, true);

        List<BookVO> list = page.getRecords();
        if (list.isEmpty()) return List.of();

        Collections.shuffle(list);
        return list.stream().limit(realLimit).collect(Collectors.toList());
    }


}