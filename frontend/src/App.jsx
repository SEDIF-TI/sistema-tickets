import { useContext } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

// --- Material UI y Temas ---
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './theme/theme.js';
// Contextos
import { AuthContext, AuthProvider } from './context/AuthContext.jsx';
import { NotificationProvider } from './context/NotificationContext.jsx';
import { WebSocketProvider } from './context/WebSocketContext.jsx';

// Layouts y Páginas Base
import LoginPage from './pages/LoginPage.jsx';
import MainLayout from './components/MainLayout.jsx';
import PerfilPage from './pages/PerfilPage.jsx';

// Páginas de Roles
import FormularioTicket from './pages/empleado/FormularioTicket';
import TicketsPage from './pages/tickets/TicketsPage';
import PanelSoporte from './pages/soporte/PanelSoporte.jsx';
import AdminUsuariosPage from './pages/admin/AdminUsuariosPage.jsx';
import AdminAvisosPage from './pages/admin/AdminAvisosPage.jsx';
import AdminAreasPage from './pages/admin/AdminAreasPage';
import DashboardPage from './pages/admin/DashboardPage.jsx';
import AdminEquiposPage from './pages/admin/AdminEquiposPage.jsx';
import GestionTaller from './pages/soporte/GestionTaller.jsx';
import GestionResguardos from './pages/soporte/GestionResguardos.jsx';
import HistorialResguardos from './pages/admin/HistorialResguardos.jsx';

// Componentes Adicionales
import GeneradorDocumentos from './components/GeneradorDocumentos.jsx'; 

import '@fontsource/inter/400.css';
import '@fontsource/inter/600.css';
import '@fontsource/inter/700.css';

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

    if (user.passwordTemporal) {
        return (
            <Routes>
                <Route path="/perfil" element={<MainLayout><PerfilPage /></MainLayout>} />
                <Route path="*" element={<Navigate to="/perfil" replace />} />
            </Routes>
        );
    }

    // REDIRECCIÓN DINÁMICA: La ruta inicial por defecto es la primera vista asignada en la BD
    const tieneVistas = user.vistasPermitidas && user.vistasPermitidas.length > 0;
    const rutaPorDefecto = tieneVistas ? user.vistasPermitidas[0].ruta : '/perfil';

    return (
        <Routes>
            <Route path="/" element={<Navigate to={rutaPorDefecto} replace />} />
            <Route path="/login" element={<Navigate to={rutaPorDefecto} replace />} />

            {/* Rutas de Administrador */}
            <Route path="/admin/dashboard" element={<MainLayout><DashboardPage /></MainLayout>} />
            <Route path="/admin/usuarios" element={<MainLayout><AdminUsuariosPage /></MainLayout>} />
            <Route path="/admin/areas" element={<MainLayout><AdminAreasPage /></MainLayout>} />
            <Route path="/admin/avisos" element={<MainLayout><AdminAvisosPage /></MainLayout>} />
            <Route path="/admin/bitacora" element={<MainLayout><TicketsPage /></MainLayout>} />
            <Route path="/admin/equipos" element={<MainLayout><AdminEquiposPage /></MainLayout>} />
            {/* Vista de seguimiento: el alta y las devoluciones viven en
                /soporte/resguardos. La pantalla existia sin ruta que la
                alcanzara, asi que era codigo inalcanzable. */}
            <Route path="/admin/resguardos" element={<MainLayout><HistorialResguardos /></MainLayout>} />

            {/* Rutas de Soporte / Documentos */}
            <Route path="/soporte/bandeja" element={<MainLayout><PanelSoporte /></MainLayout>} />
            <Route path="/soporte/taller" element={<MainLayout><GestionTaller /></MainLayout>} />
            <Route path="/soporte/resguardos" element={<MainLayout><GestionResguardos /></MainLayout>} />
            <Route path="/documentos/crear" element={<MainLayout><GeneradorDocumentos /></MainLayout>} />

            {/* Ruta Compartida de Tickets */}
            <Route path="/tickets/nuevo" element={<MainLayout><FormularioTicket /></MainLayout>} /> 

            {/* Rutas de Empleado */}
            <Route path="/empleado/nuevo" element={<MainLayout><FormularioTicket /></MainLayout>} />
            <Route path="/empleado/historial" element={<MainLayout><TicketsPage /></MainLayout>} />

            {/* Perfil */}
            <Route path="/perfil" element={<MainLayout><PerfilPage /></MainLayout>} />

            {/* Cualquier otra ruta no registrada activa el rebote seguro */}
            <Route path="*" element={<Navigate to={rutaPorDefecto} replace />} />
        </Routes>
    );
}

export default function App() {
    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
            {/* NotificationProvider envuelve al resto para que cualquier
                pantalla pueda lanzar avisos con useNotification(), en lugar
                de montar su propio Snackbar o usar alert() nativo. */}
            <NotificationProvider>
                <AuthProvider>
                    <WebSocketProvider>
                        <Router>
                            <AppContent />
                        </Router>
                    </WebSocketProvider>
                </AuthProvider>
            </NotificationProvider>
        </ThemeProvider>
    );
}