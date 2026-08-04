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
        const userData = response.data; 
        
        // Mapeo de la lista de vistas del backend al formato que espera el frontend
        userData.vistasPermitidas = userData.vistas || [];
        
        // Lectura directa de la propiedad enviada desde el backend sin decodificar el JWT
        userData.passwordTemporal = userData.passwordTemporal || false;

        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
        return userData;
    };

    /**
     * Sustituye el menú guardado por el que el servidor considera vigente.
     *
     * Lo llama MainLayout al montarse: sin esto, el menú se congelaba en el
     * navegador desde el inicio de sesión.
     */
    const actualizarVistas = (vistas) => {
        setUser((actual) => {
            if (!actual) return actual;
            const actualizado = { ...actual, vistasPermitidas: vistas ?? [] };
            localStorage.setItem('user', JSON.stringify(actualizado));
            return actualizado;
        });
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
        <AuthContext.Provider value={{ user, login, logout, loading, marcarPasswordCambiada, actualizarVistas }}>
            {children}
        </AuthContext.Provider>
    );
};