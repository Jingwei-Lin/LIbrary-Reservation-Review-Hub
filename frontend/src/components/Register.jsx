import './Register.css';
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerUser } from '../services/authApi';

const Register = (theme) => {
    const [formData, setFormData] = useState({
        userAccount: '',
        userPassword: '',
        checkPassword: '',
        firstName: '',
        lastName: '',
        phone: '',
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    // map backend messages to English (add more if needed)
    const errorMessages = {
        "参数为空": "No empty input allowed",
        "用户账号过短": "Too short account",
        "用户密码过短": "Too short password",
        "两次输入的密码不一致": "Inconsistent password",
        "账号重复": "Account already exists",
        "注册失败，数据库错误": "Registration failed caused by database error"

    };

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (formData.userPassword !== formData.checkPassword) {
            setError('Passwords do not match');
            return;
        }

        setLoading(true);

        try {
            await registerUser(formData);
            alert('Registration successful! Please log in.');
            navigate('/login');
        } catch (err) {
            const backendMessage = err.message;
            setError(errorMessages[backendMessage] || backendMessage || 'Registration failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={`page-container register-container ${theme === "dark" ? "dark-mode" : ""}`}>
            <h2>Register</h2>
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
                <div>
                    <label htmlFor="checkPassword">Confirm Password:</label>
                    <input
                        type="password"
                        id="checkPassword"
                        name="checkPassword"
                        value={formData.checkPassword}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="firstName">First Name:</label>
                    <input
                        type="text"
                        id="firstName"
                        name="firstName"
                        value={formData.firstName}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="lastName">Last Name:</label>
                    <input
                        type="text"
                        id="lastName"
                        name="lastName"
                        value={formData.lastName}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="phone">Phone:</label>
                    <input
                        type="tel"
                        id="phone"
                        name="phone"
                        value={formData.phone}
                        onChange={handleChange}
                        required
                    />
                </div>
                {error && <p className="error">{error}</p>}
                <button type="submit" disabled={loading}>
                    {loading ? 'Registering...' : 'Register'}
                </button>
            </form>
            <p>
                Already have an account? <Link to="/login">Login here</Link>
            </p>
        </div>
    );
};

export default Register;
