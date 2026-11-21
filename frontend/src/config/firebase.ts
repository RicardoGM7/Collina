// Configuração e inicialização do Firebase SDK
import { initializeApp } from 'firebase/app'
import { getAuth } from 'firebase/auth'

const env = import.meta.env

const firebaseConfig = {
  apiKey: env.VITE_FIREBASE_API_KEY,
  authDomain: env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: env.VITE_FIREBASE_APP_ID,
}

// Campos mínimos para o login funcionar
export const firebaseConfigurado = Boolean(
  firebaseConfig.apiKey && firebaseConfig.authDomain && firebaseConfig.projectId && firebaseConfig.appId,
)

if (!firebaseConfigurado) {
  console.error(
    '[Firebase] Variáveis VITE_FIREBASE_* ausentes. Copie frontend/.env.example para frontend/.env.local e preencha.',
  )
}

// Com apiKey vazia o getAuth lançaria erro e derrubaria a aplicação inteira; o placeholder permite
// renderizar a tela de login avisando o problema de configuração.
const app = initializeApp({
  ...firebaseConfig,
  apiKey: firebaseConfig.apiKey || 'configuracao-ausente',
})

export const auth = getAuth(app)
