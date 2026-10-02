// Vite dev server (5173) doesn't run the backend, so dev builds call it directly at 8080.
// Production is served by Spring Boot itself (same origin), so no prefix is needed there.
// Các service có ghép API_BASE gọi BE 8080 ở dev; đường dẫn tương đối trong catalogService/reviewService không dùng biến này.
export const API_BASE = import.meta.env.DEV ? 'http://localhost:8080' : ''
