// Instância do Axios com interceptor de autenticação Firebase e tratamento de erros da API
import axios from 'axios'
import { signOut } from 'firebase/auth'
import { auth } from '../config/firebase'

export interface ApiErrorPayload {
  timestamp?: string
  status?: number
  error?: string
  message?: string
  path?: string
  fieldErrors?: Record<string, string>
}

const baseURL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080')
  .replace(/\/+$/, '')
  .replace(/\/api$/, '')

export const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
})

// Anexa o ID token do Firebase em toda requisição (o SDK renova o token sozinho quando está perto de expirar)
api.interceptors.request.use(async (config) => {
  const usuario = auth.currentUser
  if (usuario) {
    const token = await usuario.getIdToken()
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 401 com usuário logado = token recusado pela API (sessão revogada ou conta desativada): encerra a sessão
// e o ProtectedRoute leva o usuário para o login.
api.interceptors.response.use(
  (resposta) => resposta,
  async (erro) => {
    if (axios.isAxiosError(erro) && erro.response?.status === 401 && auth.currentUser) {
      await signOut(auth).catch(() => undefined)
    }
    return Promise.reject(erro)
  },
)

export function getApiErrorMessage(
  error: unknown,
  fallback = 'Não foi possível completar a operação.',
): string {
  if (axios.isAxiosError<ApiErrorPayload>(error)) {
    const data = error.response?.data

    if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
      const firstFieldMessage = Object.values(data.fieldErrors)[0]
      if (firstFieldMessage) return firstFieldMessage
    }

    if (data?.message) {
      return data.message
    }

    if (error.code === 'ERR_NETWORK' || !error.response) {
      return 'Não foi possível conectar ao servidor. Verifique se o backend está rodando na porta 8080.'
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}
