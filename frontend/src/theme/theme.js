import { createTheme } from '@mui/material/styles';

const theme = createTheme({
    // 1. Definimos la paleta de colores institucionales
    palette: {
        primary: {
            main: '#5c0a28', // Color guinda/vino institucional
            dark: '#4a0820',
            contrastText: '#ffffff',
        },
        success: {
            main: '#ff7ccd',
            light: '#e8f5e9',
        },
        background: {
            default: '#f8fafc', // Fondo sutil para las pantallas
            paper: '#ffffff',
        },
    },
    // 2. Definimos estilos globales para componentes específicos
    components: {
        MuiButton: {
            styleOverrides: {
                root: {
                    textTransform: 'none', // Quita las mayúsculas forzadas
                    borderRadius: '8px',   // Bordes redondeados institucionales
                    fontWeight: 600,
                    padding: '8px 20px',
                },
            },
        },
        MuiTableHead: {
            styleOverrides: {
                root: {
                    backgroundColor: '#f8fafc', // Fondo grisáceo limpio para cabeceras
                },
            },
        },
        MuiTableCell: {
            styleOverrides: {
                head: {
                    fontWeight: 600,
                    color: '#475569',
                },
            },
        },
    },
});

export default theme;