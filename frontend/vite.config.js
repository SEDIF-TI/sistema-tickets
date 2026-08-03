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
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff,woff2}'],

        // Las peticiones a la API NUNCA se cachean: servirían datos obsoletos
        // de tickets o resguardos y, peor aún, podrían dejar respuestas con
        // información de un usuario en el equipo de otro.
        navigateFallbackDenylist: [/^\/api/],
        runtimeCaching: [
          {
            urlPattern: /^.*\/api\/.*$/,
            handler: 'NetworkOnly',
          },
        ],
      },
      manifest: {
        name: 'Sistema de Tickets',
        short_name: 'Tickets',
        description: 'Gestión y Generación de Dictámenes Técnicos',
        theme_color: '#801A36',
        background_color: '#f8fafc',
        display: 'standalone',
        lang: 'es-MX',
      },
    }),
  ],

  // Mantiene la configuración global para SockJS
  define: {
    global: 'window',
  },

  build: {
    // Sin source maps en producción: publicarlos entrega el código fuente
    // completo, con comentarios y nombres de variables, a cualquiera que abra
    // las herramientas de desarrollo.
    sourcemap: false,

    rollupOptions: {
      output: {
        // Se separan las librerías grandes del código propio: cambian mucho
        // menos, así que el navegador las reutiliza entre despliegues en lugar
        // de volver a descargar un bundle único de más de 1 MB.
        //
        // Rolldown (el empaquetador de Vite 8) exige que manualChunks sea una
        // función; la forma de objeto de Rollup ya no se admite.
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;

          if (id.includes('react-router')) return 'react';
          if (id.includes('/react-dom/') || id.includes('/react/')) return 'react';
          if (id.includes('@mui/icons-material')) return 'mui-iconos';
          if (id.includes('@mui/') || id.includes('@emotion/')) return 'mui';
          if (id.includes('recharts') || id.includes('d3-')) return 'graficas';
          if (id.includes('stompjs') || id.includes('sockjs')) return 'tiempo-real';

          return undefined;
        },
      },
    },
  },
});
