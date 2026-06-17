import axios from 'axios';

// Creamos la instancia base apuntando a tu backend de Spring Boot
const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json'
    }
});

// Interceptor: Antes de que salga cualquier petición, revisa si hay un token guardado
api.interceptors.request.use(
    (config) => {
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
            const { token } = JSON.parse(storedUser);
            if (token) {
                // Le agregamos la palabra Bearer, tal como lo espera tu JwtAuthenticationFilter en Java
                config.headers.Authorization = `Bearer ${token}`;
            }
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Interceptor de respuestas: Atrapa los errores del servidor antes de que rompan a React
api.interceptors.response.use(
    (response) => response, // Si la respuesta es exitosa (200), pasa sin tocarla
    (error) => {
        // Verificamos si el servidor respondió con 401 (No autorizado) o 403 (Prohibido)
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            console.warn("La sesión ha expirado o es inválida. Redirigiendo al login...");
            
            // Borramos la sesión corrupta o vieja del navegador
            localStorage.removeItem('user');
            
            // Forzamos la redirección limpia a la raíz (Login)
            window.location.href = '/';
        }
        
        return Promise.reject(error);
    }
);

export default api;