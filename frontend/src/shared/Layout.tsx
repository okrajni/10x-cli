import { Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@features/auth/context/AuthContext'
import Header from './Header'
import Sidebar from './Sidebar'

export default function Layout() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  // Hide layout on public pages
  const isPublicPage = location.pathname === '/login' || location.pathname === '/register'
  const showLayout = isAuthenticated && !isPublicPage

  return (
    <div className="flex h-screen flex-col bg-canvas text-accent">
      {showLayout && <Header />}

      <div className="flex flex-1 overflow-hidden">
        {showLayout && <Sidebar />}

        <main className="flex-1 overflow-auto">
          {/* Generous, consistent page padding — the layout breathes. */}
          <div className="mx-auto max-w-content px-6 py-10 sm:px-10 sm:py-14">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
