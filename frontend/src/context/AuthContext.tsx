// Contexto global de autenticação (usuário, token, roles)
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import {
  createUserWithEmailAndPassword,
  deleteUser,
  onAuthStateChanged,
  sendEmailVerification,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signOut,
  updateProfile,
  type User,
} from 'firebase/auth'
import { auth } from '../config/firebase'
import { api } from '../services/api'
import { isPerfil, type Usuario, type UsuarioApi } from '../types/user'

interface AuthContextValue {
  usuario: Usuario | null
  /** true enquanto o Firebase restaura a sessão (primeiro carregamento da página) */
  carregando: boolean
  login: (email: string, senha: string) => Promise<Usuario>
  cadastrar: (
    nome: string,
    email: string,
    senha: string,
    aposSincronizar?: () => Promise<void>,
  ) => Promise<Usuario>
  logout: () => Promise<void>
  recuperarSenha: (email: string) => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

/**
 * Monta o Usuario a partir do usuário do Firebase. O perfil vem da custom claim "perfil" do ID token.
 * Se a claim não existe (conta nova) ou se forcarSync = true (login/cadastro), chama o back-end
 * (POST /api/auth/sincronizar), que grava o perfil, e renova o token para enxergar a claim nova.
 */
async function resolverUsuario(fb: User, forcarSync: boolean): Promise<Usuario | null> {
  let token = await fb.getIdTokenResult()
  let perfil = isPerfil(token.claims.perfil) ? token.claims.perfil : null
  let nomeApi: string | null = null

  if (forcarSync || !perfil) {
    const { data } = await api.post<UsuarioApi>('/api/auth/sincronizar')
    nomeApi = data.nome
    token = await fb.getIdTokenResult(true)
    perfil = isPerfil(token.claims.perfil) ? token.claims.perfil : null
  }

  if (!perfil) return null

  const email = fb.email ?? ''
  return {
    uid: fb.uid,
    email,
    nome: fb.displayName ?? nomeApi ?? email,
    perfil,
    emailVerificado: fb.emailVerified,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [carregando, setCarregando] = useState(true)
  // Durante login/cadastro o próprio fluxo resolve o usuário; o listener do Firebase fica de fora para não competir
  const fluxoManualRef = useRef(false)

  useEffect(() => {
    return onAuthStateChanged(auth, async (fb) => {
      if (fluxoManualRef.current) return

      if (!fb) {
        setUsuario(null)
        setCarregando(false)
        return
      }

      try {
        setUsuario(await resolverUsuario(fb, false))
      } catch {
        // Não foi possível obter o perfil (ex.: API fora do ar): encerra a sessão em vez de deixar o usuário "meio logado"
        await signOut(auth).catch(() => undefined)
        setUsuario(null)
      } finally {
        setCarregando(false)
      }
    })
  }, [])

  const concluirAcesso = useCallback(async (fb: User): Promise<Usuario> => {
    const resolvido = await resolverUsuario(fb, true)
    if (!resolvido) {
      throw new Error('Sua conta não possui um perfil de acesso. Procure um administrador do clube.')
    }
    setUsuario(resolvido)
    return resolvido
  }, [])

  const login = useCallback(
    async (email: string, senha: string) => {
      fluxoManualRef.current = true
      try {
        const credencial = await signInWithEmailAndPassword(auth, email.trim(), senha)
        return await concluirAcesso(credencial.user)
      } catch (erro) {
        setUsuario(null)
        await signOut(auth).catch(() => undefined)
        throw erro
      } finally {
        fluxoManualRef.current = false
      }
    },
    [concluirAcesso],
  )

  const cadastrar = useCallback(
    async (
      nome: string,
      email: string,
      senha: string,
      aposSincronizar?: () => Promise<void>,
    ) => {
      fluxoManualRef.current = true
      let usuarioCriado: User | null = null
      try {
        const credencial = await createUserWithEmailAndPassword(auth, email.trim(), senha)
        usuarioCriado = credencial.user
        await updateProfile(credencial.user, { displayName: nome.trim() })
        // E-mail de verificação é opcional para o uso comum, mas necessário para virar ADMIN por collina.auth.admin-emails
        await sendEmailVerification(credencial.user).catch(() => undefined)
        const resolvido = await resolverUsuario(credencial.user, true)
        if (!resolvido) {
          throw new Error('Sua conta não possui um perfil de acesso. Procure um administrador do clube.')
        }
        if (aposSincronizar) {
          await aposSincronizar()
        }
        setUsuario(resolvido)
        return resolvido
      } catch (erro) {
        if (usuarioCriado) {
          // Evita deixar conta órfã no Firebase se o cadastro do associado no banco falhar (ex.: CPF duplicado)
          await deleteUser(usuarioCriado).catch(() => undefined)
        }
        setUsuario(null)
        await signOut(auth).catch(() => undefined)
        throw erro
      } finally {
        fluxoManualRef.current = false
      }
    },
    [],
  )

  const logout = useCallback(async () => {
    await signOut(auth)
    setUsuario(null)
  }, [])

  const recuperarSenha = useCallback(async (email: string) => {
    await sendPasswordResetEmail(auth, email.trim())
  }, [])

  const valor = useMemo<AuthContextValue>(
    () => ({ usuario, carregando, login, cadastrar, logout, recuperarSenha }),
    [usuario, carregando, login, cadastrar, logout, recuperarSenha],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext)
  if (!contexto) {
    throw new Error('useAuth deve ser usado dentro de <AuthProvider>.')
  }
  return contexto
}
