import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

// Fuente Inter autoalojada (@fontsource). El tema la declara en
// typography.fontFamily, pero hasta ahora no se importaba en ningún sitio, así
// que la aplicación caía al fallback del sistema y la tipografía del diseño no
// llegaba a verse. Se cargan solo los pesos que usa el tema: 400 cuerpo,
// 500 etiquetas, 600 encabezados y botones, 700 display.
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
