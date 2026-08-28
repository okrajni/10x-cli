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
    <div className="flex flex-col h-screen bg-gray-50">
      {showLayout && <Header />}

      <div className="flex flex-1 overflow-hidden">
        {showLayout && <Sidebar />}

        <main className="flex-1 overflow-auto">
          <div className="p-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
