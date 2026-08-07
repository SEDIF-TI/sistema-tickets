import { createTheme, alpha } from '@mui/material/styles';

/**
 * ============================================================================
 * SISTEMA DE DISEÑO — Sistema de Tickets SEDIF
 * ============================================================================
 *
 * Fuente única de verdad para color, tipografía, espaciado y estados.
 * Ningún componente escribe colores a mano: lo que falte se añade aquí y se
 * consume desde la paleta o desde `theme.brand`.
 *
 * Todos los pares de color están verificados contra WCAG 2.1 AA:
 * texto normal ≥ 4.5:1, componentes de interfaz ≥ 3:1.
 * ============================================================================
 */

// ---------------------------------------------------------------------------
// 1. TOKENS DE COLOR
// ---------------------------------------------------------------------------

/**
 * Escala institucional guinda. Los tonos 50–300 son solo para fondos y
 * bordes: su contraste sobre blanco es bajo y nunca deben usarse como texto.
 * Desde el 400 (4.41:1) el color sirve para texto grande; el 500 alcanza
 * 9.91:1, apto para cualquier tamaño.
 */
const guinda = {
  50:  '#FDF2F5',   //  1.09:1 — fondo muy suave (filas alternas, hover)
  100: '#FAE0E7',   //  1.24:1 — fondo de chips e insignias
  200: '#F0BCC9',   //  1.65:1 — bordes decorativos
  300: '#D98EA3',   //  2.52:1 — estados deshabilitados
  400: '#B85A76',   //  4.41:1 — texto grande sobre blanco
  500: '#801A36',   //  9.91:1 — PRINCIPAL (AAA para texto normal)
  600: '#6D142E',   // hover de superficies guinda
  700: '#5C0A28',   // 13.73:1 — pulsado / énfasis
  800: '#42071C',   // 16.42:1 — máximo contraste
};

/** Grises neutros (escala slate) para texto, bordes y fondos. */
const neutro = {
  50:  '#F8FAFC',   // fondo de página
  100: '#F1F5F9',   // fondo de cabeceras de tabla
  200: '#E2E8F0',   // bordes y divisores
  300: '#CBD5E1',   // bordes de input
  400: '#94A3B8',   // texto deshabilitado / placeholder
  500: '#64748B',   //  4.76:1 — texto secundario (cumple AA)
  600: '#475569',   //  7.58:1 — texto secundario de alto contraste
  700: '#334155',
  800: '#1E293B',
  900: '#0F172A',   // texto principal
};

/**
 * Colores de estado. Cada `main` sirve para los dos usos que les da MUI:
 * texto sobre blanco y fondo con texto blanco. Ratios sobre blanco:
 * success 5.02:1, warning 5.02:1, error 5.62:1, info 5.85:1.
 *
 * El uso más exigente es `Alert` en variante rellena, donde el `main` pasa a
 * ser fondo y el texto se pinta con `contrastText`.
 */
const estado = {
  success: { main: '#15803D', light: '#DCFCE7', dark: '#166534', contrastText: '#FFFFFF' },
  error:   { main: '#C62828', light: '#FEE2E2', dark: '#991B1B', contrastText: '#FFFFFF' },
  warning: { main: '#B45309', light: '#FEF3C7', dark: '#92400E', contrastText: '#FFFFFF' },
  info:    { main: '#0369A1', light: '#E0F2FE', dark: '#075985', contrastText: '#FFFFFF' },
};

// ---------------------------------------------------------------------------
// 2. TOKENS DE ELEVACIÓN Y MOVIMIENTO
// ---------------------------------------------------------------------------

/** Sombras suaves, teñidas del neutro 900 en lugar de negro puro. */
const sombra = {
  sm: '0 1px 2px rgba(15, 23, 42, 0.06)',
  md: '0 2px 8px rgba(15, 23, 42, 0.08)',
  lg: '0 4px 16px rgba(15, 23, 42, 0.12)',
  xl: '0 8px 32px rgba(15, 23, 42, 0.18)',
};

/** Curva y duraciones estándar: 150 ms para hover, 250 ms para diálogos. */
const CURVA = 'cubic-bezier(0.4, 0, 0.2, 1)';
const transicion = {
  rapida: `all 150ms ${CURVA}`,
  media: `all 250ms ${CURVA}`,
};

// ---------------------------------------------------------------------------
// 3. TEMA
// ---------------------------------------------------------------------------

const theme = createTheme({

  palette: {
    primary: {
      main: guinda[500],
      light: guinda[400],
      dark: guinda[700],
      contrastText: '#FFFFFF',
    },
    // Neutro oscuro para acciones secundarias: no compite con el guinda.
    secondary: {
      main: neutro[700],
      light: neutro[500],
      dark: neutro[900],
      contrastText: '#FFFFFF',
    },
    ...estado,
    text: {
      primary: neutro[900],
      secondary: neutro[600],
      disabled: neutro[400],
    },
    background: {
      default: neutro[50],
      paper: '#FFFFFF',
    },
    divider: neutro[200],
    grey: neutro,
    action: {
      hover: alpha(guinda[500], 0.04),
      selected: alpha(guinda[500], 0.08),
      disabledBackground: neutro[200],
      disabled: neutro[400],
    },
  },

  /** Tokens propios, accesibles desde cualquier componente vía useTheme(). */
  brand: { guinda, neutro, sombra, transicion },

  // -------------------------------------------------------------------------
  // TIPOGRAFÍA — base 16px, escala modular, pesos jerarquizados
  // -------------------------------------------------------------------------
  typography: {
    fontFamily: '"Inter", "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
    fontSize: 16,

    // 700 en h1–h2 y 600 en el resto: la jerarquía se apoya en el peso además
    // del tamaño.
    h1: { fontSize: '2.5rem',   fontWeight: 700, lineHeight: 1.2,  letterSpacing: '-0.02em' },
    h2: { fontSize: '2rem',     fontWeight: 700, lineHeight: 1.25, letterSpacing: '-0.01em' },
    h3: { fontSize: '1.75rem',  fontWeight: 600, lineHeight: 1.3 },
    h4: { fontSize: '1.5rem',   fontWeight: 600, lineHeight: 1.3 },
    h5: { fontSize: '1.25rem',  fontWeight: 600, lineHeight: 1.4 },
    h6: { fontSize: '1.125rem', fontWeight: 600, lineHeight: 1.4 },

    subtitle1: { fontSize: '1rem',      fontWeight: 600, lineHeight: 1.5 },
    subtitle2: { fontSize: '0.875rem',  fontWeight: 600, lineHeight: 1.5 },
    body1:     { fontSize: '1rem',      fontWeight: 400, lineHeight: 1.6 },
    body2:     { fontSize: '0.875rem',  fontWeight: 400, lineHeight: 1.5 },
    caption:   { fontSize: '0.75rem',   fontWeight: 400, lineHeight: 1.4 },
    overline:  { fontSize: '0.75rem',   fontWeight: 600, lineHeight: 1.4, letterSpacing: '0.08em' },
    button:    { fontSize: '0.9375rem', fontWeight: 600, textTransform: 'none' },
  },

  shape: { borderRadius: 8 },

  // Sistema de 8px: theme.spacing(1) = 8px, spacing(3) = 24px.
  spacing: 8,

  breakpoints: {
    values: { xs: 0, sm: 600, md: 960, lg: 1264, xl: 1904 },
  },

  // -------------------------------------------------------------------------
  // COMPONENTES
  // -------------------------------------------------------------------------
  components: {

    MuiCssBaseline: {
      styleOverrides: {
        // Foco visible en todo elemento interactivo (WCAG 2.4.7).
        ':focus-visible': {
          outline: `3px solid ${guinda[500]}`,
          outlineOffset: '2px',
          borderRadius: '4px',
        },
        // Respeta a quien tenga activado "reducir movimiento" en su sistema.
        '@media (prefers-reduced-motion: reduce)': {
          '*, *::before, *::after': {
            animationDuration: '0.01ms !important',
            animationIterationCount: '1 !important',
            transitionDuration: '0.01ms !important',
            scrollBehavior: 'auto !important',
          },
        },
        // Ninguna página debe provocar barra de scroll horizontal.
        'html, body': { overflowX: 'hidden' },
        body: { backgroundColor: neutro[50] },
        // Barra de scroll discreta y acorde con la paleta.
        '::-webkit-scrollbar': { width: 10, height: 10 },
        '::-webkit-scrollbar-track': { background: neutro[100] },
        '::-webkit-scrollbar-thumb': {
          background: neutro[300],
          borderRadius: 8,
          '&:hover': { background: neutro[400] },
        },
      },
    },

    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          borderRadius: 8,
          minHeight: 44,          // WCAG 2.5.5: objetivo táctil mínimo
          padding: '8px 20px',
          transition: transicion.rapida,
          '&:active': { transform: 'scale(0.98)' },
          '&.Mui-disabled': { opacity: 0.6 },
        },
        // Tamaños del estándar: 32 / 44 / 48 px de altura.
        sizeSmall: { minHeight: 32, padding: '4px 12px', fontSize: '0.8125rem' },
        sizeLarge: { minHeight: 48, padding: '12px 28px', fontSize: '1rem' },
        contained: {
          boxShadow: 'none',
          '&:hover': { boxShadow: sombra.md },
        },
        outlined: {
          borderWidth: '1.5px',
          '&:hover': { borderWidth: '1.5px', backgroundColor: alpha(guinda[500], 0.04) },
        },
      },
    },

    MuiIconButton: {
      styleOverrides: {
        // 44x44 también en iconos: son objetivos táctiles.
        root: { minWidth: 44, minHeight: 44, transition: transicion.rapida },
        sizeSmall: { minWidth: 34, minHeight: 34 },
      },
    },

    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { borderRadius: 12, backgroundImage: 'none' },
        outlined: { border: `1px solid ${neutro[200]}` },
        elevation1: { boxShadow: sombra.sm },
        elevation2: { boxShadow: sombra.md },
        elevation3: { boxShadow: sombra.lg },
      },
    },

    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          border: `1px solid ${neutro[200]}`,
          boxShadow: sombra.sm,
          transition: transicion.rapida,
        },
      },
    },

    // --- Formularios ---
    MuiTextField: { defaultProps: { variant: 'outlined', size: 'medium' } },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          backgroundColor: '#FFFFFF',
          transition: 'border-color 150ms, box-shadow 150ms',
          '& fieldset': { borderColor: neutro[300] },
          '&:hover fieldset': { borderColor: neutro[400] },
          '&.Mui-focused fieldset': { borderColor: guinda[500], borderWidth: 2 },
          '&.Mui-error.Mui-focused fieldset': { borderColor: estado.error.main },
          '&.Mui-disabled': { backgroundColor: neutro[100] },
        },
        // 16px evita que Safari en iOS haga zoom al enfocar el campo.
        input: { fontSize: '1rem' },
      },
    },
    MuiInputLabel: { styleOverrides: { root: { fontWeight: 500, color: neutro[600] } } },
    MuiFormHelperText: { styleOverrides: { root: { fontSize: '0.75rem', marginLeft: 2 } } },

    // --- Tablas ---
    MuiTableContainer: {
      // Scroll horizontal siempre disponible: es lo que salva a las tablas
      // anchas en pantallas pequeñas.
      styleOverrides: { root: { overflowX: 'auto', WebkitOverflowScrolling: 'touch' } },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { padding: '12px 16px', borderBottom: `1px solid ${neutro[200]}` },
        head: {
          fontWeight: 600,
          fontSize: '0.8125rem',
          letterSpacing: '0.02em',
          textTransform: 'uppercase',
          whiteSpace: 'nowrap',
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          transition: 'background-color 150ms',
          // Resaltado de fila al pasar el cursor. Solo tiene efecto visible en
          // las filas de datos: la fila de encabezado lleva fondo guinda
          // aplicado desde DynamicTable, que gana en especificidad.
          '&:hover': { backgroundColor: neutro[50] },
        },
      },
    },

    // Control de ordenacion de las cabeceras de columna.
    //
    // El encabezado no reacciona al cursor: conserva color y fondo, y la flecha
    // solo se muestra en la columna por la que esta ordenada la tabla
    // (`.Mui-active`). Por defecto MUI revela la flecha al pasar el raton sobre
    // cualquier columna ordenable, y anima esa aparicion mediante opacidad.
    MuiTableSortLabel: {
      // TableSortLabel extiende ButtonBase, que dibuja la onda de Material al
      // pulsar. Se desactiva unicamente en este componente; botones y menus la
      // conservan.
      defaultProps: { disableRipple: true },
      styleOverrides: {
        root: {
          // `inherit` toma el color de la celda, que sobre fondo guinda es
          // blanco. El fondo se deja transparente porque el color lo aporta la
          // celda contenedora: fijarlo aqui taparia el guinda.
          color: 'inherit',
          transition: 'none',
          '&:hover': { color: 'inherit', backgroundColor: 'transparent' },
          '&.Mui-focusVisible': { color: 'inherit', backgroundColor: 'transparent' },

          // Flecha oculta en reposo y al pasar el cursor.
          '& .MuiTableSortLabel-icon': {
            opacity: 0,
            transition: 'none',
          },
          '&:hover .MuiTableSortLabel-icon': { opacity: 0 },

          // Columna activa: la flecha indica la direccion del orden vigente.
          '&.Mui-active': {
            color: 'inherit',
            '&:hover': { color: 'inherit', backgroundColor: 'transparent' },
            '& .MuiTableSortLabel-icon': {
              color: 'inherit !important',
              opacity: 1,
              transition: 'none',
            },
          },
        },
      },
    },

    MuiTablePagination: {
      styleOverrides: {
        // En movil los controles se apilan y el selector de filas se oculta:
        // ocupa mas de lo que aporta en una pantalla estrecha.
        toolbar: { flexWrap: 'wrap', gap: 4, minHeight: 56 },
        selectLabel: { margin: 0 },
        displayedRows: { margin: 0 },
      },
    },

    // Barra de filtros sobre las tablas.
    MuiToolbar: {
      styleOverrides: {
        root: { '&.barra-filtros': { paddingLeft: 16, paddingRight: 16, gap: 12 } },
      },
    },

    // --- Superposiciones ---
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 12,
          boxShadow: sombra.xl,
          // En móvil el diálogo ocupa casi todo el ancho disponible.
          margin: 16,
          width: 'calc(100% - 32px)',
        },
      },
    },
    MuiDialogTitle: {
      styleOverrides: { root: { fontSize: '1.25rem', fontWeight: 600, padding: '20px 24px 8px' } },
    },
    MuiDialogContent: { styleOverrides: { root: { padding: '8px 24px' } } },
    MuiDialogActions: { styleOverrides: { root: { padding: '16px 24px 20px', gap: 8 } } },

    /**
     * Alert sin el override rosa anterior, que forzaba con !important el mismo
     * aspecto para éxito, error y advertencia y anulaba la señal de color.
     * Ahora cada severidad conserva su semántica.
     */
    MuiAlert: {
      styleOverrides: {
        root: { borderRadius: 8, fontWeight: 500, alignItems: 'center' },
        standardSuccess: { backgroundColor: estado.success.light, color: estado.success.dark },
        standardError:   { backgroundColor: estado.error.light,   color: estado.error.dark },
        standardWarning: { backgroundColor: estado.warning.light, color: estado.warning.dark },
        standardInfo:    { backgroundColor: estado.info.light,    color: estado.info.dark },
      },
    },

    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600, borderRadius: 6 },
        sizeSmall: { height: 24, fontSize: '0.75rem' },
      },
    },

    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          borderRadius: 6,
          fontSize: '0.75rem',
          padding: '6px 10px',
          backgroundColor: neutro[800],
        },
      },
    },

    MuiDrawer: { styleOverrides: { paper: { borderRadius: 0, border: 'none' } } },
    MuiAppBar: { defaultProps: { elevation: 0 }, styleOverrides: { root: { borderRadius: 0 } } },

    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          transition: transicion.rapida,
          '&.Mui-selected': {
            backgroundColor: alpha(guinda[500], 0.1),
            '&:hover': { backgroundColor: alpha(guinda[500], 0.16) },
          },
        },
      },
    },

    MuiTab: {
      styleOverrides: {
        root: { textTransform: 'none', fontWeight: 600, fontSize: '0.9375rem', minHeight: 48 },
      },
    },

    MuiSwitch: { styleOverrides: { root: { padding: 8 } } },
    MuiLinearProgress: { styleOverrides: { root: { borderRadius: 4, height: 6 } } },
    MuiSkeleton: { styleOverrides: { root: { backgroundColor: neutro[200], borderRadius: 6 } } },
  },
});

export default theme;
