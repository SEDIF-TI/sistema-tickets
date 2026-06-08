import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './theme/theme.js';
import { AuthProvider } from './context/AuthContext.jsx';
import { WebSocketProvider } from './context/WebSocketContext.jsx';
import LoginPage from './pages/LoginPage.jsx';
import MainLayout from './components/MainLayout.jsx';
import { Typography } from '@mui/material';
import FormularioTicket from './pages/empleado/FormularioTicket';
import TicketsPage from './pages/tickets/TicketsPage';

// Páginas de prueba temporales para verificar que las rutas funcionan
const LevantarTicket = () => <Typography variant="h4">Formulario: Crear Nuevo Ticket</Typography>;
const MisTickets = () => <Typography variant="h4">Bandeja: Tickets por Atender</Typography>;
const Dashboard = () => <Typography variant="h4">Dashboard General del Administrador</Typography>;

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router> {/* Router afuera de los proveedores */}
        <AuthProvider>
          <WebSocketProvider>
            <Routes>
              {/* Ruta pública del Login */}
              <Route path="/login" element={<LoginPage />} />

              {/* Rutas Protegidas (Solo dejamos las que funcionan) */}
              <Route path="/empleado/nuevo" element={<MainLayout><FormularioTicket /></MainLayout>} />
              <Route path="/empleado/historial" element={<MainLayout><TicketsPage /></MainLayout>} />
              <Route path="/soporte/bandeja" element={<MainLayout><MisTickets /></MainLayout>} />
              <Route path="/admin/dashboard" element={<MainLayout><Dashboard /></MainLayout>} />

              {/* Redirección por defecto */}
              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
          </WebSocketProvider>
        </AuthProvider>
      </Router>
    </ThemeProvider>
  );
}

export default App;