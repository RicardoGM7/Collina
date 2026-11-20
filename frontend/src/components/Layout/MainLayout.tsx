import type { ReactNode } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Navbar } from '../Navbar/Navbar'
import { ChatSidebar } from '../Sidebar/ChatSidebar'
import './MainLayout.css'

interface MainLayoutProps {
  children?: ReactNode
  userName?: string
  userRole?: string
}

export function MainLayout({ children, userName, userRole }: MainLayoutProps) {
  const location = useLocation()
  const isAssociadoRoute = location.pathname.startsWith('/associado')

  const resolvedUserName =
    userName ??
    (isAssociadoRoute ? 'Nome do Titular' : 'Nome do Administrador')
  const resolvedUserRole =
    userRole ?? (isAssociadoRoute ? 'Titular' : 'Administrador')

  return (
    <div className="app-shell">
      <Navbar userName={resolvedUserName} userRole={resolvedUserRole} />

      <div className="app-body">
        <main className="app-main">{children ?? <Outlet />}</main>
        <ChatSidebar />
      </div>
    </div>
  )
}
