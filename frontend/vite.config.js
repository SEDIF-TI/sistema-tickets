import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define:{
    global: 'window', // Soluciona el error "global is not defined" al usar SockJS en Vite
  },
})
