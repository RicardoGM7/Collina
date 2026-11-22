import { LogOut, User } from 'lucide-react'
import logoBranca from '../../assets/LogoBrancaCollina.png'
import './Navbar.css'

interface NavbarProps {
  userName?: string
  userRole?: string
  /** Quando informado, exibe o botão "Sair" */
  onLogout?: () => void
  /** Ação ao clicar no perfil do usuário */
  onOpenProfile?: () => void
}

export function Navbar({
  userName = 'Nome do Administrador',
  userRole = 'Administrador',
  onLogout,
  onOpenProfile,
}: NavbarProps) {
  return (
    <header className="navbar">
      <div className="navbar-left" />

      <div className="navbar-center">
        <img
          src={logoBranca}
          alt="Collina Sports Club"
          className="navbar-logo"
        />
      </div>

      <div className="navbar-profile">
        <button
          type="button"
          className="navbar-profile-trigger"
          onClick={onOpenProfile}
          title="Ver perfil e dados pessoais"
        >
          <div className="navbar-avatar">
            <User size={18} />
          </div>
          <div className="navbar-user-info">
            <span className="navbar-user-name" title={userName}>
              {userName}
            </span>
            <span className="navbar-user-role">{userRole}</span>
          </div>
        </button>
        {onLogout && (
          <button
            type="button"
            className="navbar-logout-btn"
            onClick={onLogout}
            aria-label="Sair"
            title="Sair"
          >
            <LogOut size={17} />
          </button>
        )}
      </div>
    </header>
  )
}
