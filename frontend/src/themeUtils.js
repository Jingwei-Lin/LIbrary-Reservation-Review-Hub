export const getTheme = () => {
    return localStorage.getItem("theme") || "light";
};

export const toggleTheme = () => {
    const currentTheme = localStorage.getItem("theme") || "light";
    const newTheme = currentTheme === "light" ? "dark" : "light";
    localStorage.setItem("theme", newTheme);
    console.log(localStorage);
    return newTheme;
};