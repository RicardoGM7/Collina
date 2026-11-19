import { Navigate, Route, Routes } from 'react-router-dom'
import AssociadosLayout from '../features/associados/AssociadosLayout'
import AssociadosListPage from '../features/associados/pages/AssociadosListPage'
import AssociadoDetalhePage from '../features/associados/pages/AssociadoDetalhePage'
import PlanosPage from '../features/associados/pages/PlanosPage'

// Definição de rotas do react-router-dom.
// Cada módulo registra suas rotas aqui. A proteção por perfil (ProtectedRoute) entra com o módulo de autenticação.
export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/associados" replace />} />

      {/* Módulo de Associados */}
      <Route path="/associados" element={<AssociadosLayout />}>
        <Route index element={<AssociadosListPage />} />
        <Route path="planos" element={<PlanosPage />} />
        <Route path=":id" element={<AssociadoDetalhePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/associados" replace />} />
    </Routes>
  )
}
