// App.jsx

import './App.css';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Login from './components/Login.jsx';
import Register from './components/Register.jsx';
import Dashboard from './components/Dashboard.jsx';
import AdminLogin from './components/AdminLogin.jsx';
import AdminDashboard from './components/AdminDashboard.jsx';
import Reservations from './components/Reservations.jsx';
import Book from './components/Book';
import { validateSession, logoutUser } from './services/authApi';
import ReservationForm from "./components/ReservationForm.jsx";
import WaitingList from "./components/WaitingList.jsx";
import Profile from "./components/Profile.jsx";

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(() => !!sessionStorage.getItem("user"));
    const [user, setUser] = useState(() => JSON.parse(sessionStorage.getItem("user") || '{}'));

    // Validate session on mount
    useEffect(() => {
        const checkSession = async () => {
            try {
                const response = await validateSession(); // Always call
                if (response && response.code === 0 && response.data) {
                    // Session valid → restore from backend
                    const userData = response.data;
                    sessionStorage.setItem('user', JSON.stringify(userData));
                    setUser(userData);
                    setIsAuthenticated(true);
                } else {
                    // Invalid → ensure clean state
                    logoutUser();
                    setIsAuthenticated(false);
                    setUser({});
                }
            } catch (error) {
                console.error('Session validation failed:', error);
                logoutUser();
                setIsAuthenticated(false);
                setUser({});
            }
        };
        checkSession();
    }, []);

    return (
        <Router>
            <div className="App">
                <Routes>
                    <Route
                        path="/dashboard"
                        element={
                            <Dashboard
                                user={user}
                                setUser={setUser}
                                setIsAuthenticated={setIsAuthenticated}
                                isAuthenticated={isAuthenticated}
                            />
                        }
                    />
                    <Route
                        path="/login"
                        element={
                            !isAuthenticated ? (
                                <Login
                                    setIsAuthenticated={setIsAuthenticated}
                                    setUser={setUser}
                                />
                            ) : (
                                <Navigate to="/dashboard" />
                            )
                        }
                    />
                    <Route
                        path="/register"
                        element={
                            !isAuthenticated ? (
                                <Register />
                            ) : (
                                <Navigate to="/dashboard" />
                            )
                        }
                    />
                    {/*<Route*/}
                    {/*    path="/book/:id"*/}
                    {/*    element={<Book isAuthenticated={isAuthenticated} />}*/}
                    {/*/>*/}
                    <Route path="/" element={<Navigate to="/dashboard" />} />
                    <Route
                        path="/reservations"
                        element={<Reservations isAuthenticated={isAuthenticated} />}
                    />
                    {/*<Route path="/reservation/form/:bookId" element={<ReservationForm isAuthenticated={isAuthenticated} setIsAuthenticated={setIsAuthenticated} />} />*/}
                    <Route path="/waitinglist" element={<WaitingList isAuthenticated={isAuthenticated} setIsAuthenticated={setIsAuthenticated} />} />
                    <Route path="/profile" element={<Profile user={user} setIsAuthenticated={setIsAuthenticated} />} />
                    <Route path="/admin/login" element={<AdminLogin setIsAuthenticated={setIsAuthenticated} setUser={setUser} />} />
                    <Route path="/admin/dashboard" element={<AdminDashboard user={user} isAuthenticated={isAuthenticated} setIsAuthenticated={setIsAuthenticated} />} />
                    <Route path="/admin" element={<Navigate to="/admin/login" />} />
                    <Route path="/" element={<Navigate to="/dashboard" />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;