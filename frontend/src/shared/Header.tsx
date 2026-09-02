import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '@features/auth/context/AuthContext'
import { Button } from './components/Button'

export default function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <header className="border-b border-accent/30 bg-canvas">
      <div className="flex items-center justify-between gap-6 px-6 py-5 sm:px-10">
        <Link to="/" className="wordmark text-lg transition hover:opacity-80 sm:text-xl">
          Done yet?
        </Link>

        <div className="flex items-center gap-5">
          <span className="hidden text-xs font-light text-accent/75 sm:inline">{user?.email}</span>
          <Button variant="secondary" size="sm" onClick={handleLogout}>
            Logout
          </Button>
        </div>
      </div>
    </header>
  )
}
