import { useState, type ReactNode } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import {
  MeuCadastroProvider,
  useMeuCadastro,
} from '../../features/associados/MeuCadastroContext'
import { ProfileModal } from '../../features/associados/ProfileModal'
import { PERFIL_LABEL } from '../../types/user'
import { Navbar } from '../Navbar/Navbar'
import { ChatSidebar } from '../Sidebar/ChatSidebar'
import './MainLayout.css'

interface MainLayoutProps {
  children?: ReactNode
  userName?: string
  userRole?: string
}

function MainLayoutShell({ children, userName, userRole }: MainLayoutProps) {
  const navigate = useNavigate()
  const { usuario, logout } = useAuth()
  const { associado } = useMeuCadastro()
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false)

  const nomeExibicao =
    userName ??
    (usuario?.perfil === 'ASSOCIADO'
      ? associado?.pessoa.nome ?? usuario.nome
      : usuario?.nome)

  function handleOpenProfile() {
    if (usuario?.perfil === 'ASSOCIADO') {
      navigate('/associado/mensalidades?aba=dados')
      return
    }
    setIsProfileModalOpen(true)
  }

  return (
    <div className="app-shell">
      <Navbar
        userName={nomeExibicao}
        userRole={
          userRole ?? (usuario ? PERFIL_LABEL[usuario.perfil] : undefined)
        }
        onLogout={() => void logout()}
        onOpenProfile={handleOpenProfile}
      />

      <div className="app-body">
        <main className="app-main">{children ?? <Outlet />}</main>
        <ChatSidebar />
      </div>

      <ProfileModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
      />
    </div>
  )
}

export function MainLayout(props: MainLayoutProps) {
  return (
    <MeuCadastroProvider>
      <MainLayoutShell {...props} />
    </MeuCadastroProvider>
  )
}
