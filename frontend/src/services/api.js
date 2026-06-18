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

export default api;