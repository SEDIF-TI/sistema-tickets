import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './theme/theme.js';
import { AuthProvider } from './context/AuthContext.jsx';
import LoginPage from './pages/LoginPage.jsx';

function App() {
  return (
    // ThemeProvider inyecta los colores a todo Material UI
    <ThemeProvider theme={theme}>
      {/* CssBaseline limpia márgenes e inyecta el color de fondo general */}
      <CssBaseline /> 
      
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
      
    </ThemeProvider>
  );
}

export default App;