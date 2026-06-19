import React, { useContext } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Typography } from '@mui/material';

// --- Material UI y Temas ---
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './theme/theme.js';

// Contextos
import { AuthContext, AuthProvider } from './context/AuthContext.jsx';
import { WebSocketProvider } from './context/WebSocketContext.jsx';

// Layouts y Páginas
import LoginPage from './pages/LoginPage.jsx';
import MainLayout from './components/MainLayout.jsx';
import SoporteLayout from './components/SoporteLayout.jsx';
import FormularioTicket from './pages/empleado/FormularioTicket';
import TicketsPage from './pages/tickets/TicketsPage';
import PanelSoporte from './pages/soporte/PanelSoporte.jsx';
import AdminUsuariosPage from './pages/admin/AdminUsuariosPage.jsx';
import AdminAreasPage from './pages/admin/AdminAreasPage';
import PerfilPage from './pages/PerfilPage.jsx';
import DashboardPage from './pages/admin/DashboardPage.jsx';

// --- IMPORTACIÓN NUEVA (Asegúrate de que la ruta de la carpeta sea correcta) ---
import GeneradorDocumentos from './components/GeneradorDocumentos.jsx'; 

// 1. EL POLICÍA DE TRÁNSITO (AppContent)
function AppContent() {
    const { user } = useContext(AuthContext);

    if (!user) {
        return (
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        );
    }

    const userRole = user.rol || user.role || user.rolNombre || '';
    const cleanRole = userRole.replace('ROLE_', '').toUpperCase();

    let rutaPorDefecto = '/empleado/historial'; 
    if (cleanRole === 'ADMINISTRADOR') rutaPorDefecto = '/admin/dashboard';
    if (cleanRole === 'SOPORTE') rutaPorDefecto = '/soporte/bandeja';

    return (
        <Routes>
            <Route path="/" element={<Navigate to={rutaPorDefecto} replace />} />
            <Route path="/login" element={<Navigate to={rutaPorDefecto} replace />} />

            {/* Rutas Protegidas de Admin */}
            <Route path="/admin/dashboard" element={<MainLayout><DashboardPage /></MainLayout>} />
            <Route path="/admin/usuarios" element={<MainLayout><AdminUsuariosPage /></MainLayout>} />
            <Route path="/admin/areas" element={<MainLayout><AdminAreasPage /></MainLayout>} />

            {/* Rutas Protegidas de Soporte */}
            <Route path="/soporte/bandeja" element={<SoporteLayout><PanelSoporte /></SoporteLayout>} />
            
            {/* CORRECCIÓN: Ajustamos esta ruta para que coincida con lo que tu compañero guardó en la BD */}
            <Route path="/tickets/nuevo" element={<SoporteLayout><FormularioTicket /></SoporteLayout>} /> 
            
            {/* NUEVA RUTA: Agregamos la ruta del generador de documentos dentro del layout de soporte */}
            <Route path="/documentos/crear" element={<SoporteLayout><GeneradorDocumentos /></SoporteLayout>} />

            {/* Rutas Protegidas de Empleado */}
            <Route path="/empleado/nuevo" element={<MainLayout><FormularioTicket /></MainLayout>} />
            <Route path="/empleado/historial" element={<MainLayout><TicketsPage /></MainLayout>} />

            {/* Ruta del Nuevo Perfil */}
            <Route path="/perfil" element={<MainLayout><PerfilPage /></MainLayout>} />

            {/* Cualquier otra ruta inventada lo regresa a su panel */}
            <Route path="*" element={<Navigate to={rutaPorDefecto} replace />} />
        </Routes>
    );
}

// 2. LA ESTRUCTURA PRINCIPAL
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