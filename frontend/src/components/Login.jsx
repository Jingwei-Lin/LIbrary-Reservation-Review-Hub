// Login.jsx
import './Login.css';
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { loginUser } from '../services/authApi';

const Login = ({ setIsAuthenticated, setUser, theme }) => {
    const [formData, setFormData] = useState({ userAccount: '', userPassword: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    // map backend messages to English (add more if needed)
    const errorMessages = {
        "参数为空": "No empty input allowed",
        "密码错误": "Incorrect password",
        "用户不存在": "User not found",
    };

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await loginUser(formData); // call backend
            // console.log("Login response:", response);  // log backend response
            sessionStorage.setItem('user', JSON.stringify(response));
            setUser(response);             // update app-level user state
            setIsAuthenticated(true);      // update app-level auth state
            navigate('/dashboard');        // redirect to dashboard
        } catch (err) {
            // Axios error message from authApi.js
            const backendMessage = err.message;
            setError(errorMessages[backendMessage] || backendMessage || 'Login failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={`page-container login-container ${theme === "dark" ? "dark-mode" : ""}`}>
            <button onClick={() => navigate("/dashboard")} className="back-button">
                ← Back to Dashboard
            </button>

            <h2>Login</h2>
            <form onSubmit={handleSubmit}>
                <div>
                    <label htmlFor="userAccount">Email:</label>
                    <input
                        type="email"
                        id="userAccount"
                        name="userAccount"
                        value={formData.userAccount}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="userPassword">Password:</label>
                    <input
                        type="password"
                        id="userPassword"
                        name="userPassword"
                        value={formData.userPassword}
                        onChange={handleChange}
                        required
                    />
                </div>
                {error && <p className="error">{error}</p>}
                <button type="submit" disabled={loading}>
                    {loading ? 'Logging in...' : 'Login'}
                </button>
            </form>
            <p>
                Don&apos;t have an account? <Link to="/register">Register here</Link>
            </p>
        </div>
    );
};

export default Login;