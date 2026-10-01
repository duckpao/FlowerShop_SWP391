import axios from 'axios';
import { authModel } from '../models/authModel';
import { API_BASE } from '../apiBase';
import { authService } from '../services/authService';

const axiosClient = axios.create({
  baseURL: `${API_BASE}/api`,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor — TODO: thêm Authorization header khi có JWT
axiosClient.interceptors.request.use(
  async (config) => {
    const token = authModel.getToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    if (!['get', 'head', 'options'].includes(config.method?.toLowerCase())) {
      const response = await fetch(`${API_BASE}/api/auth/csrf`, { credentials: 'include', cache: 'no-store' });
      if (!response.ok) throw new Error('Không thể tạo phiên bảo vệ yêu cầu.');
      const csrf = await response.json();
      config.headers[csrf.headerName] = csrf.token;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — xử lý lỗi chung
axiosClient.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    if (error.response?.status === 401 && !error.config?._retried) {
      error.config._retried = true;
      try {
        await authService.me();
        return axiosClient(error.config);
      } catch { /* The session has expired. */ }
    }
    const message =
      error.response?.data?.message ||
      error.message ||
      'Có lỗi xảy ra, vui lòng thử lại.';
    return Promise.reject(new Error(message));
  }
);

export default axiosClient;

