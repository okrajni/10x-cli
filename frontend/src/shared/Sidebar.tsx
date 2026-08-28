import { Link, useLocation } from 'react-router-dom'
import clsx from 'clsx'

export default function Sidebar() {
  const location = useLocation()

  const isActive = (path: string) => location.pathname === path

  const links = [
    { path: '/dashboard', label: '📊 Dashboard' },
    { path: '/settings', label: '⚙️ Settings' },
  ]

  return (
    <aside className="w-64 bg-white border-r border-gray-200 p-6">
      <nav className="space-y-2">
        {links.map(({ path, label }) => (
          <Link
            key={path}
            to={path}
            className={clsx(
              'block px-4 py-2 rounded-md transition',
              isActive(path)
                ? 'bg-blue-100 text-blue-700 font-medium'
                : 'text-gray-700 hover:bg-gray-100'
            )}
          >
            {label}
          </Link>
        ))}
      </nav>
    </aside>
  )
}
