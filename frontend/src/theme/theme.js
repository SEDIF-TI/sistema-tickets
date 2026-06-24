import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  // 1. PALETA DE COLORES
  palette: {
    primary: {
      main: '#db2777', // Primario
      light: '#fc90b4', // Primario light (rosa logo)
      dark: '#be185d', // Primario dark
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#0f172a', // Secundario (escala slate)
      contrastText: '#ffffff',
    },
    success: {
      main: '#16a34a',
      light: '#86efac',
      dark: '#15803d',
      contrastText: '#ffffff',
    },
    error: {
      main: '#dc2626',
      light: '#fca5a5',
      dark: '#b91c1c',
      contrastText: '#ffffff',
    },
    warning: {
      main: '#d97706',
      light: '#fcd34d',
      dark: '#b45309',
      contrastText: '#ffffff',
    },
    info: {
      main: '#0369a1',
      light: '#7dd3fc',
      dark: '#075985',
      contrastText: '#ffffff',
    },
    text: {
      primary: '#0f172a',
      secondary: '#64748b',
    },
    background: {
      default: '#f8fafc',
      paper: '#ffffff',
    },
    divider: '#e2e8f0',
  },

  // 1.1 TOKENS CUSTOM
  brand: {
    rosaLogo: '#fc90b4',
    rosaOnda: '#fc6c9c',
    gradiente: 'linear-gradient(135deg, #fc90b4 0%, #fc6c9c 45%, #db2777 100%)',
    gradienteSuave: 'linear-gradient(135deg, #fde7ef 0%, #fbcfe0 100%)',
  },

  // 2. TIPOGRAFÍA
  typography: {
    fontFamily: '"Inter", "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
    fontSize: 16,
    h1: { fontSize: '2.5rem', fontWeight: 700, lineHeight: 1.2 },
    h2: { fontSize: '2rem', fontWeight: 700, lineHeight: 1.25 },
    h3: { fontSize: '1.75rem', fontWeight: 700, lineHeight: 1.25 },
    h4: { fontSize: '1.5rem', fontWeight: 700, lineHeight: 1.3 },
    h5: { fontSize: '1.25rem', fontWeight: 700, lineHeight: 1.3 },
    h6: { fontSize: '1.125rem', fontWeight: 700, lineHeight: 1.4 },
    subtitle1: { fontWeight: 600, lineHeight: 1.5 },
    subtitle2: { fontWeight: 600, lineHeight: 1.5 },
    body1: { fontSize: '1rem', fontWeight: 400, lineHeight: 1.6 },
    body2: { fontSize: '0.875rem', fontWeight: 400, lineHeight: 1.5 },
    caption: { fontSize: '0.75rem', fontWeight: 400, lineHeight: 1.4 },
    button: { 
      fontWeight: 600, 
      textTransform: 'none'
    },
  },

  // 3. FORMA (Shape)
  shape: {
    borderRadius: 8,
  },

  // 4. OVERRIDES DE COMPONENTES
  components: {
    MuiCssBaseline: {
      styleOverrides: `
        :focus-visible {
          outline: 2px solid #db2777;
          outline-offset: 2px;
        }
        @media (prefers-reduced-motion: reduce) {
          * {
            animation-duration: 0.01ms !important;
            transition-duration: 0.01ms !important;
          }
        }
      `,
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          borderRadius: 8,
          minHeight: 40,
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          boxShadow: '0px 2px 8px rgba(15, 23, 42, 0.08)',
          transition: 'box-shadow 150ms ease-in-out',
        },
      },
    },
    MuiDialog: {
      styleOverrides: { paper: { borderRadius: 18 } },
    },
    MuiTooltip: {
      styleOverrides: { tooltip: { borderRadius: 8 } },
    },
    MuiIconButton: {
      styleOverrides: {
        root: { minHeight: 40, minWidth: 40 }
      }
    },

    // ... dentro de components:
   MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: '8px !important',
          backgroundImage: 'linear-gradient(135deg, #fde7ef 0%, #fbcfe0 100%) !important',
          color: '#801A36 !important',
          fontWeight: 'bold',
          border: '1px solid #fc90b4',
          '& .MuiAlert-icon': {
            color: '#801A36 !important',
          },
        },
      },
    },
  },
});

export default theme;