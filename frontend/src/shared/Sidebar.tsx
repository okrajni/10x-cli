import { Link, useLocation } from 'react-router-dom'
import clsx from 'clsx'

export default function Sidebar() {
  const location = useLocation()

  const isActive = (path: string) => location.pathname === path

  const links = [
    { path: '/dashboard', label: 'Dashboard' },
    { path: '/task', label: 'Tasks' },
    { path: '/settings', label: 'Settings' },
  ]

  return (
    <aside className="hidden w-60 shrink-0 border-r border-accent/30 px-5 py-10 md:block">
      <p className="label px-4">Navigation</p>

      <nav className="space-y-1.5">
        {links.map(({ path, label }) => (
          <Link
            key={path}
            to={path}
            aria-current={isActive(path) ? 'page' : undefined}
            className={clsx(
              // Nav items echo the button language: pill shape, hairline border.
              'block rounded-pill border px-4 py-2 text-sm transition',
              isActive(path)
                ? 'border-accent bg-accent font-medium text-canvas'
                : 'border-transparent font-light text-accent hover:border-accent/40 hover:bg-accent/10'
            )}
          >
            {label}
          </Link>
        ))}
      </nav>
    </aside>
  )
}
