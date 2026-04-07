import axios from 'axios';
import { logoutUser } from './authApi';

const api = axios.create({
    baseURL: '/api',
    withCredentials: true,
});

export const reviewService = {
    /**
     * Get all reviews for a specific book
     * @param {number} bookId - The ID of the book
     * @returns {Promise<Array>} List of reviews
     */
    getReviewsByBookId: async (bookId) => {
        const response = await api.get(`/review/book/${bookId}`);
        console.log('getReviewsByBookId response:', response);
        return response.data;
    },

    /**
     * Add a new review for a specific book
     * @param {number} bookId - The ID of the book
     * @param {Object} reviewData - Review data { rating, comment }
     * @returns {Promise<number>} The ID of the created review
     */
    addReview: async (bookId, reviewData) => {
        const response = await api.post(`/review/add/${bookId}`, reviewData);
        console.log('addReview response:', response);
        if (response.data.code !== 0) {
            logoutUser();
            throw new Error(response.data.message || 'Failed to add review');
        }
        return response.data.data;
    },

    /**
     * Delete a specific review
     * @param {number} reviewId - The ID of the review
     * @returns {Promise<boolean>} True if deletion was successful
     */

    deleteReview: async (reviewId) => {
        const response = await api.delete(`/review/delete/${reviewId}`);
        console.log('deleteReview response:', response);
        if (response.data.code !== 0) {
            logoutUser();
            throw new Error(response.data.message || 'Failed to delete review');
        }
        return response.data.data;
    },

    /**
     * Update a specific review
     * @param {Object} reviewData - Review data { reviewId, rating, comment }
     * @returns {Promise<boolean>} True if update was successful
     */
    updateReview: async (reviewData) => {
        const response = await api.put('/review/update', reviewData);
        console.log('updateReview response:', response);
        if (response.data.code !== 0) {
            logoutUser();
            throw new Error(response.data.message || 'Failed to update review');
        }
        return response.data.data;
    },

    // 获取全站评论（分页，可选过滤）
    getAllReviews: async ({ current = 1, pageSize = 20, bookId, userId, minRating, maxRating } = {}) => {
        try {
            const params = new URLSearchParams();
            params.set('current', current);
            params.set('pageSize', pageSize);
            if (bookId != null) params.set('bookId', bookId);
            if (userId != null) params.set('userId', userId);
            if (minRating != null) params.set('minRating', minRating);
            if (maxRating != null) params.set('maxRating', maxRating);

            const response = await api.get(`/review/all?${params.toString()}`);
            console.log('getAllReviews response:', response);
            return response.data;
        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            console.error('Error fetching reviews:', error);
            throw new Error(error.response?.data?.message || 'Failed to fetch reviews');
        }
    }
};