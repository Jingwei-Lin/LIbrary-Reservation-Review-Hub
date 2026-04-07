import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { bookService } from "../services/bookService";
import { reservationService } from "../services/reservationService";
import { reviewService } from "../services/reviewService";
import { waitingListService } from "../services/waitingListService";
import { logoutUser } from "../services/authApi";
import Portal from './Portal';
import "./Book.css";

const Book = ({ isAuthenticated, setIsAuthenticated, id, onBack }) => {
    // const { id } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const [book, setBook] = useState(null);
    const [reviews, setReviews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [newReview, setNewReview] = useState({ rating: 0, comment: "" });
    const [isOnWaitingList, setIsOnWaitingList] = useState(false);
    const [isNotified, setIsNotified] = useState(false);
    const [waitingStatus, setWaitingStatus] = useState(null);
    const [hasActiveReservation, setHasActiveReservation] = useState(false);
    const [autoOpenReserve, setAutoOpenReserve] = useState(false);

    // State for delete review popup
    const [showDeletePopup, setShowDeletePopup] = useState(false);
    const [reviewToDelete, setReviewToDelete] = useState(null);

    // State for reservation popup
    const [showReservationPopup, setShowReservationPopup] = useState(false);
    const [reservationForm, setReservationForm] = useState({
        pickupTime: "",
        dueTime: ""
    });
    const [reservationLoading, setReservationLoading] = useState(false);
    const [reservationError, setReservationError] = useState("");
    const [reservationSuccess, setReservationSuccess] = useState("");

    // Review submission feedback
    const [reviewSuccess, setReviewSuccess] = useState("");
    const [reviewError, setReviewError] = useState("");
    const [bookRating, setBookRating] = useState(null);

    useEffect(() => {
        const fetchBookAndReviews = async () => {
            try {
                setLoading(true);
                setError("");

                const promises = [
                    bookService.getBookById(id).catch(err => ({ code: -1, message: err.message })),
                    bookService.getRatingByBookId(id).catch(err => ({ code: -1, message: err.message })),
                    reviewService.getReviewsByBookId(id).catch(err => ({ code: -1, message: err.message }))
                ];

                if (isAuthenticated) {
                    promises.push(reservationService.hasActiveReservation(id).catch(err => ({ code: -1, message: err.message })));
                    promises.push(waitingListService.getMyWaitingList().catch(err => ({ code: -1, message: err.message })));
                }

                const responses = await Promise.all(promises);

                let bookRes, ratingRes, reviewRes, reservationRes, waitRes;
                [bookRes, ratingRes, reviewRes, reservationRes, waitRes] = responses.length === 5 ? responses : [...responses, null, null];

                // Process book
                if (bookRes && bookRes.code === 0 && bookRes.data) {
                    setBook(bookRes.data);
                } else {
                    console.warn("Book fetch failed:", bookRes?.message);
                    setError(bookRes?.message || "Invalid response format for book");
                    return; // Early return if book fails, as it's critical
                }

                // Process rating
                if (ratingRes && ratingRes.code === 0 && ratingRes.data) {
                    setBookRating(ratingRes.data);
                } else {
                    console.warn("No rating data:", ratingRes?.message);
                    setBookRating(null);
                }

                // Process reviews
                if (reviewRes && reviewRes.code === 0 && reviewRes.data) {
                    setReviews(Array.isArray(reviewRes.data) ? reviewRes.data : []);
                } else {
                    console.warn(reviewRes?.message || "No reviews found");
                    setReviews([]);
                }

                // If authenticated, process reservation and waiting list
                if (isAuthenticated) {
                    // Process reservation
                    if (reservationRes && reservationRes.code === 0) {
                        setHasActiveReservation(reservationRes.data || false);
                    } else {
                        console.warn(reservationRes?.message || "Failed to check reservation status");
                        setHasActiveReservation(false);
                    }

                    // Process waiting list
                    if (waitRes && waitRes.code === 0 && waitRes.data) {
                        const waitItem = waitRes.data.find((item) => item.bookId === parseInt(id));
                        if (waitItem) {
                            setIsOnWaitingList(waitItem.status === "waiting" || waitItem.status === "notified");
                            setIsNotified(waitItem.status === "notified");
                            setWaitingStatus(waitItem.status);
                        } else {
                            setIsOnWaitingList(false);
                            setIsNotified(false);
                            setWaitingStatus(null);
                        }
                    } else {
                        console.warn(waitRes?.message || "Failed to check waiting list status");
                        setIsOnWaitingList(false);
                        setIsNotified(false);
                        setWaitingStatus(null);
                    }
                }
            } catch (err) {
                if (err.message.includes("Session expired or user banned") && isAuthenticated) {
                    localStorage.removeItem('user');
                    sessionStorage.removeItem('user');
                    logoutUser();
                    setIsAuthenticated(false);
                    navigate("/login");
                    return;
                }
                setError(err.message || "Failed to load book details or reviews");
                console.error("Error fetching book or reviews:", err);
                setReviews([]);
            } finally {
                setLoading(false);
            }
        };

        fetchBookAndReviews();
    }, [id, setIsAuthenticated, isAuthenticated, hasActiveReservation, setHasActiveReservation]);

    useEffect(() => {
        if (showReservationPopup) {
            document.body.classList.add('popup-open');
        } else {
            document.body.classList.remove('popup-open');
        }

        return () => {
            document.body.classList.remove('popup-open');
        };
    }, [showReservationPopup]);

    // Check for ?reserve=true in URL and set flag to auto-open popup
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        if (params.get('reserve') === 'true') {
            setAutoOpenReserve(true);
            // Remove the 'reserve' param from URL to clean it up
            params.delete('reserve');
            navigate({ search: params.toString() }, { replace: true });
        }
    }, [location.search, navigate]);

    // Auto-open the reservation popup after data is loaded if flag is set
    useEffect(() => {
        if (autoOpenReserve && !loading && book && !error) {
            // Optional: Add eligibility check (e.g., if notified or available)
            if (!hasActiveReservation && (book.available || isNotified)) {
                setShowReservationPopup(true);
            }
            setAutoOpenReserve(false); // Reset flag after handling
        }
    }, [autoOpenReserve, loading, book, error, hasActiveReservation, isNotified]);

    const handleBack = () => {
        if (onBack) {
            // onBack(); // Call onBack if provided
            navigate('/dashboard');
        } else {
            navigate('/dashboard'); // Fallback to dashboard
        }
    };

    const handleReserve = () => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        setShowReservationPopup(true);
        // navigate(`/reservation/form/${id}?fromBook=${id}`);
    };

    const handleReservationSubmit = async (e) => {
        e.preventDefault();
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }

        setReservationLoading(true);
        setReservationError("");
        setReservationSuccess("");

        try {
            const pickupDate = new Date(reservationForm.pickupTime);
            const dueDate = new Date(reservationForm.dueTime);
            const today = new Date();
            today.setHours(0, 0, 0, 0);

            if (pickupDate < today) {
                throw new Error("Pickup date cannot be in the past");
            }
            if (dueDate <= pickupDate) {
                throw new Error("Due date must be after pickup date");
            }

            // Calculate maximum allowed due date (7 days from pickup)
            const maxDueDate = new Date(pickupDate);
            maxDueDate.setDate(pickupDate.getDate() + 30);

            if (dueDate > maxDueDate) {
                throw new Error("Reservation period cannot exceed 30 days");
            }

            const response = await reservationService.createReservation(id, {
                pickupTime: reservationForm.pickupTime,
                dueTime: reservationForm.dueTime
            });

            if (response && response.code === 0) {
                setReservationSuccess("Reservation created successfully!");
                setHasActiveReservation(true);
                setTimeout(() => {
                    setShowReservationPopup(false);
                    setReservationForm({ pickupTime: "", dueTime: "" });
                    navigate("/reservations");
                }, 2000);
            } else if (response && response.code === 50001) {
                setReservationError(response.message);
            } else {
                setReservationError(response?.message || "Failed to create reservation");
            }
        } catch (err) {
            console.error("createReservation error:", err);
            if (err.message.includes("Session expired") || err.message.includes("Please log in")) {
                localStorage.removeItem('user');
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setReservationError(err.message || "Failed to create reservation");
            }
        } finally {
            setReservationLoading(false);
        }
    };

    const handleReservationFormChange = (e) => {
        setReservationForm({ ...reservationForm, [e.target.name]: e.target.value });
    };

    const handleCloseReservationPopup = () => {
        setShowReservationPopup(false);
        setReservationForm({ pickupTime: "", dueTime: "" });
        setReservationError("");
        setReservationSuccess("");
    };

    const handleJoinWaiting = async () => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        try {
            const response = await waitingListService.addToWaitingList(parseInt(id));
            if (response && response.code === 0) {
                window.alert("Successfully added to waiting list!");
                setIsOnWaitingList(true);
                setIsNotified(false);
                setWaitingStatus("waiting");
            } else {
                window.alert(response.message || "Failed to add to waiting list");
            }
        } catch (err) {
            window.alert(err.message || "Failed to add to waiting list");
        }
    };

    const handleCancelWaiting = async () => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        if (!window.confirm("Are you sure you want to cancel your waiting list entry?")) return;
        try {
            const response = await waitingListService.cancelWaiting(parseInt(id));
            if (response && response.code === 0) {
                window.alert("Waiting list entry cancelled successfully!");
                setIsOnWaitingList(false);
                setIsNotified(false);
                setWaitingStatus(null);
            } else {
                window.alert(response.message || "Failed to cancel waiting list entry");
            }
        } catch (err) {
            window.alert(err.message || "Failed to cancel waiting list entry");
        }
    };

    const handleAddReview = async (e) => {
        e.preventDefault();
        setReviewSuccess("");
        setReviewError("");

        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        if (newReview.rating < 1 || newReview.rating > 5) {
            setReviewError("Rating must be between 1 and 5.");
            return;
        }
        if (!newReview.comment.trim()) {
            setReviewError("Comment cannot be empty.");
            return;
        }

        try {
            setError("");
            const reviewData = { rating: newReview.rating, comment: newReview.comment };
            await reviewService.addReview(parseInt(id), reviewData);

            setReviewSuccess("✅ Review submitted successfully!");

            setNewReview({ rating: 0, comment: "" });

            // Update reviews immediately
            const updatedReviews = await reviewService.getReviewsByBookId(id);
            if (updatedReviews && updatedReviews.code === 0 && Array.isArray(updatedReviews.data)) {
                setReviews(updatedReviews.data);
                await fetchBookRating();
            } else {
                console.warn("No updated reviews found");
            }

        } catch (err) {
            window.alert(err.message || "Failed to add review");
            console.error("Error adding review:", err);
        }
    };


    const handleDeleteReview = async (reviewId) => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        // open confirmation popup
        setReviewToDelete(reviewId);
        setShowDeletePopup(true);
    };

    const confirmDeleteReview = async () => {
        try {
            setError("");
            await reviewService.deleteReview(reviewToDelete);
            setShowDeletePopup(false);
            setReviewToDelete(null);

            // Refresh the reviews after deletion
            const reviewResponse = await reviewService.getReviewsByBookId(id);
            if (reviewResponse && reviewResponse.code === 0 && Array.isArray(reviewResponse.data)) {
                setReviews(reviewResponse.data);
                await fetchBookRating();
            }
        } catch (err) {
            console.error("Error deleting review:", err);
            setShowDeletePopup(false);
            setReviewToDelete(null);
        }
    };

    const cancelDeleteReview = () => {
        setShowDeletePopup(false);
        setReviewToDelete(null);
    };

    const handleStarClick = (rating) => {
        setNewReview({ ...newReview, rating });
    };

    const fetchBookRating = async () => {
        try {
            const response = await bookService.getRatingByBookId(id);
            if (response && response.code === 0 && response.data) {
                setBookRating(response.data);
            } else {
                setBookRating(null);
            }
        } catch (err) {
            console.warn("Failed to refresh rating:", err);
            setBookRating(null);
        }
    };

    return (
        <div className="book-detail-container">
            {/* Delete Review Popup */}
            {showDeletePopup && (
                <Portal>
                    <div className="popup-overlay">
                        <div className="delete-popup">
                            <div className="popup-header">
                                <h3>Delete Review</h3>
                                <button
                                    className="close-popup"
                                    onClick={cancelDeleteReview}
                                    aria-label="Close delete popup"
                                >
                                    ×
                                </button>
                            </div>
                            <div className="popup-body">
                                <p>Are you sure you want to delete this review <strong>permanently</strong>?</p>
                            </div>
                            <div className="popup-actions">
                                <button
                                    className="cancel-btn"
                                    onClick={cancelDeleteReview}
                                >
                                    Cancel
                                </button>
                                <button
                                    className="confirm-btn"
                                    onClick={confirmDeleteReview}
                                >
                                    Yes, Delete
                                </button>
                            </div>
                        </div>
                    </div>
                </Portal>
            )}

            {/* Reservation Popup */}
            {showReservationPopup && (
                <Portal>
                    <div className="popup-overlay">
                        <div className="reservation-popup">
                            <div className="popup-header">
                                <h3>Reserve "{book?.title}"</h3>
                                <button
                                    className="close-popup"
                                    onClick={handleCloseReservationPopup}
                                    aria-label="Close reservation popup"
                                >
                                    ×
                                </button>
                            </div>
                            <form onSubmit={handleReservationSubmit} className="reservation-form">
                                <div className="form-group">
                                    <label htmlFor="pickupTime">Pickup Date:</label>
                                    <input
                                        type="date"
                                        id="pickupTime"
                                        name="pickupTime"
                                        value={reservationForm.pickupTime}
                                        onChange={handleReservationFormChange}
                                        required
                                        min={new Date().toISOString().split("T")[0]}
                                        max={(() => {
                                            const maxDate = new Date();
                                            maxDate.setDate(maxDate.getDate() + 7);
                                            return maxDate.toISOString().split("T")[0];
                                        })()}
                                    />
                                    <small className="date-hint">Select a date within the next 7 days</small>
                                </div>
                                <div className="form-group">
                                    <label htmlFor="dueTime">Due Date:</label>
                                    <input
                                        type="date"
                                        id="dueTime"
                                        name="dueTime"
                                        value={reservationForm.dueTime}
                                        onChange={handleReservationFormChange}
                                        required
                                        min={reservationForm.pickupTime || new Date().toISOString().split("T")[0]}
                                        max={(() => {
                                            if (reservationForm.pickupTime) {
                                                const maxDueDate = new Date(reservationForm.pickupTime);
                                                maxDueDate.setDate(maxDueDate.getDate() + 30);
                                                return maxDueDate.toISOString().split("T")[0];
                                            }
                                            const maxDate = new Date();
                                            maxDate.setDate(maxDate.getDate() + 30);
                                            return maxDate.toISOString().split("T")[0];
                                        })()}
                                    />
                                    <small className="date-hint">
                                        {reservationForm.pickupTime
                                            ? `Due date must be within 30 days of pickup (max: ${(() => {
                                                const maxDueDate = new Date(reservationForm.pickupTime);
                                                maxDueDate.setDate(maxDueDate.getDate() + 30);
                                                return maxDueDate.toISOString().split("T")[0];
                                            })()})`
                                            : "Select pickup date first"
                                        }
                                    </small>
                                </div>
                                {reservationError && <p className="error">{reservationError}</p>}
                                {reservationSuccess && <p className="success">{reservationSuccess}</p>}
                                <div className="popup-actions">
                                    <button
                                        type="button"
                                        onClick={handleCloseReservationPopup}
                                        className="cancel-btn"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        type="submit"
                                        disabled={reservationLoading}
                                        className="confirm-btn"
                                    >
                                        {reservationLoading ? "Reserving..." : "Confirm Reservation"}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </Portal>
            )}
            <button className="book-back-button" onClick={handleBack} aria-label="Back to dashboard">
                ← Back
            </button>

            {loading && <div className="loading">Loading book details...</div>}
            {error && (
                <div className="error">
                    {error}
                    <button onClick={() => window.location.reload()} className="retry-btn" aria-label="Retry loading">
                        Retry
                    </button>
                </div>
            )}

            {!loading && !error && book && (
                <div className="book-detail-card">
                    <div className="book-detail-cover">
                        {book.path ? (
                            <img
                                src={`/api/images/books/${encodeURIComponent(book.path)}`}
                                alt={book.title}
                                onError={(e) => {
                                    console.error('Image failed to load:', {
                                        bookTitle: book.title,
                                        databasePath: book.path,
                                        imageSrc: `/api/images/books/${encodeURIComponent(book.path)}`,
                                        timestamp: new Date().toISOString(),
                                    });
                                    e.target.src = '/placeholder-book-cover.jpg';
                                }}
                            />
                        ) : (
                            <div className="book-cover-placeholder">📖</div>
                        )}
                    </div>
                    <div className="book-detail-info">
                        <h1>{book.title}</h1>
                        <p className="book-author">by {book.author}</p>
                        <p className="book-genre">{book.genre}</p>

                        {bookRating && (
                            <div className="book-rating-display">
                                <div className="star-rating-large">
                                    {[1, 2, 3, 4, 5].map((star) => {
                                        const rating = bookRating.avgRating ?? 0;
                                        const fullStars = Math.floor(rating);
                                        const decimal = rating - fullStars;
                                        let isFilled = star <= fullStars;
                                        let isHalf = false;
                                        if (!isFilled && star === fullStars + 1 && decimal >= 0.25 && decimal < 0.75) {
                                            isHalf = true;
                                        } else if (!isFilled && star === fullStars + 1 && decimal >= 0.75) {
                                            isFilled = true;
                                        }
                                        return (
                                            <span key={star} className={`star large ${isFilled ? "filled" : isHalf ? "half" : ""}`}> ★ </span>
                                        );
                                    })}
                                </div>
                                {(bookRating?.avgRating ?? 0) > 0 ? (
                                    <p className="rating-text">
                                        <strong>{bookRating.avgRating.toFixed(1)}</strong> out of 5
                                    </p>
                                ) : (
                                    <p className="rating-text">No ratings yet</p>
                                )}
                            </div>
                        )}

                        <p className="book-description-title">Description:</p>
                        <p className="book-description">{book.description}</p>
                        <div className="book-status">
                            <span className={`availability ${book.available ? 'available' : 'unavailable'}`}>
                                {book.available ? 'Available' : 'Unavailable'}
                            </span>
                            {book.quantity !== undefined && (
                                <span className="quantity">Quantity: {book.quantity}</span>
                            )}
                        </div>
                        {hasActiveReservation ? (
                            <p className="reservation-status">You have already reserved this book.</p>
                        ) : book.available && book.quantity > 0 ? (
                            <button
                                className="reserve-button"
                                onClick={handleReserve}
                                aria-label="Reserve book"
                                disabled={hasActiveReservation}
                            >
                                Reserve Now
                            </button>
                        ) : (
                            <>
                                {isOnWaitingList ? (
                                    <>
                                        <p className="waiting-status">
                                            You are on the waiting list. Status: {waitingStatus?.charAt(0).toUpperCase() + waitingStatus?.slice(1)}
                                        </p>
                                        {isNotified && (
                                            <button
                                                className="reserve-button"
                                                onClick={handleReserve}
                                                aria-label="Reserve book after notification"
                                            >
                                                Reserve Now
                                            </button>
                                        )}
                                        <button
                                            className="cancel-waiting-button"
                                            onClick={handleCancelWaiting}
                                            aria-label="Cancel waiting list entry"
                                        >
                                            Cancel Waiting
                                        </button>
                                    </>
                                ) : (
                                    <button
                                        className="join-waiting-button"
                                        onClick={handleJoinWaiting}
                                        aria-label="Join waiting list"
                                    >
                                        Join Waiting List
                                    </button>
                                )}
                            </>
                        )}
                    </div>
                </div>
            )}

            {book && (
                <div className="reviews-section">
                    <h2>Reviews ({reviews.length})</h2>

                    {isAuthenticated && (
                        <form className="review-form" onSubmit={handleAddReview}>
                            <h3>Write a Review</h3>
                            <div className="form-group">
                                <label htmlFor="rating">Rating:</label>
                                <div className="star-rating" role="radiogroup" aria-label="Select review rating">
                                    {[1, 2, 3, 4, 5].map((num) => (
                                        <span
                                            key={num}
                                            className={`star ${newReview.rating >= num ? 'filled' : ''}`}
                                            onClick={() => handleStarClick(num)}
                                            role="radio"
                                            aria-checked={newReview.rating === num}
                                            tabIndex={0}
                                            onKeyDown={(e) => {
                                                if (e.key === 'Enter' || e.key === ' ') {
                                                    handleStarClick(num);
                                                    e.preventDefault();
                                                }
                                            }}
                                            aria-label={`${num} star${num > 1 ? 's' : ''}`}
                                        >
                                            ★
                                        </span>
                                    ))}
                                </div>
                            </div>
                            <div className="form-group">
                                <label htmlFor="comment">Comment:</label>
                                <textarea
                                    id="comment"
                                    value={newReview.comment}
                                    onChange={(e) => setNewReview({ ...newReview, comment: e.target.value })}
                                    required
                                    placeholder="Write your review here..."
                                    aria-label="Write your review"
                                />
                            </div>
                            <button
                                type="submit"
                                className="submit-review-button"
                                aria-label="Submit review"
                            >
                                Submit Review
                            </button>
                            {reviewSuccess && <p className="review-success">{reviewSuccess}</p>}
                            {reviewError && <p className="review-error">{reviewError}</p>}
                        </form>
                    )}

                    {reviews.length > 0 ? (
                        <div className="reviews-list">
                            {reviews.map((review) => (
                                <div key={review.reviewId} className="review-card">
                                    <div className="review-header">
                                        <span className="review-user">{review.userName}</span>
                                        <div className="review-rating" aria-label={`Rating ${review.rating} out of 5 stars`}>
                                            {[1, 2, 3, 4, 5].map((num) => (
                                                <span
                                                    key={num}
                                                    className={`star ${review.rating >= num ? 'filled' : ''}`}
                                                    aria-hidden="true"
                                                >
                                                    ★
                                                </span>
                                            ))}
                                        </div>
                                    </div>
                                    {review.comment && <p className="review-comment">{review.comment}</p>}
                                    <p className="review-date">
                                        Posted on: {new Date(review.reviewDate).toLocaleDateString()}
                                    </p>
                                    {isAuthenticated &&
                                        (review.userId === JSON.parse(sessionStorage.getItem("user"))?.id ||
                                            JSON.parse(sessionStorage.getItem("user"))?.userRole === "admin") && (
                                            <div className="review-actions">
                                                <button
                                                    className="delete-review-button"
                                                    onClick={() => handleDeleteReview(review.reviewId)}
                                                    aria-label={`Delete review by user ${review.userId}`}
                                                >
                                                    Delete
                                                </button>
                                            </div>
                                        )}
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="no-reviews">
                            <p>No reviews yet for this book. Be the first to review!</p>
                        </div>
                    )}
                </div>
            )}

            {!loading && !error && !book && (
                <div className="no-book">
                    Book not found.
                    <button
                        onClick={handleBack}
                        aria-label="Back to dashboard"
                    >
                        Back to Dashboard
                    </button>
                </div>
            )}
        </div>
    );
};

export default Book;