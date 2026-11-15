import axios from 'axios';
import { authModel } from '../models/authModel';
import { API_BASE } from '../apiBase';
import { authService } from '../services/authService';
import { fetchCsrf } from './csrf';

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
      const csrf = await fetchCsrf();
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
        await authService.refresh();
        const token = authModel.getToken();
        if (token) {
          error.config.headers.Authorization = `Bearer ${token}`;
        }
        return axiosClient(error.config);
      } catch {
        authModel.clear();
      }
    }
    // 403 do CSRF không có thông báo trong thân: token lệch phiên, lấy token mới và gửi lại đúng một lần
    if (error.response?.status === 403 && !error.response?.data?.message && !error.config?._csrfRetried) {
      error.config._csrfRetried = true;
      return axiosClient(error.config);
    }
    const message =
      error.response?.data?.message ||
      (error.response?.status === 401 ? 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.' : error.message) ||
      'Có lỗi xảy ra, vui lòng thử lại.';
    return Promise.reject(new Error(message));
  }
);

export default axiosClient;

