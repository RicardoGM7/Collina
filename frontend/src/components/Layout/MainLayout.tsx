import type { ReactNode } from 'react'
import { Outlet } from 'react-router-dom'
import { Navbar } from '../Navbar/Navbar'
import { ChatSidebar } from '../Sidebar/ChatSidebar'
import './MainLayout.css'

interface MainLayoutProps {
  children?: ReactNode
  userName?: string
  userRole?: string
}

export function MainLayout({ children, userName, userRole }: MainLayoutProps) {
  return (
    <div className="app-shell">
      <Navbar userName={userName} userRole={userRole} />

      <div className="app-body">
        <main className="app-main">{children ?? <Outlet />}</main>
        <ChatSidebar />
      </div>
    </div>
  )
}
