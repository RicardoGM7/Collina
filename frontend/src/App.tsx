import { MainLayout } from './components/Layout/MainLayout'

export default function App() {
  return (
    <MainLayout userName="Nome do Titular" userRole="Administrador">
      <h1>Painel Administrativo</h1>
      <p style={{ color: '#64748B', marginTop: '6px' }}>
        O conteúdo de cada página entra aqui dentro!
      </p>
    </MainLayout>
  )
}
