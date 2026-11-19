import { NavLink, Outlet } from 'react-router-dom'
import './associados.css'

export default function AssociadosLayout() {
  return (
    <div className="as-page">
      <header className="as-header">
        <div className="as-brand">⚽ Collina</div>
        <nav className="as-nav">
          <NavLink to="/associados" end className={({ isActive }) => (isActive ? 'ativo' : '')}>
            Associados
          </NavLink>
          <NavLink to="/associados/planos" className={({ isActive }) => (isActive ? 'ativo' : '')}>
            Planos
          </NavLink>
        </nav>
      </header>
      <main className="as-main">
        <Outlet />
      </main>
    </div>
  )
}
