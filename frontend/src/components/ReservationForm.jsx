import React, { useState } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { reservationService } from "../services/reservationService";
import "./ReservationForm.css";

const ReservationForm = ({ isAuthenticated, setIsAuthenticated }) => {
    const { bookId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const [formData, setFormData] = useState({
        pickupTime: "",
        dueTime: "",
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }

        setLoading(true);
        setError("");
        setSuccessMessage("");

        try {
            const pickupDate = new Date(formData.pickupTime);
            const dueDate = new Date(formData.dueTime);
            const today = new Date();
            dueDate.setHours(23, 59, 59, 999);
            pickupDate.setHours(0, 0, 0, 1);
            today.setHours(0, 0, 0, 0);

            if (pickupDate < today) {
                throw new Error("Pickup date cannot be in the past");
            }
            if (dueDate <= pickupDate) {
                throw new Error("Due date must be after pickup date");
            }

            const response = await reservationService.createReservation(bookId, {
                pickupTime: pickupDate,
                dueTime: dueDate
            });
            console.log("createReservation response:", response);
            if (response && response.code === 0) {
                setSuccessMessage("Reservation created successfully!");
                setTimeout(() => navigate("/reservations"), 2000);
            } else if (response && response.code === 50001) {
                // Handle OPERATION_ERROR specifically
                setError(response.message);
            } else {
                setError(response?.message);
            }
        } catch (err) {
            console.error("createReservation error:", err);
            if (err.message.includes("Session expired") || err.message.includes("Please log in")) {
                localStorage.removeItem('user');
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setError(err.message || "Failed to create reservation");
            }
        } finally {
            setLoading(false);
        }
    };

    // Parse fromBook query parameter
    const params = new URLSearchParams(location.search);
    const fromBook = params.get('fromBook') || bookId;

    return (
        <div className="reservation-form-container">
            <h2>Reserve Book</h2>
            <button onClick={() => navigate(`/dashboard?book=${fromBook}`)} className="back-button">
                ← Back to Book
            </button>

            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="pickupTime">Pickup Date:</label>
                    <input
                        type="date"
                        id="pickupTime"
                        name="pickupTime"
                        value={formData.pickupTime}
                        onChange={handleChange}
                        required
                        min={new Date().toISOString().split("T")[0]}
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="dueTime">Due Date:</label>
                    <input
                        type="date"
                        id="dueTime"
                        name="dueTime"
                        value={formData.dueTime}
                        onChange={handleChange}
                        required
                        min={formData.pickupTime || new Date().toISOString().split("T")[0]}
                    />
                </div>
                {error && <p className="error">{error}</p>}
                {successMessage && <p className="success">{successMessage}</p>}
                <button type="submit" disabled={loading}>
                    {loading ? "Reserving..." : "Confirm Reservation"}
                </button>
            </form>
        </div>
    );
};

export default ReservationForm;