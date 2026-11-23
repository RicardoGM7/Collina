// Componente para proteção de rotas por perfil (RBAC)
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { HOME_POR_PERFIL, type PerfilUsuario } from '../types/user'

function TelaCarregando() {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#F1F6FA',
        color: '#64748B',
        fontSize: '14px',
        fontWeight: 600,
      }}
    >
      Carregando...
    </div>
  )
}

interface ProtectedRouteProps {
  /** Perfis autorizados. Sem a prop, basta estar logado. */
  perfis?: PerfilUsuario[]
}

/**
 * Uso como "layout route":
 *   <Route element={<ProtectedRoute perfis={['ADMIN']} />}>
 *     <Route path="/financeiro" element={...} />
 *   </Route>
 * Sem login -> /login (guardando a página pedida). Perfil sem permissão -> tela inicial do próprio perfil.
 */
export function ProtectedRoute({ perfis }: ProtectedRouteProps) {
  const { usuario, carregando } = useAuth()
  const location = useLocation()

  if (carregando) return <TelaCarregando />

  if (!usuario) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (perfis && !perfis.includes(usuario.perfil)) {
    return <Navigate to={HOME_POR_PERFIL[usuario.perfil]} replace />
  }

  return <Outlet />
}

/** Rotas de visitante (login/registro): quem já está logado vai direto para a sua tela inicial. */
export function PublicOnlyRoute() {
  const { usuario, carregando } = useAuth()
  const location = useLocation()

  if (carregando) return <TelaCarregando />

  if (usuario) {
    const origem = (location.state as { from?: string } | null)?.from
    return <Navigate to={origem ?? HOME_POR_PERFIL[usuario.perfil]} replace />
  }

  return <Outlet />
}

/** Rota "/": envia para a tela inicial do perfil ou para o login. */
export function RootRedirect() {
  const { usuario, carregando } = useAuth()

  if (carregando) return <TelaCarregando />

  return <Navigate to={usuario ? HOME_POR_PERFIL[usuario.perfil] : '/login'} replace />
}
