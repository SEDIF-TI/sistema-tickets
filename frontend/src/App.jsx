import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './theme/theme.js';
import { AuthProvider } from './context/AuthContext.jsx';
import { WebSocketProvider } from './context/WebSocketContext.jsx';
import LoginPage from './pages/LoginPage.jsx';
import MainLayout from './components/MainLayout.jsx';
import { Typography } from '@mui/material';
// import FormularioTicket from './pages/empleado/FormularioTicket';
import PanelSoporte from './pages/soporte/PanelSoporte.jsx';

// Páginas de prueba temporales para verificar que las rutas funcionan
const LevantarTicket = () => <Typography variant="h4">Formulario: Crear Nuevo Ticket</Typography>;
const MisTickets = () => <Typography variant="h4">Bandeja: Tickets por Atender</Typography>;
const Dashboard = () => <Typography variant="h4">Dashboard General del Administrador</Typography>;

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <AuthProvider>
          <WebSocketProvider>
            <Routes>
              {/* Ruta pública del Login */}
              <Route path="/login" element={<LoginPage />} />

              {/* Rutas Protegidas envueltas en el MainLayout */}
              <Route path="/empleado/nuevo" element={<MainLayout><LevantarTicket /></MainLayout>} />
              <Route path="/soporte/bandeja" element={<MainLayout><MisTickets /></MainLayout>} />
              <Route path="/admin/dashboard" element={<MainLayout><Dashboard /></MainLayout>} />
              
              {/* Esta línea se conserva comentada para no afectar el código de su compañero */}
              {/* <Route path="/empleado/nuevo" element={<MainLayout><FormularioTicket /></MainLayout>} /> */}
              
              <Route path="/panel-soporte" element={<PanelSoporte />} />

              {/* Redirección por defecto si entran a una ruta que no existe */}
              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
          </WebSocketProvider>
        </AuthProvider>
      </Router>
    </ThemeProvider>
  );
}

export default App;