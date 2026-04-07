import axios from 'axios';

const api = axios.create({
    baseURL: '/api',
    withCredentials: true,
});

export const userService = {
    getUserProfile: async () => {
        try {
            const response = await api.get('/user/getProfile');
            console.log('getProfile response:', response);
            return response.data;
        } catch (error) {
            console.error('Error fetching user profile:', error);
            throw error;
        }
    },

    updateUserProfile: async (profileData) => {
        try {
            const response = await api.put('/user/updateProfile', profileData);
            console.log('updateProfile response:', response);
            return response.data;
        } catch (error) {
            console.error('Error updating user profile:', error);
            throw error;
        }
    },
};