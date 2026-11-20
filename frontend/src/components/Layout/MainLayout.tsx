import { useState, type ReactNode } from 'react'
import { Outlet } from 'react-router-dom'
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
  const { usuario, logout } = useAuth()
  const { associado } = useMeuCadastro()
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false)

  const nomeExibicao =
    userName ??
    (usuario?.perfil === 'ASSOCIADO'
      ? associado?.pessoa.nome ?? usuario.nome
      : usuario?.nome)

  return (
    <div className="app-shell">
      <Navbar
        userName={nomeExibicao}
        userRole={
          userRole ?? (usuario ? PERFIL_LABEL[usuario.perfil] : undefined)
        }
        onLogout={() => void logout()}
        onOpenProfile={() => setIsProfileModalOpen(true)}
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
