import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookService } from '../services/bookService';
import { reviewService } from '../services/reviewService';
import { adminUserService } from '../services/adminUserService';
import { reservationService } from '../services/reservationService';
import { waitingListService } from '../services/waitingListService';
import './AdminDashboard.css';
import NumericStepper from './NumericStepper';

/**
 * 组件入参
 * - user: 当前登录用户信息（含 isAdmin / userRole）
 * - isAuthenticated: 是否已登录
 * - setIsAuthenticated: 可用于更新登录态（本组件暂未使用）
 */
const AdminDashboard = ({ user, isAuthenticated, setIsAuthenticated }) => {
  const navigate = useNavigate();

  // helper: case-insensitive substring match
  const includesQuery = (value, q) => String(value ?? '').toLowerCase().includes(q);

  /**
 * 进入页面时校验管理员权限
 * 非管理员或未登录，统一跳转到后台登录页
 */
useEffect(() => {
    const isAdmin = user?.isAdmin === true || user?.userRole === 'admin';
    if (!isAuthenticated || !isAdmin) {
      navigate('/admin/login');
    }
  }, [user, isAuthenticated, navigate]);

  // ====== Users ======
  // 用户管理：分页查询、状态标记（封禁/正常）、内联编辑保存
  const [users, setUsers] = useState([]);
  const [usersPage, setUsersPage] = useState(1);
  const USERS_PAGE_SIZE = 10;
  const [usersTotal, setUsersTotal] = useState(0);
  const [usersLoading, setUsersLoading] = useState(false);
  const [usersError, setUsersError] = useState('');
  const [usersSuccess, setUsersSuccess] = useState('');
  const [usersOriginal, setUsersOriginal] = useState([]);
  const [usersQuery, setUsersQuery] = useState('');
  const filteredUsers = React.useMemo(() => {
    const q = usersQuery.trim().toLowerCase();
    if (!q) return users;
    return users.filter(u => (
      includesQuery(u.id, q) ||
      includesQuery(u.userAccount, q) ||
      includesQuery(u.firstName, q) ||
      includesQuery(u.lastName, q) ||
      includesQuery(u.userRole, q)
    ));
  }, [users, usersQuery]);

  /**
   * 根据用户ID补充用户状态（status：0 正常 / 1 封禁）
   * 并保持一份原始数据 usersOriginal 以便比较是否被修改
   */
  const enrichUsersWithStatus = async (list) => {
    if (!Array.isArray(list) || list.length === 0) {
      setUsers(list || []);
      setUsersOriginal(list || []);
      return;
    }
    try {
      const enriched = await Promise.all(list.map(async (u) => {
        try {
          const res = await adminUserService.getUserById(u.id);
          if (res && res.code === 0 && res.data) {
            return { ...u, status: res.data.status };
          }
        } catch (_) { /* no-op */ }
        return { ...u };
      }));
      setUsers(enriched);
      setUsersOriginal(enriched);
    } catch (e) {
      setUsers(list || []);
      setUsersOriginal(list || []);
    }
  };

  /**
   * 分页拉取用户列表，并调用 enrichUsersWithStatus 补充状态
   * 同步维护加载/错误/成功提示
   */
  const fetchUsers = async () => {
    try {
      setUsersLoading(true); setUsersError(''); setUsersSuccess('');
      const res = await adminUserService.listUsersPage({ current: usersPage, pageSize: USERS_PAGE_SIZE });
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        setUsersTotal(res.data.total ?? 0);
        await enrichUsersWithStatus(records);
      } else {
        setUsersError(res?.msg || 'Failed to load users');
        setUsers([]); setUsersOriginal([]); setUsersTotal(0);
      }
    } catch (err) {
      setUsersError(err.message || 'Failed to load users');
      setUsersOriginal([]); setUsersTotal(0);
    } finally {
      setUsersLoading(false);
    }
  };

  /**
   * 本地修改某个用户的一列字段（不直接提交后端）
   */
  const handleUserFieldChange = (id, field, value) => {
    setUsers(prev => {
      const copy = [...prev];
      const idx = copy.findIndex(it => it.id === id);
      if (idx >= 0) {
        copy[idx] = { ...copy[idx], [field]: value };
      }
      return copy;
    });
  };

  /**
   * 比较当前用户与原始数据，判断是否有改动（避免无意义保存）
   */
  const isUserUnchanged = (u) => {
    const orig = usersOriginal.find(o => o.id === u.id);
    if (!orig) return false;
    const role = String(u.userRole || '').toLowerCase();
    const origRole = String(orig.userRole || '').toLowerCase();
    return (
      (u.userAccount || '') === (orig.userAccount || '') &&
      (u.firstName || '') === (orig.firstName || '') &&
      (u.lastName || '') === (orig.lastName || '') &&
      role === origRole
    );
  };

  /**
   * 保存用户编辑：含二次确认、角色处理（admin）、错误/成功提示与数据刷新
   */
  const handleUserUpdate = async (u) => {
    if (isUserUnchanged(u)) {
      alert('No changes made');
      return;
    }
    const confirmed = window.confirm('Are you sure you want to save the changes?');
    if (!confirmed) {
      await fetchUsers();
      setUsersSuccess('');
      setUsersError('');
      return;
    }
    try {
      setUsersError(''); setUsersSuccess('');
      const payload = {
        id: u.id,
        firstName: u.firstName,
        lastName: u.lastName,
        email: u.userAccount,
      };
      if (typeof u.userRole === 'string') {
        const role = u.userRole.toLowerCase();
        payload.isAdmin = role === 'admin';
      }
      const res = await adminUserService.updateUser(payload);
      if (res && res.code === 0) {
        setUsersSuccess('User updated successfully');
        await fetchUsers();
        setTimeout(() => setUsersSuccess(''), 2000);
      } else {
        setUsersError(res?.msg || 'Update failed');
      }
    } catch (err) {
      setUsersError(err.message || 'Update failed');
    }
  };

  /**
   * 切换封禁状态：status 0→1 封禁 / 1→0 解禁，二次确认与刷新
   */
  const handleToggleBan = async (u) => {
    const nextStatus = u.status === 0 ? 1 : 0;
    const msg = nextStatus === 0 ? 'Are you sure you want to ban this user?' : 'Are you sure you want to unban this user?';
    const confirmed = window.confirm(msg);
    if (!confirmed) {
      await fetchUsers();
      setUsersSuccess('');
      setUsersError('');
      return;
    }
    try {
      setUsersError(''); setUsersSuccess('');
      const payload = {
        id: u.id,
        firstName: u.firstName,
        lastName: u.lastName,
        email: u.userAccount,
        status: nextStatus,
      };
      if (typeof u.userRole === 'string') {
        payload.isAdmin = u.userRole.toLowerCase() === 'admin';
      }
      const res = await adminUserService.updateUser(payload);
      if (res && res.code === 0) {
        setUsersSuccess(nextStatus === 0 ? 'User has been banned' : 'User unbanned');
        await fetchUsers();
        setTimeout(() => setUsersSuccess(''), 2000);
      } else {
        setUsersError(res?.msg || 'Failed to update user status');
      }
    } catch (err) {
      setUsersError(err.message || 'Failed to update user status');
    }
  };

  // Independent effect: load user pagination
  useEffect(() => {
    if (isAuthenticated && (user?.isAdmin || user?.userRole === 'admin')) {
      fetchUsers();
    }
  }, [usersPage]);

  // ====== Books ======
  // 图书管理：分页查询、内联编辑保存、删除与恢复
  const [books, setBooks] = useState([]);
  const [booksPage, setBooksPage] = useState(1);
  const BOOKS_PAGE_SIZE = 10;
  const [booksTotal, setBooksTotal] = useState(0);
  const [booksLoading, setBooksLoading] = useState(false);
  const [booksError, setBooksError] = useState('');
  const [booksSuccess, setBooksSuccess] = useState('');
  const [booksOriginal, setBooksOriginal] = useState([]);
  const [booksQuery, setBooksQuery] = useState('');
  const filteredBooks = React.useMemo(() => {
    const q = booksQuery.trim().toLowerCase();
    if (!q) return books;
    return books.filter(b => (
      includesQuery(b.bookId, q) ||
      includesQuery(b.title, q) ||
      includesQuery(b.author, q) ||
      includesQuery(b.genre, q) ||
      includesQuery(b.quantity, q) ||
      includesQuery(b.available, q)
    ));
  }, [books, booksQuery]);

  /**
   * 分页拉取图书列表，并维护加载/错误/成功提示
   */
  const fetchBooks = async () => {
    try {
      setBooksLoading(true); setBooksError(''); setBooksSuccess('');
      const res = await bookService.getAllBooks(booksPage, BOOKS_PAGE_SIZE);
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        setBooks(records);
        setBooksOriginal(records);
        setBooksTotal(res.data.total ?? 0);
      } else {
        setBooksError(res?.msg || 'Failed to load books');
        setBooks([]); setBooksOriginal([]); setBooksTotal(0);
      }
    } catch (err) {
      setBooksError(err.message || 'Failed to load books'); setBooksOriginal([]); setBooksTotal(0);
    } finally {
      setBooksLoading(false);
    }
  };

  /**
   * 本地修改图书的某个字段（不直接提交后端）
   */
  const handleBookFieldChange = (bookId, field, value) => {
    setBooks(prev => {
      const copy = [...prev];
      const idx = copy.findIndex(it => it.bookId === bookId);
      if (idx >= 0) {
        const updated = { ...copy[idx], [field]: value };
        if (field === 'quantity') {
          const q = Number(value);
          const safeQ = Number.isFinite(q) ? q : 0;
          updated.available = safeQ > 0;
        }
        copy[idx] = updated;
      }
      return copy;
    });
  };

  const isBookUnchanged = (b) => {
    const orig = booksOriginal.find(o => o.bookId === b.bookId);
    if (!orig) return false;
    return (
      (b.title || '') === (orig.title || '') &&
      (b.author || '') === (orig.author || '') &&
      (b.genre || '') === (orig.genre || '') &&
      Number(b.quantity ?? 0) === Number(orig.quantity ?? 0) &&
      Boolean(b.available) === Boolean(orig.available)
    );
  };

  /**
   * 保存图书编辑（内联保存），成功后刷新列表
   */
  const handleBookUpdate = async (b) => {
    if (isBookUnchanged(b)) {
      alert('No changes made');
      return;
    }
    const confirmed = window.confirm('Are you sure you want to save the changes?');
    if (!confirmed) {
      await fetchBooks();
      setBooksSuccess('');
      setBooksError('');
      return;
    }
    try {
      setBooksError(''); setBooksSuccess('');
      const res = await bookService.updateBook(b.bookId, { ...b, bookId: b.bookId });
      if (res && res.code === 0) {
        setBooksSuccess('Book updated successfully');
        await fetchBooks();
        setTimeout(() => setBooksSuccess(''), 2000);
      } else {
        setBooksError(res?.msg || 'Update failed');
      }
    } catch (err) {
      setBooksError(err.message || 'Update failed');
    }
  };

  /**
   * 删除图书（软删除），成功后刷新列表
   */
  const handleBookDelete = async (bookId) => {
    try {
      setBooksError(''); setBooksSuccess('');
      const res = await bookService.deleteBook(bookId);
      if (res && res.code === 0) {
        setBooksSuccess('Book deleted successfully');
        await fetchBooks();
        setTimeout(() => setBooksSuccess(''), 2000);
      } else {
        setBooksError(res?.msg || 'Delete failed');
      }
    } catch (err) {
      setBooksError(err.message || 'Delete failed');
    }
  };

  /**
   * 恢复已删除图书，成功后刷新列表
   */
  const handleBookRestore = async (bookId) => {
    try {
      setBooksError(''); setBooksSuccess('');
      const res = await bookService.restoreBook(bookId);
      if (res && res.code === 0) {
        setBooksSuccess('Book restored successfully');
        await fetchBooks();
        setTimeout(() => setBooksSuccess(''), 2000);
      } else {
        setBooksError(res?.msg || 'Restore failed');
      }
    } catch (err) {
      setBooksError(err.message || 'Restore failed');
    }
  };


  // ====== Reviews ======
  // 评论管理：分页查询、内联编辑评分与评论、保存与删除
  const [reviews, setReviews] = useState([]);
  const [reviewsOriginal, setReviewsOriginal] = useState([]);
  const [rvLoading, setRvLoading] = useState(false);
  const [rvError, setRvError] = useState('');
  const [rvSuccess, setRvSuccess] = useState('');
  const [rvPage, setRvPage] = useState(1);
  const RV_PAGE_SIZE = 10;
  const [rvTotal, setRvTotal] = useState(0);
  const [reviewsQuery, setReviewsQuery] = useState('');
  const filteredReviews = React.useMemo(() => {
    const q = reviewsQuery.trim().toLowerCase();
    if (!q) return reviews;
    return reviews.filter(r => (
      includesQuery(r.reviewId, q) ||
      includesQuery(r.bookTitle ?? r.bookId, q) ||
      includesQuery(r.userName ?? r.userId, q) ||
      includesQuery(r.rating, q) ||
      includesQuery(r.comment, q) ||
      includesQuery(r.reviewDate, q)
    ));
  }, [reviews, reviewsQuery]);

  /**
   * 为评论记录批量补充书名与用户名，避免逐条请求造成性能问题
   */
  const enrichReviewsWithNames = async (list) => {
    if (!Array.isArray(list) || list.length === 0) return list || [];
    const bookIds = Array.from(new Set(list.map(r => r.bookId).filter(Boolean)));
    const userIds = Array.from(new Set(list.map(r => r.userId).filter(Boolean)));
    const bookMap = new Map();
    const userMap = new Map();

    await Promise.all([
      Promise.all(bookIds.map(async (id) => {
        try {
          const resp = await bookService.getBookById(id);
          if (resp && resp.code === 0 && resp.data) {
            bookMap.set(id, resp.data.title);
          }
        } catch (_) { /* no-op */ }
      })),
      Promise.all(userIds.map(async (id) => {
        try {
          const res = await adminUserService.getUserById(id);
          if (res && res.code === 0 && res.data) {
            const fn = res.data.firstName || '';
            const ln = res.data.lastName || '';
            const name = `${fn}${fn && ln ? ' ' : ''}${ln}`.trim() || res.data.userAccount || String(id);
            userMap.set(id, name);
          }
        } catch (_) { /* no-op */ }
      }))
    ]);

    return list.map(r => ({
      ...r,
      bookTitle: r.bookTitle || bookMap.get(r.bookId),
      userName: r.userName || userMap.get(r.userId),
    }));
  };

  /**
   * 分页拉取评论列表，保存原始数据用于修改比较
   */
  const fetchAllReviews = async () => {
    try {
      setRvLoading(true); setRvError(''); setRvSuccess('');
      const res = await reviewService.getAllReviews({ current: rvPage, pageSize: RV_PAGE_SIZE });
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        setRvTotal(res.data.total ?? 0);
        const enriched = await enrichReviewsWithNames(records);
        setReviews(enriched);
        setReviewsOriginal(enriched);
      } else {
        setRvError(res?.msg || 'Failed to load reviews');
        setReviews([]); setReviewsOriginal([]); setRvTotal(0);
      }
    } catch (err) {
      setRvError(err.message || 'Failed to load reviews');
      setReviews([]); setReviewsOriginal([]); setRvTotal(0);
    } finally {
      setRvLoading(false);
    }
  };

  /**
   * 本地修改评论的某个字段（如评分/评论内容）
   */
  const handleReviewFieldChange = (reviewId, field, value) => {
    setReviews(prev => {
      const copy = [...prev];
      const idx = copy.findIndex(it => it.reviewId === reviewId);
      if (idx >= 0) {
        copy[idx] = { ...copy[idx], [field]: value };
      }
      return copy;
    });
  };

  /**
   * 比较当前评论与原始记录，判断是否有改动
   */
  const isReviewUnchanged = (rv) => {
    const orig = reviewsOriginal.find(o => o.reviewId === rv.reviewId);
    if (!orig) return false;
    const currRating = Number(rv.rating);
    const origRating = Number(orig.rating);
    const currComment = (rv.comment || '').trim();
    const origComment = (orig.comment || '').trim();
    return currRating === origRating && currComment === origComment;
  };

  /**
   * 保存评论编辑：二次确认、评分范围校验（1-5）、非空校验、成功后刷新
   */
  const handleReviewUpdate = async (rv) => {
    if (isReviewUnchanged(rv)) {
      alert('No changes made');
      return;
    }
    const confirmed = window.confirm('Are you sure you want to save the changes?');
    if (!confirmed) {
      await fetchAllReviews();
      setRvSuccess('');
      setRvError('');
      return;
    }

    const ratingNum = Number(rv.rating);
    if (!Number.isFinite(ratingNum) || ratingNum < 1 || ratingNum > 5) {
      setRvError('Rating must be between 1 and 5');
      return;
    }
    const commentTrim = (rv.comment || '').trim();
    if (!commentTrim) {
      setRvError('Comment cannot be empty');
      return;
    }

    try {
      setRvError(''); setRvSuccess('');
      const ok = await reviewService.updateReview({ reviewId: rv.reviewId, rating: ratingNum, comment: commentTrim });
      if (ok) {
        setRvSuccess('Review updated successfully');
        await fetchAllReviews();
        setTimeout(() => setRvSuccess(''), 2000);
      } else {
        setRvError('Update failed');
      }
    } catch (err) {
      setRvError(err.message || 'Update failed');
    }
  };

  /**
   * 删除评论：二次确认，成功后刷新
   */
  const handleReviewDelete = async (reviewId) => {
    const confirmed = window.confirm('Are you sure you want to delete this review?');
    if (!confirmed) return;
    try {
      setRvError(''); setRvSuccess('');
      const ok = await reviewService.deleteReview(reviewId);
      if (ok) {
        setRvSuccess('Review deleted successfully');
        await fetchAllReviews();
        setTimeout(() => setRvSuccess(''), 2000);
      } else {
        setRvError('Delete failed');
      }
    } catch (err) {
      setRvError(err.message || 'Delete failed');
    }
  };

  useEffect(() => {
    if (isAuthenticated && (user?.isAdmin || user?.userRole === 'admin')) {
      fetchAllReviews();
    }
  }, [rvPage]); // 仅在评论分页变化时触发

  // ====== Waiting List ======
  // 等待列表：仅展示（按所有用户汇总），不提供任何操作
  const [waitingLists, setWaitingLists] = useState([]);
  const [wlLoading, setWlLoading] = useState(false);
  const [wlError, setWlError] = useState('');
  const [wlPage, setWlPage] = useState(1);
  const WL_PAGE_SIZE = 10;
  const [wlQuery, setWlQuery] = useState('');
  const filteredWaitingLists = React.useMemo(() => {
    const q = wlQuery.trim().toLowerCase();
    if (!q) return waitingLists;
    return waitingLists.filter(w => (
      includesQuery(w.waitId, q) ||
      includesQuery(w.userName ?? w.userId, q) ||
      includesQuery(w.bookTitle ?? w.bookId, q) ||
      includesQuery(w.status, q) ||
      includesQuery(w.joinDate, q) ||
      includesQuery(w.notificationDate, q)
    ));
  }, [waitingLists, wlQuery]);

  // 拉取所有用户（跨页）
  const fetchAllUsers = async () => {
    const PAGE_SIZE = 100; // 提高每页数量，减少请求次数
    const first = await adminUserService.listUsersPage({ current: 1, pageSize: PAGE_SIZE });
    if (!(first && first.code === 0 && first.data)) {
      throw new Error(first?.msg || 'Failed to load users');
    }
    const total = Number(first.data.total ?? 0);
    const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));
    const all = [...(first.data.records || [])];
    for (let p = 2; p <= pages; p++) {
      try {
        const res = await adminUserService.listUsersPage({ current: p, pageSize: PAGE_SIZE });
        if (res && res.code === 0 && res.data) {
          all.push(...(res.data.records || []));
        }
      } catch (_) { /* no-op */ }
    }
    return all;
  };

  const fetchWaitingListsAll = async () => {
    try {
      setWlLoading(true); setWlError('');
      const usersAll = await fetchAllUsers();
      const rowsByUser = await Promise.all(usersAll.map(async (u) => {
        const displayName = `${u.firstName || ''}${u.firstName && u.lastName ? ' ' : ''}${u.lastName || ''}`.trim() || u.userAccount || String(u.id);
        try {
          const resp = await waitingListService.getByUserAdmin(u.id);
          if (resp && resp.code === 0 && Array.isArray(resp.data)) {
            return resp.data.map(item => ({
              ...item,
              userId: item.userId ?? u.id,
              userName: item.userName || displayName,
              bookTitle: item.bookTitle || item.bookId,
            }));
          }
        } catch (_) { /* no-op per user */ }
        return [];
      }));
      const rows = rowsByUser.flat();
      // 排序：先按用户ID，再按等待ID倒序
      rows.sort((a, b) => {
        const ua = Number(a.userId ?? 0); const ub = Number(b.userId ?? 0);
        if (ua !== ub) return ua - ub;
        return Number(b.waitId ?? 0) - Number(a.waitId ?? 0);
      });
      setWaitingLists(rows);
      setWlPage(1);
    } catch (err) {
      setWlError(err.message || 'Failed to load waiting lists');
      setWaitingLists([]);
    } finally {
      setWlLoading(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated && (user?.isAdmin || user?.userRole === 'admin')) {
      fetchWaitingListsAll();
    }
  }, []);

  // ====== Reservations (Borrowing Management) ======
  const [reservations, setReservations] = useState([]);
  const [resLoading, setResLoading] = useState(false);
  const [resError, setResError] = useState('');
  const [resSuccess, setResSuccess] = useState('');
  const [resPage, setResPage] = useState(1);
  const RES_PAGE_SIZE = 5; // UI page size per requirement
  const [resFilter, setResFilter] = useState('all'); // borrowed | active | all
  const [resQuery, setResQuery] = useState('');
  const filteredReservations = React.useMemo(() => {
    const q = resQuery.trim().toLowerCase();
    if (!q) return reservations;
    return reservations.filter(r => (
      includesQuery(r.reservationId, q) ||
      includesQuery(r.userId, q) ||
      includesQuery(r.bookTitle ?? r.bookId, q) ||
      includesQuery(r.reservationStatus, q) ||
      includesQuery(r.pickupTime ? new Date(r.pickupTime).toLocaleDateString() : '', q) ||
      includesQuery(r.dueTime ? new Date(r.dueTime).toLocaleDateString() : '', q)
    ));
  }, [reservations, resQuery]);

  useEffect(() => {
    const maxPage = Math.max(1, Math.ceil(filteredReservations.length / RES_PAGE_SIZE));
    if (resPage > maxPage) {
      setResPage(maxPage);
    }
  }, [filteredReservations.length]);

  const filterReservations = (records, filter) => {
    switch (filter) {
      case 'borrowed':
        return records.filter(r => ['borrowed', 'overdue'].includes(r.reservationStatus));
      case 'active':
        return records.filter(r => ['reserved', 'borrowed', 'overdue'].includes(r.reservationStatus));
      case 'all':
        return records;
      default:
        return records;
    }
  };

  const fetchReservations = async () => {
    try {
      setResLoading(true); setResError(''); setResSuccess('');
      // Fetch a large page then paginate client-side
      const res = await reservationService.getAllReservations(1, 1000);
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        const filtered = filterReservations(records, resFilter);
        setReservations(filtered);
        setResPage(1);
      } else {
        setResError(res?.message || 'Failed to load reservations');
        setReservations([]);
      }
    } catch (err) {
      setResError(err.message || 'Failed to load reservations');
      setReservations([]);
    } finally {
      setResLoading(false);
    }
  };

  const isDueToday = (dueTime) => {
    if (!dueTime) return false;
    const d = new Date(dueTime);
    const now = new Date();
    return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate();
  };

  const isOverdue = (dueTime, status) => {
    if (!dueTime) return false;
    const d = new Date(dueTime);
    return d.getTime() < Date.now() && status !== 'returned' && status !== 'cancelled';
  };

  const handleConfirmBorrow = async (reservationId) => {
    const confirmed = window.confirm("Confirm marking this reservation as borrowed?");
    if (!confirmed) return;
    try {
      setResError("");
      setResSuccess("");
      const resp = await reservationService.updateReservationStatus(reservationId, "borrowed");
      if (resp && resp.code === 0 && resp.data === true) {
        setResSuccess("Borrow confirmed successfully");
        await fetchReservations();
        setTimeout(() => setResSuccess(""), 2000);
      } else {
        setResError(resp?.message || "Failed to confirm borrow");
      }
    } catch (err) {
      setResError(err.message || "Failed to confirm borrow");
    }
  };

  const handleConfirmReturn = async (reservationId) => {
    const confirmed = window.confirm("Confirm marking this reservation as returned?");
    if (!confirmed) return;
    try {
      setResError("");
      setResSuccess("");
      const resp = await reservationService.updateReservationStatus(reservationId, "returned");
      if (resp && resp.code === 0 && resp.data === true) {
        setResSuccess("Return confirmed successfully");
        await fetchReservations();
        setTimeout(() => setResSuccess(""), 2000);
      } else {
        setResError(resp?.message || "Failed to confirm return");
      }
    } catch (err) {
      setResError(err.message || "Failed to confirm return");
    }
  };

  useEffect(() => {
    if (isAuthenticated && (user?.isAdmin || user?.userRole === 'admin')) {
      fetchReservations();
    }
  }, [resFilter]);

  useEffect(() => {
    if (isAuthenticated && (user?.isAdmin || user?.userRole === 'admin')) {
      fetchBooks();
    }
  }, [booksPage]);

  // ====== UI Rendering ======
  return (
    <div className="admin-container">
      <h2 className="page-title">Admin Panel</h2>

      {/* 用户管理区块 */}
      <div className="admin-section">
        <h3>Users</h3>
        <div className="admin-controls">
          <input
            className="input input-sm"
            placeholder="Search users..."
            value={usersQuery}
            onChange={(e) => setUsersQuery(e.target.value)}
          />
          <span>Page size: {USERS_PAGE_SIZE}</span>
          <span>Page: {usersPage} / {Math.max(1, Math.ceil((usersTotal || 0) / USERS_PAGE_SIZE))}</span>
          <button className="btn btn-primary" onClick={fetchUsers}>Refresh</button>
        </div>
        {usersLoading && <div className="loading">Loading users...</div>}
        {usersError && <div className="alert alert-error">{usersError}</div>}
        {usersSuccess && <div className="alert alert-success">{usersSuccess}</div>}
        <table className="admin-table users-table">
          <thead>
            <tr>
              <th></th><th>ID</th><th>Account</th><th>First Name</th><th>Last Name</th><th>Role</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', color: '#666' }}>No matched results</td>
              </tr>
            ) : filteredUsers.map((u, idx) => (
              <tr key={u.id || idx}>
                <td>{u.id}</td>
                <td><input className="input" value={u.userAccount || ''} onChange={(e) => handleUserFieldChange(u.id, 'userAccount', e.target.value)} /></td>
                <td><input className="input" value={u.firstName || ''} onChange={(e) => handleUserFieldChange(u.id, 'firstName', e.target.value)} /></td>
                <td><input className="input" value={u.lastName || ''} onChange={(e) => handleUserFieldChange(u.id, 'lastName', e.target.value)} /></td>
                <td><input className="input" value={u.userRole || ''} onChange={(e) => handleUserFieldChange(u.id, 'userRole', e.target.value)} /></td>
                <td className="actions">
                  <button className="btn btn-primary" onClick={() => handleUserUpdate(u)}>Save</button>
                  <button className="btn btn-outline" onClick={() => handleToggleBan(u)}>
                    {u.status === 0 ? 'Unban' : 'Ban'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="admin-pagination-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
          <button
            className="btn btn-secondary"
            onClick={() => setUsersPage(Math.max(1, usersPage - 1))}
            disabled={usersPage <= 1}
          >Prev</button>
          <button
            className="btn btn-secondary"
            onClick={() => setUsersPage(usersPage + 1)}
            disabled={usersPage >= Math.max(1, Math.ceil((usersTotal || 0) / USERS_PAGE_SIZE))}
          >Next</button>
        </div>
      </div>

      {/* 评论管理区块 */}
      <div className="admin-section">
        <h3>Reviews</h3>
        <div className="admin-controls">
          <input
            className="input input-sm"
            placeholder="Search reviews..."
            value={reviewsQuery}
            onChange={(e) => setReviewsQuery(e.target.value)}
          />
          <span>Page size: {RV_PAGE_SIZE}</span>
          <span>Page: {rvPage} / {Math.max(1, Math.ceil((rvTotal || 0) / RV_PAGE_SIZE))}</span>
          <button className="btn btn-primary" onClick={fetchAllReviews}>Refresh</button>
        </div>
        {rvLoading && <div className="loading">Loading reviews...</div>}
        {rvError && <div className="alert alert-error">{rvError}</div>}
        {rvSuccess && <div className="alert alert-success">{rvSuccess}</div>}
        <table className="admin-table reviews-table">
          <thead>
            <tr>
              <th></th><th>ID</th><th>Book</th><th>User</th><th>Rating</th><th>Comment</th><th>Date</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredReviews.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', color: '#666' }}>No matched results</td>
              </tr>
            ) : filteredReviews.map((r, idx) => (
              <tr key={r.reviewId || idx}>
                <td>{r.reviewId}</td>
                <td>{r.bookTitle || r.bookId}</td>
                <td>{r.userName || r.userId}</td>
                <td><NumericStepper min={1} max={5} step={1} value={r.rating ?? 1} onChange={(val) => handleReviewFieldChange(r.reviewId, 'rating', val)} /></td>
                <td><input className="input" value={r.comment || ''} onChange={(e) => handleReviewFieldChange(r.reviewId, 'comment', e.target.value)} /></td>
                <td>{r.reviewDate}</td>
                <td className="actions">
                  <button className="btn btn-primary" onClick={() => handleReviewUpdate(r)}>Save</button>
                  <button className="btn btn-danger" onClick={() => handleReviewDelete(r.reviewId)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="admin-pagination-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
          <button
            className="btn btn-secondary"
            onClick={() => setRvPage(Math.max(1, rvPage - 1))}
            disabled={rvPage <= 1}
          >Prev</button>
          <button
            className="btn btn-secondary"
            onClick={() => setRvPage(rvPage + 1)}
            disabled={rvPage >= Math.max(1, Math.ceil((rvTotal || 0) / RV_PAGE_SIZE))}
          >Next</button>
        </div>
      </div>

      {/* 等待列表展示区块（只读） */}
      <div className="admin-section">
        <h3>Waiting List</h3>
        <div className="admin-controls">
          <input
            className="input input-sm"
            placeholder="Search waiting list..."
            value={wlQuery}
            onChange={(e) => setWlQuery(e.target.value)}
          />
          <span>Page size: {WL_PAGE_SIZE}</span>
          <button className="btn btn-primary" onClick={fetchWaitingListsAll}>Refresh</button>
          <span>Page: {wlPage} / {Math.max(1, Math.ceil(filteredWaitingLists.length / WL_PAGE_SIZE))}</span>
        </div>
        {wlLoading && <div className="loading">Loading waiting lists...</div>}
        {wlError && <div className="alert alert-error">{wlError}</div>}
        <table className="admin-table waitinglist-table">
          <thead>
            <tr>
              <th></th>
              <th></th>
              <th>User</th>
              <th>Book</th>
              <th>Join Date</th>
              <th>Status</th>
              <th>Notified</th>
              <th>Notification Date</th>
            </tr>
          </thead>
          <tbody>
            {(() => {
              const start = (wlPage - 1) * WL_PAGE_SIZE;
              const pageItems = filteredWaitingLists.slice(start, start + WL_PAGE_SIZE);
              if (pageItems.length === 0) {
                return (
                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center', color: '#666' }}>No matched results</td>
                  </tr>
                );
              }
              return pageItems.map((w, idx) => (
                <tr key={w.waitId || `${w.userId}-${w.bookId}-${idx}`}>
                  <td>{w.waitId}</td>
                  <td>{w.userName || w.userId}</td>
                  <td>{w.bookTitle || w.bookId}</td>
                  <td>{w.joinDate || 'N/A'}</td>
                  <td>{String(w.status || '').toLowerCase()}</td>
                  <td>{w.isNotified ? 'Yes' : 'No'}</td>
                  <td>{w.notificationDate || 'N/A'}</td>
                </tr>
              ));
            })()}
          </tbody>
        </table>
        <div className="admin-pagination-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
          <button className="btn btn-secondary" onClick={() => setWlPage(Math.max(1, wlPage - 1))} disabled={wlPage <= 1}>Prev</button>
          <button className="btn btn-secondary" onClick={() => setWlPage(wlPage + 1)} disabled={wlPage >= Math.max(1, Math.ceil(filteredWaitingLists.length / WL_PAGE_SIZE))}>Next</button>
        </div>
      </div>

      {/* 图书管理区块 */}
      <div className="admin-section">
        <h3>Books</h3>
        <div className="admin-controls">
          <input
            className="input input-sm"
            placeholder="Search books..."
            value={booksQuery}
            onChange={(e) => setBooksQuery(e.target.value)}
          />
          <span>Page size: {BOOKS_PAGE_SIZE}</span>
          <span>Page: {booksPage} / {Math.max(1, Math.ceil((booksTotal || 0) / BOOKS_PAGE_SIZE))}</span>
          <button className="btn btn-primary" onClick={fetchBooks}>Refresh</button>
        </div>
        {booksLoading && <div className="loading">Loading books...</div>}
        {booksError && <div className="alert alert-error">{booksError}</div>}
        {booksSuccess && <div className="alert alert-success">{booksSuccess}</div>}
        <table className="admin-table books-table">
          <thead>
            <tr>
              <th></th><th>ID</th><th>Title</th><th>Author</th><th>Genre</th><th>Quantity</th><th>Available</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredBooks.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', color: '#666' }}>No matched results</td>
              </tr>
            ) : filteredBooks.map((b, idx) => (
              <tr key={b.bookId || idx}>
                <td>{b.bookId}</td>
                <td><input className="input" value={b.title || ''} onChange={(e) => handleBookFieldChange(b.bookId, 'title', e.target.value)} /></td>
                <td><input className="input" value={b.author || ''} onChange={(e) => handleBookFieldChange(b.bookId, 'author', e.target.value)} /></td>
                <td><input className="input" value={b.genre || ''} onChange={(e) => handleBookFieldChange(b.bookId, 'genre', e.target.value)} /></td>
                <td><NumericStepper min={0} step={1} value={b.quantity ?? 0} onChange={(val) => handleBookFieldChange(b.bookId, 'quantity', val)} /></td>
                <td><input className="checkbox" type="checkbox" checked={!!b.available} onChange={(e) => handleBookFieldChange(b.bookId, 'available', e.target.checked)} /></td>
                <td className="actions">
                  <button className="btn btn-primary" onClick={() => handleBookUpdate(b)}>Save</button>
                  <button className="btn btn-danger" onClick={() => handleBookDelete(b.bookId)}>Delete</button>
                  <button className="btn btn-outline" onClick={() => handleBookRestore(b.bookId)}>Restore</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="admin-pagination-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
          <button
            className="btn btn-secondary"
            onClick={() => setBooksPage(Math.max(1, booksPage - 1))}
            disabled={booksPage <= 1}
          >Prev</button>
          <button
            className="btn btn-secondary"
            onClick={() => setBooksPage(booksPage + 1)}
            disabled={booksPage >= Math.max(1, Math.ceil((booksTotal || 0) / BOOKS_PAGE_SIZE))}
          >Next</button>
        </div>
      </div>
      {/* 预约管理区块 */}
      <div className="admin-section">
        <h3>Reservations</h3>
        <div className="admin-controls">
          <span>Page size: {RES_PAGE_SIZE}</span>
          <label>Filter:</label>
          <select className="input input-sm" value={resFilter} onChange={(e) => setResFilter(e.target.value)}>
            <option value="all">All statuses</option>
            <option value="borrowed">Borrowed/Overdue only</option>
            <option value="active">Include reserved</option>
          </select>
          <button className="btn btn-primary" onClick={fetchReservations}>Refresh</button>
          <input
            className="input input-sm"
            placeholder="Search reservations..."
            value={resQuery}
            onChange={(e) => setResQuery(e.target.value)}
          />
          <span>Page: {resPage} / {Math.max(1, Math.ceil(filteredReservations.length / RES_PAGE_SIZE))}</span>
        </div>
        {resLoading && <div className="loading">Loading reservations...</div>}
        {resError && <div className="alert alert-error">{resError}</div>}
        {resSuccess && <div className="alert alert-success">{resSuccess}</div>}
        <table className="admin-table reservations-table">
          <thead>
            <tr>
              <th></th>
              <th>User ID</th>
              <th>Book Title</th>
              <th>Borrowed Date</th>
              <th>Due Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {(() => {
              const start = (resPage - 1) * RES_PAGE_SIZE;
              const pageItems = filteredReservations.slice(start, start + RES_PAGE_SIZE);
              if (pageItems.length === 0) {
                return (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', color: '#666' }}>No matched results</td>
                  </tr>
                );
              }
              return pageItems.map((r, idx) => {
                const red = isDueToday(r.dueTime) || isOverdue(r.dueTime, r.reservationStatus);
                return (
                  <tr key={r.reservationId || idx} className={red ? 'row-red' : ''}>
                    <td>{r.userId}</td>
                    <td>{r.bookTitle || r.bookId}</td>
                    <td>{r.pickupTime ? new Date(r.pickupTime).toLocaleDateString() : 'N/A'}</td>
                    <td>{r.dueTime ? new Date(r.dueTime).toLocaleDateString() : 'N/A'}</td>
                    <td className="actions">
                      {['borrowed', 'overdue'].includes(r.reservationStatus) && (
                        <button className="btn btn-primary" onClick={() => handleConfirmReturn(r.reservationId)}>Confirm Return</button>
                      )}
                      {r.reservationStatus === 'reserved' && (
                        <button className="btn btn-outline" onClick={() => handleConfirmBorrow(r.reservationId)}>Confirm Borrow</button>
                      )}
                    </td>
                  </tr>
                );
              });
            })()}
          </tbody>
        </table>
        <div className="admin-pagination-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
          <button className="btn btn-secondary" onClick={() => setResPage(Math.max(1, resPage - 1))} disabled={resPage <= 1}>Prev</button>
          <button className="btn btn-secondary" onClick={() => setResPage(resPage + 1)} disabled={resPage >= Math.max(1, Math.ceil(filteredReservations.length / RES_PAGE_SIZE))}>Next</button>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
