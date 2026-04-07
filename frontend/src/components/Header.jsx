import React, { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { logoutUser } from "../services/authApi";
import { getTheme, toggleTheme } from "../themeUtils";
import "./Header.css";

const Header = ({ user, setIsAuthenticated, isAuthenticated, searchTerm, setSearchTerm, handleSearch }) => {
    const [isPanelOpen, setIsPanelOpen] = useState(false);
    const [theme, setTheme] = useState(getTheme());
    const panelRef = useRef(null);
    const navigate = useNavigate();

    useEffect(() => {
        // Apply theme class to header's parent or body
        document.body.classList.remove("light", "dark");
        document.body.classList.add(theme);
    }, [theme]);

    const handleToggleTheme = () => {
        const newTheme = toggleTheme();
        setTheme(newTheme);
        // Dispatch custom event for in-page sync
        window.dispatchEvent(new CustomEvent('themeChange', { detail: { theme: newTheme } }));
    };

    const handleLogout = async () => {
        try {
            await logoutUser(); // Now awaits backend
        } catch (err) {
            // Optional: show toast
        } finally {
            setIsAuthenticated(false);
            setIsPanelOpen(false);
        }
    };

    const handleLoginRedirect = () => {
        navigate("/login");
    };

    const handleProfileRedirect = () => {
        navigate("/profile");
        setIsPanelOpen(false);
    };

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (panelRef.current && !panelRef.current.contains(event.target)) {
                setIsPanelOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, []);

    const handleSearchSubmit = (e) => {
        e.preventDefault();
        handleSearch(e);
        // The URL will be updated by the Dashboard component
    };

    return (
        <header className={`header ${theme === "dark" ? "dark-mode" : ""}`}>
            <form className="search-bar" onSubmit={handleSearchSubmit}>
                <input
                    type="text"
                    placeholder="Search books, authors, categories..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                />
                <button type="submit">Search</button>
                <button
                    type="button"
                    onClick={() => {
                        setSearchTerm("");
                        // This will trigger the URL update via useEffect
                    }}
                    className="clear-btn"
                >
                    Clear
                </button>
            </form>
            <div className="user-wrapper" ref={panelRef}>
                <div className="dark-mode-toggle" onClick={handleToggleTheme}>
                    {theme === "dark" ? "🌞" : "🌙"}
                </div>
                {isAuthenticated ? (
                    <>
                        <div className="user-icon" onClick={() => setIsPanelOpen(!isPanelOpen)}>
                            👤
                        </div>
                        <div className={`user-panel ${isPanelOpen ? "open" : ""}`}>
                            <h3>User Panel</h3>
                            <p>{user?.firstName} {user?.lastName}</p>
                            <p>Email: {user?.userAccount}</p>
                            <p>UserID: {user?.id}</p>
                            <button onClick={handleProfileRedirect}>Profile</button>
                            <button onClick={handleLogout}>Logout</button>
                        </div>
                    </>
                ) : (
                    <button className="login-button" onClick={handleLoginRedirect}>
                        Login
                    </button>
                )}
            </div>
        </header>
    );
};

export default Header;