import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

// Fuente Inter autoalojada (@fontsource), declarada en typography.fontFamily
// del tema. Sin esta importación la aplicación cae al fallback del sistema.
// Solo se cargan los pesos que el tema utiliza: 400 cuerpo, 500 etiquetas,
// 600 encabezados y botones, 700 display.
import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'

import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
