import { User } from 'lucide-react'
import logoBranca from '../../assets/LogoBrancaCollina.png'
import './Navbar.css'

interface NavbarProps {
  userName?: string
  userRole?: string
}

export function Navbar({
  userName = 'Nome do Administrador',
  userRole = 'Administrador',
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
        <div className="navbar-avatar">
          <User size={18} />
        </div>
        <div className="navbar-user-info">
          <span className="navbar-user-name">{userName}</span>
          <span className="navbar-user-role">{userRole}</span>
        </div>
      </div>
    </header>
  )
}
