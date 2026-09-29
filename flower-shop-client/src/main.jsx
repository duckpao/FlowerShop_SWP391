import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { HelmetProvider } from 'react-helmet-async'
import { ThemeProvider } from './context/ThemeContext'
import { LanguageProvider } from './context/LanguageContext'
import './i18n' // <-- Initialize i18n
import './index.css'
import AppRouter from './AppRouter.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <HelmetProvider>
      <LanguageProvider>
        <ThemeProvider>
          <AppRouter />
        </ThemeProvider>
      </LanguageProvider>
    </HelmetProvider>
  </StrictMode>,
)
