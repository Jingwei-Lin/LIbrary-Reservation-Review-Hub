// Dashboard.jsx
import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { bookService } from "../services/bookService";
import { waitingListService } from "../services/waitingListService";
import { validateSession, logoutUser } from "../services/authApi";
import Header from "./Header";

import ChatPanel from "./ChatPanel";

import Book from './Book';
import { getTheme } from "../themeUtils";

import "./Dashboard.css";

const Dashboard = ({ user, setUser, setIsAuthenticated, isAuthenticated }) => {
    const navigate = useNavigate();
    const location = useLocation();

    // Get initial state from URL parameters
    const urlParams = new URLSearchParams(location.search);
    const initialSearch = urlParams.get('search') || '';
    const initialGenre = urlParams.get('genre') || '';
    const initialView = urlParams.get('view') || '';
    const initialPage = parseInt(urlParams.get('page')) || 1;

    const [books, setBooks] = useState([]);
    const [topBooks, setTopBooks] = useState([]);
    const [currentTitle, setCurrentTitle] = useState("All Books");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [searchTerm, setSearchTerm] = useState("");
    const [currentPage, setCurrentPage] = useState(initialPage);
    const [selectedBookId, setSelectedBookId] = useState(() => {
        const params = new URLSearchParams(location.search);
        const bookIdFromUrl = params.get('book');
        const id = Number(bookIdFromUrl);
        return (!isNaN(id) && id > 0) ? id : null;
    });
    const [initialLoadComplete, setInitialLoadComplete] = useState(false);
    const [theme, setTheme] = useState(getTheme());
    const [hasNotification, setHasNotification] = useState(false);
    const [currentGenre, setCurrentGenre] = useState(initialGenre);
    const [currentView, setCurrentView] = useState(initialView);

    const [totalStats, setTotalStats] = useState({
        totalBooks: 0,
        totalAvailable: 0,
        totalCategories: 0
    });

    const booksPerPage = 12;

    // Update URL when search/filter state changes
    const updateURL = (updates = {}) => {
        const params = new URLSearchParams();

        // Only include book if explicitly provided in updates or if we're on a book page
        if (updates.book !== undefined) {
            // If updates explicitly sets book, use that value
            if (updates.book) {
                params.set('book', updates.book);
            }
            // If updates sets book to null/undefined, don't include it
        } else if (selectedBookId) {
            // If no book in updates but we have selectedBookId, include it
            params.set('book', selectedBookId.toString());
        }

        if (searchTerm) params.set('search', searchTerm);
        if (currentGenre) params.set('genre', currentGenre);
        if (currentView) params.set('view', currentView);

        if (updates.page !== undefined) {
            if (updates.page > 1) {
                params.set('page', updates.page.toString());
            } else {
                params.delete('page');
            }
        } else if (currentPage > 1) {
            params.set('page', currentPage.toString());
        }

        // Apply updates
        Object.keys(updates).forEach(key => {
            if (updates[key]) {
                params.set(key, updates[key]);
            } else {
                params.delete(key);
            }
        });

        navigate(`/dashboard?${params.toString()}`, { replace: true });
    };

    // Initial data fetch
    useEffect(() => {
        const initializeDashboard = async () => {
            console.log(sessionStorage);
            console.log('Initializing dashboard with URL params:', {
                search: initialSearch,
                genre: initialGenre,
                view: initialView,
                book: selectedBookId
            });

            try {
                // If we're on a book page, only load book-specific data
                if (selectedBookId) {
                    console.log('On book page, skipping list initialization');
                }
                // If we're on Top 10 view, load top 10 books directly
                else if (initialView === 'top10') {
                    console.log('Loading top 10 books from URL parameter');
                    await loadTop10Books();
                }
                // If we have a search term, load search results
                else if (initialSearch) {
                    await fetchBooks(initialSearch);
                }
                // If we have a genre filter, load genre books
                else if (initialGenre) {
                    await loadGenreBooks(initialGenre);
                }
                // Otherwise load all books
                else {
                    await fetchBooks();
                }

                // Parallelize non-critical fetches (user, eligibility, waiting list, top books)
                const [eligibilityRes, waitingListRes, userRes, topBooksRes] = await Promise.all([
                    waitingListService.getEligibility().catch(err => ({ code: -1, message: err.message })), // Graceful fail
                    waitingListService.getMyWaitingList().catch(err => ({ code: -1, message: err.message })),
                    validateSession().catch(err => ({ code: -1, message: err.message })),
                    bookService.getTop10Books().catch(err => ({ code: -1, message: err.message })),
                    fetchTotalStats()
                ]);

                // Process responses (handle errors individually to not block everything)
                if (eligibilityRes.code !== 0) {
                    console.warn('Eligibility check failed:', eligibilityRes.message);
                }
                if (waitingListRes.code === 0 && waitingListRes.data) {
                    const hasNotified = waitingListRes.data.some(item => item.status === "notified");
                    setHasNotification(hasNotified);
                } else {
                    console.warn('Waiting list fetch failed:', waitingListRes.message);
                    setHasNotification(false);
                }
                if (userRes.code === 0 && userRes.data) {
                    setUser(userRes.data);
                    setIsAuthenticated(true);
                } else {
                    console.warn('User validation failed:', userRes.message);
                    logoutUser();
                    setIsAuthenticated(false);
                    return; // Early exit if logged out
                }
                if (topBooksRes.code === 0 && topBooksRes.data) {
                    const validBooks = topBooksRes.data.filter(book => {
                        const bookId = Number(book.bookId);
                        return book.bookId != null && !isNaN(bookId) && bookId > 0;
                    });
                    setTopBooks(validBooks);
                } else {
                    console.warn('Top books fetch failed:', topBooksRes.message);
                    setTopBooks([]);
                }
                setInitialLoadComplete(true);
                updateURL();
            } catch (error) {
                console.error('Error during initialization:', error);
                setError('Failed to initialize dashboard. Please try again.');
            } finally {
                setLoading(false); // Always stop loading, success or fail
            }
        };

        initializeDashboard();
    }, []);

    // Apply theme from localStorage
    useEffect(() => {
        document.body.classList.remove("light", "dark");
        document.body.classList.add(theme);
    }, [theme]);

    // Listen for custom themeChange event (in-page sync)
    useEffect(() => {
        const handleThemeChange = (event) => {
            setTheme(event.detail.theme);
        };
        window.addEventListener("themeChange", handleThemeChange);
        return () => {
            window.removeEventListener("themeChange", handleThemeChange);
        };
    }, []);

    // Poll localStorage for theme changes (cross-tab sync)
    useEffect(() => {
        const handleStorageChange = () => {
            setTheme(getTheme());
        };
        window.addEventListener("storage", handleStorageChange);
        return () => {
            window.removeEventListener("storage", handleStorageChange);
        };
    }, []);

    // Revalidate and sync user session when dashboard loads
    const fetchUser = async () => {
        try {
            const response = await validateSession();
            console.log("revalidate:", response.data);
            if (response && response.code === 0 && response.data) {
                setUser(response.data);
                setIsAuthenticated(true);
            } else {
                logoutUser();
                setIsAuthenticated(false);
            }
        } catch (err) {
            logoutUser();
            setIsAuthenticated(false);
            console.warn("Failed to fetchUser");
        }
    };

    // Sync URL with selected book state
    useEffect(() => {
        // if (!initialLoadComplete) return; // Wait for initial data load
        const params = new URLSearchParams(location.search);
        const bookIdFromUrl = params.get('book');

        if (bookIdFromUrl) {
            const id = Number(bookIdFromUrl);
            if (!isNaN(id) && id > 0) {
                setSelectedBookId(id);
            } else {
                updateURL({ book: null });
            }
        } else {
            setSelectedBookId(null);
        }
    }, [location.search, navigate]);


    const fetchBooks = async (keyword = "") => {
        try {
            setLoading(true);
            setError("");

            // if (keyword !== initialSearch) {
            //     setSearchTerm(keyword);
            //     setCurrentGenre('');
            //     setCurrentView('');
            // }

            const response = keyword
                ? await bookService.searchBooks(keyword)
                : await bookService.getAllBooks();

            if (response && response.code === 0 && response.data) {
                const bookList = response.data.records
                    ? response.data.records
                    : Array.isArray(response.data)
                        ? response.data
                        : [];
                const validBooks = bookList.filter(book => {
                    const bookId = Number(book.bookId);
                    const isValid = book.bookId != null && !isNaN(bookId) && bookId > 0;
                    if (!isValid) {
                        console.warn('Invalid book detected:', JSON.stringify(book, null, 2));
                    }
                    return true;
                });
                setBooks(validBooks);
                setCurrentTitle(keyword ? `Search Results for "${keyword}"` : "All Books");
                if (bookList.length > 0 && validBooks.every(book => !book.bookId)) {
                    setError("All books have missing or invalid IDs. Please contact support.");
                }
            } else {
                setError("Invalid response format");
                setBooks([]);
            }
        } catch (err) {
            if (err.message.includes("Session expired or user banned")) {
                logoutUser();
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setError(err.message || "Failed to load books");
                console.error("Error fetching books:", err);
                setBooks([]);
            }
        } finally {
            setLoading(false);
            // if (keyword !== initialSearch) {
            //     setCurrentPage(1);
            //     updateURL({ page: 1 });
            // }
            // setSelectedBookId(null);
        }
    };

    const fetchTotalStats = async () => {
        try {
            const response = await bookService.getAllBooks();
            if (response && response.code === 0 && response.data) {
                const allBooks = response.data.records || response.data;
                const totalBooks = allBooks.length;
                const totalAvailable = allBooks.filter(book => book.available).length;

                // Get unique genres
                const uniqueGenres = new Set(
                    allBooks.map(book => book.genre).filter(genre => genre && genre.trim() !== '')
                );

                setTotalStats({
                    totalBooks: totalBooks,
                    totalAvailable: totalAvailable,
                    totalCategories: uniqueGenres.size
                });
            }
        } catch (err) {
            console.error("Error fetching total statistics:", err);
        }
    };

    const loadTop10Books = async () => {
        try {
            setLoading(true);
            const response = await bookService.getTop10Books();

            if (response && response.code === 0 && response.data) {
                const bookList = Array.isArray(response.data) ? response.data : [];
                const validBooks = bookList.filter(book => {
                    const bookId = Number(book.bookId);
                    const isValid = book.bookId != null && !isNaN(bookId) && bookId > 0;
                    return isValid;
                });

                if (validBooks.length === 0) {
                    setError("No top-rated books available yet.");
                    setBooks([]);
                } else {
                    setBooks(validBooks);
                    setTopBooks(validBooks);
                    setCurrentTitle("Top 10 Rated Books");
                }
            } else {
                setError("No top-rated books available yet.");
                setBooks([]);
            }
        } catch (err) {
            console.error("Error loading top 10 books:", err);
            setError("Failed to load top 10 books");
            setBooks([]);
        } finally {
            setLoading(false);
            // if (initialView !== 'top10') {
            //     setCurrentPage(1);
            //     updateURL({ page: 1 });
            // }
            // setSelectedBookId(null);
        }
    };

    // Helper function to load genre books without setting URL states
    const loadGenreBooks = async (genre) => {
        try {
            setLoading(true);
            const response = await bookService.getBooksByGenre(genre);

            if (response && response.code === 0 && response.data) {
                const bookList = Array.isArray(response.data) ? response.data : response.data.records || [];
                const validBooks = bookList.filter(book => {
                    const bookId = Number(book.bookId);
                    const isValid = book.bookId != null && !isNaN(bookId) && bookId > 0;
                    return isValid;
                });
                setBooks(validBooks);
                setCurrentTitle(`${genre} Books`);
            } else {
                setError("Invalid response format for genre");
                setBooks([]);
            }
        } catch (err) {
            console.error("Error loading genre books:", err);
            setError(`Failed to load ${genre} books: ${err.message}`);
            setBooks([]);
        } finally {
            setLoading(false);
            // if (initialGenre !== genre) {
            //     setCurrentPage(1);
            //     updateURL({ page: 1 });
            // }

            // setSelectedBookId(null);
        }
    };

    const handleSearch = (e) => {
        e.preventDefault();
        setSelectedBookId(null);
        setCurrentGenre('');
        setCurrentView('');
        fetchBooks(searchTerm);
        updateURL({ book: null, search: searchTerm, genre: '', view: '', page: 1 });
        // navigate('/dashboard', { replace: true });
    };

    const handleGenreClick = async (genre) => {
        try {
            setSelectedBookId(null);
            setCurrentGenre(genre);
            setCurrentView('');
            setSearchTerm('');
            setCurrentPage(1); // Explicitly set page to 1
            await loadGenreBooks(genre);
            updateURL({ book: null, genre: genre, view: '', search: '', page: 1 });
        } catch (err) {
            console.error("Error in handleGenreClick:", err);
            setError(`Failed to load ${genre} books`);
        }
    };

    const handleBookClick = (bookId) => {
        const id = Number(bookId);
        if (!bookId || isNaN(id) || id <= 0) {
            console.error('Invalid bookId:', bookId);
            setError('Cannot navigate to book with invalid ID');
            return;
        }
        setSelectedBookId(id);
        setCurrentPage(1);
        updateURL({ book: id.toString(), page: 1 });
    };

    const handleBackFromBook = () => {
        setSelectedBookId(null);
        updateURL({ book: null });
    };

    const handleTop10Click = async () => {
        try {
            setSelectedBookId(null);
            setCurrentView('top10');
            setCurrentGenre('');
            setSearchTerm('');
            setCurrentPage(1);
            await loadTop10Books();
            updateURL({ book: null, view: 'top10', genre: '', search: '', page: 1 });
        } catch (err) {
            console.error("Error in handleTop10Click:", err);
            setError("Failed to load top 10 books");
        }
    };

    const handleAllBooks = () => {
        setSelectedBookId(null);
        setSearchTerm("");
        setCurrentGenre("");
        setCurrentView("");
        setCurrentPage(1);
        fetchBooks();
        updateURL({ book: null, search: '', genre: '', view: '', page: 1 });
    };

    // Fetch waiting list and check for notified status
    const fetchWaitingList = async () => {
        try {
            const response = await waitingListService.getMyWaitingList();
            if (response && response.code === 0 && response.data) {
                // Check if any waiting list item has status "notified"
                const hasNotified = response.data.some(item => item.status === "notified");
                setHasNotification(hasNotified);
            } else {
                setHasNotification(false); // No notifications if response is invalid
            }
        } catch (err) {
            console.error("fetchWaitingList error:", err);
            if (err.message.includes("Session expired or user banned")) {
                logoutUser();
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                console.warn("Failed to check waiting list notifications");
                setHasNotification(false); // Safe default on error
            }
        }
    };

    // Pagination logic
    const indexOfLastBook = currentPage * booksPerPage;
    const indexOfFirstBook = indexOfLastBook - booksPerPage;
    const currentBooks = books.slice(indexOfFirstBook, indexOfLastBook);
    const totalPages = Math.ceil(books.length / booksPerPage);

    const paginate = (pageNumber) => {
        setCurrentPage(pageNumber);
        updateURL({ page: pageNumber });
    };

    const nextPage = () => {
        const next = Math.min(currentPage + 1, totalPages);
        setCurrentPage(next);
        updateURL({ page: next });
    };

    const prevPage = () => {
        const prev = Math.max(currentPage - 1, 1);
        setCurrentPage(prev);
        updateURL({ page: prev });
    };

    const getPaginationButtons = () => {
        const buttons = [];
        buttons.push(1);
        if (totalPages <= 5) {
            for (let i = 2; i <= totalPages; i++) {
                buttons.push(i);
            }
        } else {
            if (currentPage <= 3) {
                buttons.push(2, 3, 4);
                buttons.push("...");
                buttons.push(totalPages);
            } else if (currentPage >= totalPages - 2) {
                buttons.push("...");
                for (let i = totalPages - 3; i <= totalPages; i++) {
                    buttons.push(i);
                }
            } else {
                buttons.push("...");
                buttons.push(currentPage - 1);
                buttons.push(currentPage);
                buttons.push(currentPage + 1);
                buttons.push("...");
                buttons.push(totalPages);
            }
        }
        return buttons;
    };

    return (
        <div className={`dashboard ${theme === "dark" ? "dark-mode" : ""}`}>
            <nav className="sidebar">
                <div className="logo">
                    <span className="logo-icon">📚</span>
                    <span className="logo-text">Library Hub</span>
                </div>
                <div className="nav-box">
                    <h3>Navigation</h3>
                    <ul>
                        <li onClick={handleAllBooks}>
                            All Books
                        </li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            updateURL({ book: null });
                            navigate("/reservations");
                        }}>
                            My Reservations
                        </li>
                        <li
                            onClick={() => {
                                setSelectedBookId(null);
                                updateURL({ book: null });
                                navigate("/waitinglist");
                            }}
                            className={hasNotification ? "shiny" : ""}
                        >
                            My Waiting List
                            {hasNotification && <span className="notification-dot"></span>}
                        </li>
                    </ul>
                </div>
                <div className="category-box">
                    <h3>Categories</h3>
                    <ul>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Fantasy");
                        }}>Fantasy</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Science Fiction");
                        }}>Science Fiction</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Mystery");
                        }}>Mystery</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Romance");
                        }}>Romance</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Historical Fiction");
                        }}>Historical Fiction</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Horror");
                        }}>Horror</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Thriller");
                        }}>Thriller</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Young Adult");
                        }}>Young Adult</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Non-Fiction");
                        }}>Non-Fiction</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Poetry Classic");
                        }}>Poetry/Classic</li>
                    </ul>
                </div>
            </nav>
            <div className="main-content">
                <Header
                    user={user}
                    setIsAuthenticated={setIsAuthenticated}
                    isAuthenticated={isAuthenticated}
                    searchTerm={searchTerm}
                    setSearchTerm={setSearchTerm}
                    handleSearch={handleSearch}
                />
                <div className="content-wrapper">
                    <div className="central-content">
                        {selectedBookId ? (
                            <Book
                                isAuthenticated={isAuthenticated}
                                setIsAuthenticated={setIsAuthenticated}
                                id={selectedBookId}
                                onBack={handleBackFromBook} // Use the new handler
                            />
                        ) : (
                            <>
                                <div className="welcome">
                                    <h1>Welcome to our Digital Library</h1>
                                    <p>Explore our collection of {books.length} books</p>
                                </div>
                                <div className="book-listing">
                                    <div className="book-listing-header">
                                        <h2>{currentTitle}</h2>
                                        <div className="book-count">Showing {currentBooks.length} of {books.length} books</div>
                                    </div>
                                    {loading && <div className="loading">Loading books...</div>}
                                    {error && (
                                        <div className="error">
                                            {error}
                                            <button onClick={() => fetchBooks()} className="retry-btn">
                                                Retry
                                            </button>
                                        </div>
                                    )}
                                    {!loading && !error && (
                                        <div className="books-grid">
                                            {currentBooks.map((book) => (
                                                <div
                                                    key={book.bookId || book.title + book.author}
                                                    className="book-card"
                                                    onClick={() => book.bookId ? handleBookClick(book.bookId) : setError('Cannot navigate: Book ID is missing')}
                                                    style={{ cursor: book.bookId ? 'pointer' : 'not-allowed' }}
                                                >
                                                    <div className="book-cover">
                                                        {book.path ? (
                                                            <img
                                                                src={`http://localhost:8432/api/images/books/${encodeURIComponent(book.path)}`}
                                                                alt={book.title}
                                                                onError={(e) => {
                                                                    console.error('Image failed to load:', {
                                                                        bookTitle: book.title,
                                                                        databasePath: book.path,
                                                                        imageSrc: `http://localhost:8432/api/images/books/${encodeURIComponent(book.path)}`,
                                                                        timestamp: new Date().toISOString(),
                                                                    });
                                                                    e.target.src = '/placeholder-book-cover.jpg';
                                                                }}
                                                                onLoad={() => console.log('Image loaded successfully:', `http://localhost:8432/api/images/books/${encodeURIComponent(book.path)}`)}
                                                            />
                                                        ) : (
                                                            <div className="book-cover-placeholder">📖</div>
                                                        )}
                                                    </div>
                                                    <div className="book-info">
                                                        <h3>{book.title || 'Untitled'}</h3>
                                                        <p className="books-author">by {book.author || 'Unknown'}</p>
                                                        <p className="books-genre">{book.genre || 'Unknown'}</p>
                                                        {book.avgRating != null && (
                                                            <div className="book-rating">
                                                                {book.avgRating.toFixed(1)} ★
                                                            </div>
                                                        )}
                                                        <div className="book-status">
                                                            <span className={`availability ${book.available ? 'available' : 'unavailable'}`}>
                                                                {book.available ? 'Available' : 'Unavailable'}
                                                            </span>
                                                            {book.quantity !== undefined && (
                                                                <span className="quantity">Qty: {book.quantity}</span>
                                                            )}
                                                            {!book.bookId && (
                                                                <span className="error-text">Missing Book ID</span>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                    {!loading && !error && books.length === 0 && (
                                        <div className="no-books">
                                            No books found. <button onClick={() => fetchBooks()}>Load all books</button>
                                        </div>
                                    )}
                                    {!loading && !error && books.length > 0 && (
                                        <div className="pagination">
                                            <button onClick={prevPage} disabled={currentPage === 1}>
                                                Previous
                                            </button>
                                            {getPaginationButtons().map((page, index) => (
                                                <React.Fragment key={index}>
                                                    {typeof page === "number" ? (
                                                        <button
                                                            onClick={() => paginate(page)}
                                                            className={`pagination-number ${currentPage === page ? 'active' : ''}`}
                                                        >
                                                            {page}
                                                        </button>
                                                    ) : (
                                                        <span className="pagination-ellipsis">...</span>
                                                    )}
                                                </React.Fragment>
                                            ))}
                                            <button onClick={nextPage} disabled={currentPage === totalPages}>
                                                Next
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </>
                        )}
                    </div>
                    <div className="right-panel">
                        <div className="stats-box">
                            <h3>Library Statistics</h3>
                            <p>Total books: {totalStats.totalBooks}</p>
                            <p>Available books: {totalStats.totalAvailable}</p>
                            <p>Categories: {totalStats.totalCategories}</p>
                        </div>
                        <div className="recommend-box">
                            <h3>Quick Actions</h3>
                            <ul>
                                <li onClick={handleTop10Click}>Top 10 Rated Books</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
            {/* AI Assistant Chat Panel */}
            <ChatPanel />
        </div>
    );
};

export default Dashboard;
