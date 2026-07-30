import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate', // Actualiza el Service Worker de forma silenciosa
      workbox: {
        // Almacena en caché la "cáscara" visual del sistema (archivos estáticos)
        globPatterns: ['**/*.{js,css,html,ico,png,svg}'] 
      },
      manifest: {
        name: 'Sistema de Tickets',
        short_name: 'Tickets',
        description: 'Gestión y Generación de Dictámenes Técnicos',
        theme_color: '#801A36',
        background_color: '#f8fafc',
        display: 'standalone'
      }
    })
  ],
  // Mantienes la configuración global para SockJS
  define: {
    global: 'window', 
  },
});