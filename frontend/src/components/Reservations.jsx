import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { reservationService } from "../services/reservationService";
import { logoutUser } from "../services/authApi";
import "./Reservations.css";

const Reservations = ({ isAuthenticated, setIsAuthenticated }) => {
    const navigate = useNavigate();
    const [reservations, setReservations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [cancelling, setCancelling] = useState(null);
    const [updatingPickup, setUpdatingPickup] = useState(null);
    const [newPickupDate, setNewPickupDate] = useState("");

    useEffect(() => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }

        const fetchReservations = async () => {
            try {
                setLoading(true);
                setError("");
                const response = await reservationService.getUserReservations();
                console.log("getUserReservations response:", response);

                if (response && response.code === 0 && response.data) {
                    setReservations(Array.isArray(response.data) ? response.data : []);
                } else {
                    setError(response?.message || "Invalid response format from server");
                }
            } catch (err) {
                console.error("fetchReservations error:", err);
                if (err.message.includes("Session expired or user banned")) {
                    logoutUser();
                    setIsAuthenticated(false);
                    navigate("/login");
                } else {
                    setError(err.message || "Failed to load reservations");
                }
            } finally {
                setLoading(false);
            }
        };

        fetchReservations();
    }, [isAuthenticated, navigate, setIsAuthenticated]);

    const handleCancel = async (reservationId) => {
        if (!window.confirm("Are you sure you want to cancel this reservation?")) return;

        try {
            setError("");
            setSuccessMessage("");
            setCancelling(reservationId);
            const response = await reservationService.cancelReservation(reservationId);
            console.log("cancelReservation response:", response);
            if (response && response.code === 0) {
                setReservations(reservations.map(r =>
                    r.reservationId === reservationId ? { ...r, reservationStatus: "cancelled" } : r
                ));
                setSuccessMessage("Reservation cancelled successfully!");
                setTimeout(() => setSuccessMessage(""), 3000);
            } else {
                setError(response?.message || "Failed to cancel reservation");
            }
        } catch (err) {
            console.error("cancelReservation error:", err);
            if (err.message.includes("Session expired or user banned")) {
                logoutUser();
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setError(err.message || "Failed to cancel reservation");
            }
        } finally {
            setCancelling(null);
        }
    };

    const startUpdatePickup = (reservationId, currentPickup) => {
        setUpdatingPickup(reservationId);
        // Format current pickup date to YYYY-MM-DD
        setNewPickupDate(currentPickup ? new Date(currentPickup).toISOString().slice(0, 10) : "");
    };

    const handleUpdatePickup = async (reservationId) => {
        if (!newPickupDate) {
            setError("Please select a new pickup date.");
            return;
        }

        try {
            setError("");
            setSuccessMessage("");
            // Send date as YYYY-MM-DD format
            const response = await reservationService.updatePickupTime(reservationId, { pickupTime: newPickupDate });
            console.log("updatePickupTime response:", response);
            if (response && response.code === 0) {
                setReservations(reservations.map(r =>
                    r.reservationId === reservationId ? { ...r, pickupTime: new Date(newPickupDate) } : r
                ));
                setSuccessMessage("Pickup date updated successfully!");
                setTimeout(() => setSuccessMessage(""), 3000);
                setUpdatingPickup(null);
                setNewPickupDate("");
            } else {
                setError(response?.message || "Failed to update pickup date");
            }
        } catch (err) {
            console.error("updatePickupTime error:", err);
            if (err.message.includes("Session expired or user banned")) {
                logoutUser();
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setError(err.message || "Failed to update pickup date");
            }
        }
    };

    const formatStatus = (status) => {
        if (!status) return 'Unknown';
        return status.charAt(0).toUpperCase() + status.slice(1);
    };

    const formatDate = (date) => date ? new Date(date).toLocaleDateString() : 'N/A';

    return (
        <div className="reservations-container">
            <h2>My Reservations</h2>
            <button onClick={() => navigate("/dashboard")} className="back-button">
                ← Back to Dashboard
            </button>

            {loading && <div className="loading">Loading reservations...</div>}
            {error && (
                <div className="error">
                    {error}
                    <button onClick={() => window.location.reload()} className="retry-btn">
                        Retry
                    </button>
                </div>
            )}
            {successMessage && <div className="success">{successMessage}</div>}

            {!loading && !error && reservations.length > 0 && (
                <table className="reservations-table">
                    <thead>
                    <tr>
                        <th>Book Title</th>
                        <th>Reservation Date</th>
                        <th>Pickup Date</th>
                        <th>Due Date</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {reservations.map((res) => (
                        <tr key={res.reservationId}>
                            <td>{res.bookTitle || 'Unknown'}</td>
                            <td>{formatDate(res.reservationTime)}</td>
                            <td>
                                {updatingPickup === res.reservationId ? (
                                    <input
                                        type="date" // Changed from datetime-local to date
                                        value={newPickupDate}
                                        onChange={(e) => setNewPickupDate(e.target.value)}
                                        min={new Date().toISOString().slice(0, 10)} // Prevent past dates
                                    />
                                ) : (
                                    formatDate(res.pickupTime)
                                )}
                            </td>
                            <td>{formatDate(res.dueTime)}</td>
                            <td>{formatStatus(res.reservationStatus)}</td>
                            <td>
                                {res.reservationStatus === "reserved" && (
                                    <>
                                        {updatingPickup === res.reservationId ? (
                                            <>
                                                <button
                                                    onClick={() => handleUpdatePickup(res.reservationId)}
                                                    className="update-btn"
                                                >
                                                    Save
                                                </button>
                                                <button
                                                    onClick={() => setUpdatingPickup(null)}
                                                    className="cancel-update-btn"
                                                >
                                                    Cancel
                                                </button>
                                            </>
                                        ) : (
                                            <button
                                                onClick={() => startUpdatePickup(res.reservationId, res.pickupTime)}
                                                className="edit-btn"
                                            >
                                                Edit Pickup
                                            </button>
                                        )}
                                    </>
                                )}
                                {["reserved", "borrowed"].includes(res.reservationStatus) &&
                                    updatingPickup !== res.reservationId && (
                                        <button
                                            onClick={() => handleCancel(res.reservationId)}
                                            className="cancel-btn"
                                            disabled={cancelling === res.reservationId}
                                        >
                                            {cancelling === res.reservationId ? "Cancelling..." : "Cancel"}
                                        </button>
                                    )}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            )}

            {!loading && !error && reservations.length === 0 && (
                <div className="no-reservations">
                    You have no reservations.
                    <button onClick={() => navigate("/dashboard")}>Browse Books</button>
                </div>
            )}
        </div>
    );
};

export default Reservations;