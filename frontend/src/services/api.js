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

export default api;