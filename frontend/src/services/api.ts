// Instância do Axios com interceptor de autenticação
import axios from 'axios'
import { signOut } from 'firebase/auth'
import { auth } from '../config/firebase'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
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
