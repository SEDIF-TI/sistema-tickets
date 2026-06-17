import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json'
    }
});

api.interceptors.request.use(
    (config) => {
        // 1. Buscamos tanto en 'user' como en 'token' directo por si acaso
        const storedUser = localStorage.getItem('user');
        const storedToken = localStorage.getItem('token'); 
        
        let tokenFinal = null;

        if (storedUser) {
            try {
                const parsedUser = JSON.parse(storedUser);
                // A veces se llama 'token', a veces 'jwt', sacamos el que exista
                tokenFinal = parsedUser.token || parsedUser.jwt; 
            } catch (e) {
                console.error("Error parseando el usuario del localStorage", e);
            }
        }

        // Si no estaba adentro de 'user', usamos el que está suelto
        if (!tokenFinal && storedToken) {
            tokenFinal = storedToken;
        }

        // 2. Si lo encontramos, lo inyectamos
        if (tokenFinal) {
            config.headers['Authorization'] = `Bearer ${tokenFinal}`;
        }

        // 3. CHIVATO EN CONSOLA (Para que veas qué está pasando)
        console.log(`[Axios] Petición: ${config.method.toUpperCase()} ${config.url}`);
        console.log(`[Axios] ¿Se inyectó el token?: ${tokenFinal ? '✅ SÍ' : '❌ NO (Va vacío)'}`);

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