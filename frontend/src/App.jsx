import React, { useContext } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Typography } from '@mui/material';

// --- Material UI y Temas ---
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './theme/theme.js';

// --- Contextos Globales ---
import { AuthProvider, AuthContext } from './context/AuthContext.jsx';
import { WebSocketProvider } from './context/WebSocketContext.jsx';

// --- Layouts ---
import MainLayout from './components/MainLayout.jsx';
// Importamos SoporteLayout por si en el futuro quieres separar el menú visualmente
import SoporteLayout from './components/SoporteLayout.jsx'; 

// --- Páginas Públicas / Seguridad ---
import LoginPage from './pages/LoginPage.jsx';
import PrimerCambioPassword from './pages/PrimerCambioPassword.jsx';

// --- Páginas de Administrador ---
import AdminUsuariosPage from './pages/admin/AdminUsuariosPage.jsx';
import AdminAreasPage from './pages/admin/AdminAreasPage.jsx';

// --- Páginas de Empleado / Soporte ---
import FormularioTicket from './pages/empleado/FormularioTicket.jsx';
import TicketsPage from './pages/tickets/TicketsPage.jsx';
import PanelSoporte from './pages/soporte/PanelSoporte.jsx';
// RECUPERADO DE TU CÓDIGO: El generador de PDF que tú hiciste
import GeneradorDocumentos from './components/GeneradorDocumentos.jsx';

// Página de prueba temporal
const DashboardPage = () => <Typography variant="h4">Dashboard General del Administrador</Typography>;

// ----------------------------------------------------------------------
// 1. COMPONENTE INTERNO: Manejo de Seguridad y Rutas
// ----------------------------------------------------------------------
function AppContent() {
    const { user } = useContext(AuthContext);

    // 1. SI NO ESTÁ LOGUEADO: Mandarlo al Login automáticamente
    if (!user) {
        return (
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        );
    }

    // 2. INTERCEPTOR CRÍTICO: Bloqueamos el resto si su clave es temporal
    if (user.passwordTemporal) {
        return (
            <Routes>
                <Route path="/primer-cambio" element={<PrimerCambioPassword />} />
                <Route path="*" element={<Navigate to="/primer-cambio" replace />} />
            </Routes>
        );
    }

    // 3. SI TODO ESTÁ BIEN Y ES UN USUARIO VÁLIDO: Acceso normal a la aplicación
    return (
        <MainLayout>
            <Routes>
                {/* Rutas de Admin */}
                <Route path="/admin/dashboard" element={<DashboardPage />} />
                <Route path="/admin/usuarios" element={<AdminUsuariosPage />} />
                <Route path="/admin/areas" element={<AdminAreasPage />} />
                
                {/* Rutas de Tickets Generales (Unificadas para que funcionen para todos) */}
                <Route path="/tickets/nuevo" element={<FormularioTicket />} />
                <Route path="/tickets" element={<TicketsPage />} />
                
                {/* Rutas específicas de Soporte (Recuperadas de tu versión) */}
                <Route path="/soporte/panel" element={<PanelSoporte />} />
                <Route path="/documentos" element={<GeneradorDocumentos />} />

                {/* Ruta comodín (Redirige al dashboard si escriben una URL que no existe) */}
                <Route path="*" element={<Navigate to="/admin/dashboard" replace />} />
            </Routes>
        </MainLayout>
    );
}

// ----------------------------------------------------------------------
// 2. COMPONENTE PRINCIPAL: Solo se encarga de encender los proveedores
// ----------------------------------------------------------------------
function App() {
    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <AuthProvider>
                <WebSocketProvider>
                    <Router>
                        <AppContent />
                    </Router>
                </WebSocketProvider>
            </AuthProvider>
        </ThemeProvider>
    );
}

export default App;