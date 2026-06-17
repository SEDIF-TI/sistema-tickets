import { createContext, useState, useEffect } from 'react';
import api from '../services/api';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Al recargar la página, recuperamos el usuario del localStorage
        const storedUser = localStorage.getItem('user');
        if (storedUser) setUser(JSON.parse(storedUser));
        setLoading(false);
    }, []);

    const login = async (identificador, password) => {
        const response = await api.post('/v1/auth/login', { identificador, password });
        const userData = response.data; // { id, nombre, rol, token, mensaje, vistas }
        
        // --- LA MAGIA SUCEDE AQUÍ ---
        // Si el login fue exitoso y nos mandó un token, lo abrimos para sacar la bandera
        if (userData.token) {
            try {
                // El JWT se divide en 3 partes separadas por puntos. La de en medio contiene nuestros datos.
                const payloadBase64 = userData.token.split('.')[1];
                // Desencriptamos el texto en base64 y lo convertimos a objeto JSON
                const decodedPayload = JSON.parse(atob(payloadBase64));
                
                // Le pegamos la bandera a nuestro usuario antes de guardarlo
                userData.passwordTemporal = decodedPayload.passwordTemporal;
            } catch (error) {
                console.error("Error decodificando el token JWT:", error);
            }
        }
        // -----------------------------

        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
        return userData;
    };

    const logout = () => {
        localStorage.removeItem('user');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};