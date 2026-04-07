import axios from 'axios';
import { logoutUser } from './authApi';

const api = axios.create({
    baseURL: '/api',
    withCredentials: true,
});

const getAuthHeader = () => {
    const user = JSON.parse(sessionStorage.getItem('user') || '{}');
    return user?.token ? { Authorization: `Bearer ${user.token}` } : {};
};

export const waitingListService = {
    getMyWaitingList: async () => {
        try {
            const response = await api.get('/waiting-list/my', { headers: getAuthHeader() });
            console.log('getMyWaitingList response:', response.data);
            return response.data;
        } catch (error) {
            console.error('getMyWaitingList error:', error.response?.data, error.message);
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to load waiting list');
        }
    },

    addToWaitingList: async (bookId) => {
        try {
            const response = await api.post('/waiting-list/my/add', { bookId }, { headers: getAuthHeader() });
            console.log('addToWaitingList response:', response.data);
            return response.data;
        } catch (error) {
            console.error('addToWaitingList error:', error.response?.data, error.message);
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to add to waiting list');
        }
    },

    cancelWaiting: async (bookId) => {
        try {
            const response = await api.delete(`/waiting-list/my/${bookId}`, { headers: getAuthHeader() });
            console.log('cancelWaiting response:', response.data);
            return response.data;
        } catch (error) {
            console.error('cancelWaiting error:', error.response?.data, error.message);
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to cancel waiting list entry');
        }
    },

    getEligibility: async () => {
        try {
            const response = await api.get('/waiting-list/my/eligibility', { headers: getAuthHeader() });
            console.log('getEligibility response:', response.data);
            return response.data;
        } catch (error) {
            console.error('getEligibility error:', error.response?.data, error.message);
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to check eligibility');
        }
    },

    // 管理员按用户ID查看其所有等待记录（包含 CANCELLED）
    getByUserAdmin: async (userId) => {
        try {
            const response = await api.get(`/waiting-list/user/${userId}`, { headers: getAuthHeader() });
            console.log('getByUserAdmin response:', response.data);
            return response.data;
        } catch (error) {
            console.error('getByUserAdmin error:', error.response?.data, error.message);
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to load user waiting list');
        }
    },
};