import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { HelmetProvider } from 'react-helmet-async'
import { ThemeProvider } from './context/ThemeContext'
import './index.css'
import AppRouter from './AppRouter.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <HelmetProvider>
      <ThemeProvider>
        <AppRouter />
      </ThemeProvider>
    </HelmetProvider>
  </StrictMode>,
)
