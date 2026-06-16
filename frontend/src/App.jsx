import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './theme/theme.js';
import { AuthProvider } from './context/AuthContext.jsx';
import { WebSocketProvider } from './context/WebSocketContext.jsx';
import LoginPage from './pages/LoginPage.jsx';
import MainLayout from './components/MainLayout.jsx';
import SoporteLayout from './components/SoporteLayout.jsx';

// Tus componentes de páginas
import FormularioTicket from './pages/empleado/FormularioTicket';
import TicketsPage from './pages/tickets/TicketsPage';
import PanelSoporte from './pages/soporte/PanelSoporte.jsx';
import GeneradorDocumentos from './components/GeneradorDocumentos.jsx';
import AdminUsuariosPage from './pages/admin/AdminUsuariosPage.jsx';
import adminAreasPage from './pages/admin/AdminAreasPage.jsx';
import { Typography } from '@mui/material';

// Páginas de prueba temporales (puedes reemplazarlas por tus componentes reales)
const Dashboard = () => <Typography variant="h4">Dashboard General del Administrador</Typography>;

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {/* Proveedores globales primero */}
      <AuthProvider>
        <WebSocketProvider>
          {/* Router adentro para que los contextos sean accesibles */}
          <Router>
            <Routes>
              {/* Ruta pública */}
              <Route path="/login" element={<LoginPage />} />

              {/* Rutas protegidas bajo MainLayout */}
              <Route path="/empleado/nuevo" element={<MainLayout><FormularioTicket /></MainLayout>} />
              <Route path="/empleado/historial" element={<MainLayout><TicketsPage /></MainLayout>} />
              <Route path="/admin/dashboard" element={<MainLayout><Dashboard /></MainLayout>} />
              <Route path="/admin/usuarios" element={<MainLayout><AdminUsuariosPage /></MainLayout>} />

              {/* Rutas protegidas bajo SoporteLayout */}
              <Route path="/soporte/bandeja" element={<SoporteLayout><PanelSoporte /></SoporteLayout>} />
              <Route path="/soporte/nuevo" element={<SoporteLayout><FormularioTicket /></SoporteLayout>} />
              <Route path="/documentos" element={<SoporteLayout><GeneradorDocumentos /></SoporteLayout>} />

              {/* Redirección por defecto */}
              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
          </Router>
        </WebSocketProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;