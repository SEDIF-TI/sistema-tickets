import { useContext } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './theme/theme.js';

import { AuthContext, AuthProvider } from './context/AuthContext.jsx';
import { NotificationProvider } from './context/NotificationContext.jsx';
import { WebSocketProvider } from './context/WebSocketContext.jsx';

import LoginPage from './pages/LoginPage.jsx';
import MainLayout from './components/MainLayout.jsx';
import PerfilPage from './pages/PerfilPage.jsx';

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
import HistorialPage from './pages/admin/HistorialPage.jsx';

import GeneradorDocumentos from './components/GeneradorDocumentos.jsx';

import '@fontsource/inter/400.css';
import '@fontsource/inter/600.css';
import '@fontsource/inter/700.css';

function AppContent() {
    const { user } = useContext(AuthContext);

    // Sin sesión solo existe el login: cualquier otra ruta rebota allí.
    if (!user) {
        return (
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        );
    }

    // Con contraseña temporal el único destino es el perfil, donde se cambia.
    // El árbol de rutas normal ni siquiera se declara, así que no hay forma de
    // alcanzar el resto de la aplicación escribiendo la URL a mano.
    if (user.passwordTemporal) {
        return (
            <Routes>
                <Route path="/perfil" element={<MainLayout><PerfilPage /></MainLayout>} />
                <Route path="*" element={<Navigate to="/perfil" replace />} />
            </Routes>
        );
    }

    // La ruta de entrada es la primera vista que el rol tiene concedida en la
    // base de datos, de modo que cada perfil aterriza en la pantalla que le
    // corresponde sin rutas fijas por rol en el código.
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
            <Route path="/admin/equipos" element={<MainLayout><AdminEquiposPage /></MainLayout>} />

            {/* Historial unificado: dictámenes, resguardos y tickets en
                pestañas bajo una sola entrada de menú.

                Las dos rutas siguientes no figuran en el menú y se mantienen
                declaradas para que un enlace guardado siga resolviendo. */}
            <Route path="/historial" element={<MainLayout><HistorialPage /></MainLayout>} />
            <Route path="/admin/bitacora" element={<MainLayout><TicketsPage /></MainLayout>} />
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
            {/* El orden de los proveedores importa: NotificationProvider queda
                por fuera para que cualquier pantalla lance avisos con
                useNotification(), y WebSocketProvider por dentro de
                AuthProvider porque la suscripción STOMP necesita la sesión. */}
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