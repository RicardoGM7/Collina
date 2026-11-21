import { Toaster } from 'react-hot-toast'
import { AuthProvider } from './context/AuthContext'
import { AppRoutes } from './routes'

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 3600,
          style: {
            borderRadius: '14px',
            background: '#FFFFFF',
            color: '#0B1724',
            border: '1px solid #E2E8F0',
            fontSize: '13px',
            fontWeight: 600,
            padding: '12px 16px',
            boxShadow: '0 10px 28px rgba(11, 23, 36, 0.12)',
          },
          success: {
            iconTheme: {
              primary: '#2BB673',
              secondary: '#FFFFFF',
            },
          },
          error: {
            duration: 4500,
            iconTheme: {
              primary: '#E11D48',
              secondary: '#FFFFFF',
            },
          },
        }}
      />
    </AuthProvider>
  )
}
