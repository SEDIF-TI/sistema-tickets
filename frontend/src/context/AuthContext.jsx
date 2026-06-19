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
        const userData = response.data; // { id, nombre, rol, token, mensaje, vistas }
        
        // --- DECODIFICADOR DE TOKENS A PRUEBA DE BALAS ---
        if (userData.token) {
            try {
                const payloadBase64 = userData.token.split('.')[1];
                
                // 1. Convertimos el formato Base64URL a Base64 estándar
                const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
                
                // 2. Decodificamos soportando caracteres multi-byte (acentos y eñes)
                const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                }).join(''));
                
                const decodedPayload = JSON.parse(jsonPayload);
                
                // Asignamos la bandera de manera segura
                userData.passwordTemporal = decodedPayload.passwordTemporal || false;
                
                console.log("[AuthContext] Token decodificado con éxito. ¿Es temporal?:", userData.passwordTemporal);
            } catch (error) {
                console.error("[AuthContext] Error crítico al decodificar el token JWT:", error);
                userData.passwordTemporal = false; // Fallback seguro
            }
        }
        // -------------------------------------------------

        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
        return userData;
    };

    // --- NUEVA FUNCIÓN: Actualiza el estado en caliente ---
    const marcarPasswordCambiada = () => {
        if (user) {
            const usuarioActualizado = { ...user, passwordTemporal: false };
            localStorage.setItem('user', JSON.stringify(usuarioActualizado));
            setUser(usuarioActualizado); // Esto romperá el bloqueo en App.jsx al instante
        }
    };

    const logout = () => {
        localStorage.removeItem('user');
        setUser(null);
    };

    return (
        // Agregamos marcarPasswordCambiada aquí abajo:
        <AuthContext.Provider value={{ user, login, logout, loading, marcarPasswordCambiada }}>
            {children}
        </AuthContext.Provider>
    );
};