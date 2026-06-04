import { createTheme } from '@mui/material/styles';

const theme = createTheme({
    palette: {
        // Color principal (Botones primarios, barras de navegación superior)
        primary: {
            main: '#611232', // Guinda institucional (Cambia este valor)
            light: '#8f2f53',
            dark: '#3d0a1f',
            contrastText: '#ffffff', // Color del texto sobre el color primario
        },
        // Color secundario (Botones secundarios, iconos destacados)
        secondary: {
            main: '#bda674', // Dorado institucional (Cambia este valor)
            light: '#d4c29c',
            dark: '#8f7b50',
            contrastText: '#ffffff',
        },
        // Colores de fondo de la aplicación
        background: {
            default: '#f5f5f5', // Gris muy claro para el fondo general
            paper: '#ffffff',   // Blanco para las tarjetas y formularios
        }
    },
    typography: {
        fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    },
    // Aquí podemos estandarizar cómo se ven todos los componentes
    components: {
        MuiButton: {
            styleOverrides: {
                root: {
                    borderRadius: '8px', // Bordes de los botones
                    textTransform: 'none', // Evita que el texto esté todo en MAYÚSCULAS
                    fontWeight: 'bold',
                    padding: '10px 20px',
                },
            },
        },
        MuiTextField: {
            styleOverrides: {
                root: {
                    backgroundColor: '#ffffff', // Fondo blanco para los inputs
                }
            }
        }
    },
});

export default theme;