// Vite dev server (5173) doesn't run the backend, so dev builds call it directly at 8080.
// Production is served by Spring Boot itself (same origin), so no prefix is needed there.
export const API_BASE = import.meta.env.DEV ? 'http://localhost:8080' : ''
