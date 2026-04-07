import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { waitingListService } from "../services/waitingListService";
import { logoutUser } from "../services/authApi";
import "./WaitingList.css"; // Create this CSS file based on Reservations.css as a template

const WaitingList = ({ isAuthenticated, setIsAuthenticated }) => {
    const navigate = useNavigate();
    const [waitingItems, setWaitingItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [cancelling, setCancelling] = useState(null);

    useEffect(() => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        fetchWaitingList();
    }, [isAuthenticated, navigate, setIsAuthenticated]);

    const fetchWaitingList = async () => {
        try {
            setLoading(true);
            setError("");
            await waitingListService.getEligibility();
            const response = await waitingListService.getMyWaitingList();
            if (response && response.code === 0 && response.data) {
                setWaitingItems(Array.isArray(response.data) ? response.data : []);
            } else {
-                setError(response?.msg || "Invalid response format from server");
+                setError(response?.message || "Invalid response format from server");
            }
        } catch (err) {
            console.error("fetchWaitingList error:", err);
            if (err.message.includes("Session expired or user banned")) {
                logoutUser();
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setError(err.message || "Failed to load waiting list");
            }
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async (bookId) => {
        if (!window.confirm("Are you sure you want to cancel this waiting list entry?")) return;

        try {
            setError("");
            setSuccessMessage("");
            setCancelling(bookId);
            const response = await waitingListService.cancelWaiting(bookId);
            if (response && response.code === 0) {
                setSuccessMessage("Waiting list entry cancelled successfully!");
                setTimeout(() => setSuccessMessage(""), 3000);
                await fetchWaitingList(); // Refresh list
            } else {
-                setError(response?.msg || "Failed to cancel waiting list entry");
+                setError(response?.message || "Failed to cancel waiting list entry");
            }
        } catch (err) {
            console.error("handleCancel error:", err);
            if (err.message.includes("Session expired or user banned")) {
                logoutUser();
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setError(err.message || "Failed to cancel waiting list entry");
            }
        } finally {
            setCancelling(null);
        }
    };

    const checkEligibility = async () => {
        try {
            setLoading(true);
            setError("");
            setSuccessMessage("");
            const response = await waitingListService.getEligibility();
            if (response && response.code === 0) {
                setSuccessMessage("Eligibility checked and notifications updated!");
                setTimeout(() => setSuccessMessage(""), 3000);
                await fetchWaitingList(); // Refresh to show updated statuses
            } else {
-                setError(response?.msg || "Failed to check eligibility");
+                setError(response?.message || "Failed to check eligibility");
            }
        } catch (err) {
            console.error("checkEligibility error:", err);
            if (err.message.includes("Session expired or user banned")) {
                logoutUser();
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setError(err.message || "Failed to check eligibility");
            }
        } finally {
            setLoading(false);
        }
    };

    // Helper to format status
    const formatStatus = (status) => {
        if (!status) return 'Unknown';
        return status.charAt(0).toUpperCase() + status.slice(1);
    };

    return (
        <div className="waiting-list-container">
            <h2>My Waiting List</h2>
            <button onClick={() => navigate("/dashboard")} className="back-button">
                ← Back to Dashboard
            </button>
            <button onClick={checkEligibility} disabled={loading} className="check-eligibility-btn">
                {loading ? "Checking..." : "Check Availability"}
            </button>

            {loading && <div className="loading">Loading waiting list...</div>}
            {error && (
                <div className="error">
                    {error}
                    <button onClick={fetchWaitingList} className="retry-btn">
                        Retry
                    </button>
                </div>
            )}
            {successMessage && <div className="success">{successMessage}</div>}

            {!loading && !error && waitingItems.length > 0 && (
                <table className="waiting-list-table">
                    <thead>
                    <tr>
                        <th>Book Title</th>
                        <th>Join Date</th>
                        <th>Status</th>
                        <th>Notification Date</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {waitingItems.map((item) => (
                        <tr key={item.waitId}>
                            <td>{item.bookTitle || 'Unknown'}</td>
                            <td>{item.joinDate ? new Date(item.joinDate).toLocaleDateString() : 'N/A'}</td>
                            <td>{formatStatus(item.status)}</td>
                            <td>
                                {item.status === "notified" && item.notificationDate
                                    ? new Date(item.notificationDate).toLocaleDateString()
                                    : ""}
                            </td>
                            <td>
                                {["waiting", "notified"].includes(item.status) && (
                                    <button
                                        onClick={() => handleCancel(item.bookId)}
                                        className="cancel-btn"
                                        disabled={cancelling === item.bookId}
                                    >
                                        {cancelling === item.bookId ? "Cancelling..." : "Cancel"}
                                    </button>
                                )}
                                {item.status === "notified" && (
                                    <button
                                        onClick={() => navigate(`/dashboard?book=${item.bookId}&reserve=true`)}
                                        className="reserve-btn"
                                    >
                                        Reserve Now
                                    </button>
                                )}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            )}

            {!loading && !error && waitingItems.length === 0 && (
                <div className="no-waiting-items">
                    You have no books on your waiting list.
                    <button onClick={() => navigate("/dashboard")}>Browse Books</button>
                </div>
            )}
        </div>
    );
};

export default WaitingList;