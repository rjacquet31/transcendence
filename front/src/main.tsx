import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './style/token.css'
import './style/body.css'
import App from './App.tsx'
import { AuthModalProvider } from './auth/AuthModalProvider.tsx'
import { SocketProvider } from './ws/SocketProvider.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthModalProvider>
        <SocketProvider>
          <App />
        </SocketProvider>
      </AuthModalProvider>
    </BrowserRouter>
  </StrictMode>,
)
