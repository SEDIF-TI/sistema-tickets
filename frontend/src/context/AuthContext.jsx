import { createContext, useState, useEffect } from 'react';
import api from '../services/api';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const storedUser = localStorage.getItem('user');
        if (storedUser) setUser(JSON.parse(storedUser));
        setLoading(false);
    }, []);

    const login = async (identificador, password) => {
        const response = await api.post('/v1/auth/login', { identificador, password });
        const userData = response.data; // Contiene: id, nombre, rolNombre, tokenJwt, mensaje, vistasPermitidas, areaId
        
        // --- DECODIFICADOR DE TOKENS JWT ---
        if (userData.tokenJwt) {
            try {
                const payloadBase64 = userData.tokenJwt.split('.')[1];
                const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
                const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));
                
                const decodedPayload = JSON.parse(jsonPayload);
                userData.passwordTemporal = decodedPayload.passwordTemporal || false;
            } catch (error) {
                console.error("[AuthContext] Error al decodificar el token JWT:", error);
                userData.passwordTemporal = false; 
            }
        }

        // Almacenamos el objeto completo incluyendo la propiedad areaId
        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
        return userData;
    };

    const marcarPasswordCambiada = () => {
        if (user) {
            const usuarioActualizado = { ...user, passwordTemporal: false };
            localStorage.setItem('user', JSON.stringify(usuarioActualizado));
            setUser(usuarioActualizado);
        }
    };

    const logout = () => {
        localStorage.removeItem('user');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading, marcarPasswordCambiada }}>
            {children}
        </AuthContext.Provider>
    );
};