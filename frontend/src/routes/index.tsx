import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom'
import { MainLayout } from '../components/Layout/MainLayout'
import { useAuth } from '../context/AuthContext'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { AdminHomePage } from '../features/dashboard/AdminHomePage'
import { EstoquePage } from '../features/estoque/EstoquePage'
import { AdminPedidosPage } from '../features/pedidos/AdminPedidosPage'
import { AssociadoHomePage } from '../features/associados/AssociadoHomePage'
import { AssociadoMensalidadesPage } from '../features/associados/AssociadoMensalidadesPage'
import { AssociadoPedidosPage } from '../features/associados/AssociadoPedidosPage'
import { AdminAssociadosPage } from '../features/associados/AdminAssociadosPage'
import { HOME_POR_PERFIL, PERFIL_LABEL } from '../types/user'
import { ProtectedRoute, PublicOnlyRoute, RootRedirect } from './ProtectedRoute'

function ModulePlaceholder({ title }: { title: string }) {
  const { usuario } = useAuth()
  const home = usuario ? HOME_POR_PERFIL[usuario.perfil] : '/'
  const nomeHome = usuario ? PERFIL_LABEL[usuario.perfil] : 'início'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      <Link
        to={home}
        style={{
          alignSelf: 'flex-start',
          padding: '8px 14px',
          borderRadius: '12px',
          backgroundColor: '#0B1F33',
          color: '#FFFFFF',
          fontSize: '12px',
          fontWeight: 700,
          textDecoration: 'none',
        }}
      >
        ← Voltar ao painel ({nomeHome})
      </Link>
      <h1 style={{ fontSize: '24px', fontWeight: 800, color: '#0B1F33' }}>
        {title}
      </h1>
      <p style={{ fontSize: '14px', color: '#64748B' }}>
        Esta tela será construída no próximo passo.
      </p>
    </div>
  )
}

/*
  CONTROLE DE ACESSO POR PERFIL (RBAC)
  - ADMIN:      painel administrativo e todos os módulos.
  - RECEPCAO:   atendimento (associados, dependentes) e reservas.
  - FINANCEIRO: módulo financeiro.
  - ESTOQUE:    estoque e pedidos.
  - ESCALA_RH:  escalas e turnos.
  - ASSOCIADO:  somente o portal do associado.
  Para liberar/restringir uma tela, ajuste `perfis` da <Route> ou mova-a para outro grupo.
  O back-end repete essas regras no SecurityConfig, então mantenha os dois lados alinhados.
*/
export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        {/* "/" leva ao painel do perfil logado ou ao login */}
        <Route path="/" element={<RootRedirect />} />

        {/* Rotas sem Navbar/Chat (tela cheia), só para quem NÃO está logado */}
        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>

        {/* Exige login. Rotas internas usam Navbar + fundo #F1F6FA + Carollina AI */}
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            {/* Somente administrador */}
            <Route element={<ProtectedRoute perfis={['ADMIN']} />}>
              <Route path="/admin" element={<AdminHomePage />} />
            </Route>

            {/* Cadastros de associados e dependentes: Administrador e Recepção */}
            <Route element={<ProtectedRoute perfis={['ADMIN', 'RECEPCAO']} />}>
              <Route path="/cadastros" element={<AdminAssociadosPage />} />
            </Route>

            {/* Financeiro */}
            <Route element={<ProtectedRoute perfis={['ADMIN', 'FINANCEIRO']} />}>
              <Route
                path="/financeiro"
                element={<ModulePlaceholder title="Módulo Financeiro" />}
              />
            </Route>

            {/* Estoque e pedidos */}
            <Route element={<ProtectedRoute perfis={['ADMIN', 'ESTOQUE']} />}>
              <Route path="/estoque" element={<EstoquePage />} />
              <Route path="/pedidos" element={<AdminPedidosPage />} />
            </Route>

            {/* Escalas e turnos */}
            <Route element={<ProtectedRoute perfis={['ADMIN', 'ESCALA_RH']} />}>
              <Route
                path="/escala"
                element={<ModulePlaceholder title="Escala de Hoje" />}
              />
            </Route>

            {/* Recepção: atendimento e reservas */}
            <Route element={<ProtectedRoute perfis={['ADMIN', 'RECEPCAO']} />}>
              <Route
                path="/reservas"
                element={<ModulePlaceholder title="Reservas de Espaços" />}
              />
            </Route>
            <Route element={<ProtectedRoute perfis={['RECEPCAO']} />}>
              <Route
                path="/recepcao"
                element={<ModulePlaceholder title="Painel da Recepção" />}
              />
            </Route>

            {/* Somente associado */}
            <Route element={<ProtectedRoute perfis={['ASSOCIADO']} />}>
              <Route path="/associado" element={<AssociadoHomePage />} />
              <Route
                path="/associado/mensalidades"
                element={<AssociadoMensalidadesPage />}
              />
              <Route
                path="/associado/pedidos"
                element={<AssociadoPedidosPage />}
              />
            </Route>
          </Route>
        </Route>

        {/* Qualquer endereço desconhecido */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
