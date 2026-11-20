import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom'
import { MainLayout } from '../components/Layout/MainLayout'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { AdminHomePage } from '../features/dashboard/AdminHomePage'
import { AssociadoHomePage } from '../features/associado/AssociadoHomePage'
import { AssociadoPedidosPage } from '../features/associado/AssociadoPedidosPage'
import { EstoquePage } from '../features/estoque/EstoquePage'
import { AdminPedidosPage } from '../features/pedidos/AdminPedidosPage'

function ModulePlaceholder({
  title,
  backTo = '/admin',
  backLabel = '← Voltar ao Painel Administrativo',
}: {
  title: string
  backTo?: string
  backLabel?: string
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      <Link
        to={backTo}
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
        {backLabel}
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

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        {/*
          ROTA INICIAL ("/"):
          Por enquanto redireciona para "/admin".
          Quando quiser que o sistema abra direto no Login, basta trocar to="/admin" por to="/login"
        */}
        <Route path="/" element={<Navigate to="/admin" replace />} />

        {/* Rotas sem Navbar/Chat (Tela cheia) */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Rotas internas que usam automaticamente Navbar + fundo #F1F6FA + Carollina AI */}
        <Route element={<MainLayout />}>
          {/* Área Administrativa / Funcionários */}
          <Route path="/admin">
            <Route index element={<AdminHomePage />} />
            <Route path="estoque" element={<EstoquePage />} />
            <Route
              path="escala"
              element={<ModulePlaceholder title="Escala de Hoje" />}
            />
            <Route
              path="financeiro"
              element={<ModulePlaceholder title="Módulo Financeiro" />}
            />
            <Route
              path="reservas"
              element={<ModulePlaceholder title="Reservas de Espaços" />}
            />
            <Route
              path="cadastros"
              element={<ModulePlaceholder title="Cadastros" />}
            />
            <Route path="pedidos" element={<AdminPedidosPage />} />
          </Route>

          {/* Área do Associado */}
          <Route path="/associado">
            <Route index element={<AssociadoHomePage />} />
            <Route
              path="reservas"
              element={
                <ModulePlaceholder
                  title="Reservar Espaço (Associado)"
                  backTo="/associado"
                  backLabel="← Voltar ao Início do Associado"
                />
              }
            />
            <Route
              path="financeiro"
              element={
                <ModulePlaceholder
                  title="Mensalidades (Associado)"
                  backTo="/associado"
                  backLabel="← Voltar ao Início do Associado"
                />
              }
            />
            <Route path="pedidos" element={<AssociadoPedidosPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
