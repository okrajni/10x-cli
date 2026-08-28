# Frontend Scaffold Implementation Plan

## Overview

React frontend foundation for "done yet?" household task management app. This plan implements a lightweight, fast-iterating stack (Vite + React 18 + React Router + Context API + Tailwind + Radix UI) optimized for a solo developer on a 5-week MVP timeline.

The scaffold unblocks all 6 downstream UI slices (S-01 through S-07 except S-04 which is backend-focused) and establishes patterns for component structure, state management, API integration, and styling that scale through the full MVP.

## Current State Analysis

- **Frontend:** Absent — landing page only, no React app scaffolded yet
- **Backend:** Spring Boot initialized, pom.xml configured with Spring Security + JWT, no controllers/endpoints yet
- **Build pipeline:** GitHub Actions CI configured, can be wired to frontend build steps
- **Package management:** Bun (CLI) + Node/npm (backend) — frontend will use Node/npm + Vite
- **Conventions:** Strict TypeScript, ESLint, semantic exit codes, discriminated union error handling (proven in 10x-CLI codebase)

## Desired End State

Specification: After this plan completes, the React frontend is ready to accept feature work (S-01, S-02, etc.). Verification:

1. **Development workflow:** `npm install && npm run dev` starts Vite dev server on `http://localhost:5173` with hot-module-reload working (edit component, see changes instantly without losing state)
2. **Project structure:** Folders exist and are organized domain-by-feature:
   - `src/features/auth/` (login, register, auth context, auth hooks)
   - `src/features/tasks/` (task forms, list, components)
   - `src/features/household/` (household setup, invite flows)
   - `src/lib/` (custom hooks, API client, router utilities)
   - `src/shared/` (reusable Button, Input, Dialog, TaskCard, Layout components)
3. **Auth flow works:** Click "Login" → enter email/password → submit → Context updates → navigate to dashboard → click "Logout" → redirect to login (no backend calls needed, mocked API responses)
4. **Component library ready:** Button (4 variants: primary/secondary/danger/disabled), Input (text/password/email), Dialog (modal), TaskCard (preview of task presentation for S-02/S-06) — all styled with Tailwind + Radix, keyboard-navigable, screen-reader-accessible
5. **API integration pattern established:** Custom fetch wrapper in `src/lib/api/client.ts` with:
   - Bearer token injection in Authorization header
   - Error discrimination (network error vs. 4xx validation vs. 5xx server error)
   - Automatic token refresh on 401 (based on Spring Backend pattern)
   - Logging and retry logic for recoverable failures
6. **Build output verified:**
   - `npm run typecheck` → zero type errors (strict mode)
   - `npm run lint` → zero lint issues
   - `npm run build` → produces `dist/` folder with optimized production build (~150KB gzipped)
7. **Accessibility verified:** All interactive elements keyboard-navigable (Tab, Enter, Escape), focus indicators visible, screen reader announcements present, WCAG AA color contrast confirmed on sample components
8. **No external API calls yet:** All integrations work against mock data or local Spring Boot stubs — ready for real backend wiring in S-01/S-02

### Key Discoveries

- **Auth pattern:** The 10x-CLI codebase (`src/lib/auth-guard.ts`, `src/lib/auth-flow.ts`) provides a proven token refresh + expiry checking pattern. React Context + useReducer will adapt this pattern seamlessly (check token expiry on mount, auto-refresh within 5-minute window before expiration, serialize concurrent auth requests).
- **API error handling:** The 10x-CLI API client (`src/lib/api-client.ts`) uses a discriminated union `ApiResult<T> = { ok: true; data: T } | { ok: false; code: string; error: string }` pattern. This will be ported 1:1 to the React custom fetch wrapper, allowing type-safe error handling throughout downstream UI work.
- **Folder structure precedent:** The 10x-CLI uses domain-driven organization (`src/commands/` = features, `src/lib/` = utilities). This pattern scales well and will be mirrored in React (`src/features/` = slices, `src/lib/` = utilities).
- **Strict TypeScript:** Both 10x-CLI and Spring Boot use strict TypeScript/Java. Vite + TypeScript strict mode will catch integration bugs early (mismatched API response shapes, missing auth checks, etc.).

## What We're NOT Doing

- **Server-side rendering (SSR):** Single-page app (SPA) is sufficient for household use case. No Next.js. Router-based code-splitting via Vite is enough.
- **Advanced component library customization:** No design tokens, CSS-in-JS theming, or custom component API. Tailwind defaults + Radix primitives are sufficient for MVP. Styling polish defers to post-MVP.
- **Real-time WebSocket:** Polling (refetch on interval/focus) is sufficient for MVP guardrails (<500ms latency is preferred, not required). WebSocket infrastructure adds deployment complexity (sticky sessions, fallback polling anyway). Added in v1.1 if needed.
- **Offline-first / service workers:** Not in scope. MVP assumes internet connectivity.
- **Mobile-responsive overhaul:** Mobile-first CSS is default with Tailwind, but advanced responsive UX (drawer sidebars, touch gestures) defers to post-MVP.
- **Testing scaffolding beyond mocks:** Unit tests for custom hooks only. Integration tests and E2E tests defer to S-01+. Focus scaffolding on structure, not test coverage, to hit week-1 deadline.
- **Environment configuration / .env files:** Hardcode API base URL (`http://localhost:8080/api` for dev, handle via Vite env vars later). No secrets in frontend — all sensitive config (API keys) stays in Spring Backend.

## Implementation Approach

**Incremental, feature-flagged phases** that build on each other. After each phase, you can pause, test, and iterate before moving to the next. Phases 1–3 are foundational (dev environment, state management, API layer) and must complete in order. Phases 4–5 can overlap with S-01/S-02 feature work (UI components can be built and tested independently before full router integration).

**Development workflow:**
1. Work through phases sequentially
2. After each phase, run success criteria (npm run dev, npm run typecheck, npm run lint)
3. Manual testing: interact with the app in browser, verify no console errors
4. Commit phase work before moving to next phase (allows rollback if needed)
5. After Phase 5 complete, you're ready for `/10x-implement S-01 phase 1` (registration UI uses already-scaffolded Button, Input, Dialog, API wrapper)

## Phase 1: Vite + TypeScript + Build Foundation

### Overview

Initialize Vite + React project from scratch. Install all dependencies (React, React Router, Tailwind, Radix UI, build tools). Configure TypeScript strict mode, ESLint, Prettier. Set up pre-commit hooks to enforce linting before commits. Verify dev server runs with hot-module-reload (HMR).

### Changes Required

#### 1. Project Initialization

**File:** `package.json`

**Intent:** Define all dependencies and build scripts. Package.json is the source of truth for the project's runtime and dev environment.

**Contract:** Standard Vite + React + TypeScript + Tailwind + Radix setup. No custom scripts yet — just Vite defaults plus ESLint/Prettier.

```json
{
  "name": "done-yet-frontend",
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "typecheck": "tsc --noEmit",
    "lint": "eslint src --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "lint:fix": "eslint src --ext ts,tsx --fix",
    "format": "prettier --write \"src/**/*.{ts,tsx,css}\""
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "@radix-ui/react-dialog": "^1.1.1",
    "@radix-ui/react-label": "^2.0.2",
    "@radix-ui/react-select": "^2.0.0",
    "@radix-ui/react-dropdown-menu": "^2.0.6",
    "@radix-ui/react-checkbox": "^1.0.4",
    "clsx": "^2.0.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "@typescript-eslint/eslint-plugin": "^6.15.0",
    "@typescript-eslint/parser": "^6.15.0",
    "@vitejs/plugin-react": "^4.2.0",
    "autoprefixer": "^10.4.16",
    "eslint": "^8.56.0",
    "eslint-plugin-react": "^7.33.0",
    "eslint-plugin-react-hooks": "^4.6.0",
    "postcss": "^8.4.32",
    "prettier": "^3.1.0",
    "tailwindcss": "^3.3.6",
    "typescript": "^5.3.3",
    "vite": "^5.0.8"
  }
}
```

#### 2. TypeScript Configuration

**File:** `tsconfig.json`

**Intent:** Enable strict type checking to catch bugs early. Copy proven config from 10x-CLI.

**Contract:** Strict mode settings (strict: true, noUncheckedIndexedAccess, noImplicitOverride). Path aliases for cleaner imports (@/*, @features/*, etc.).

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitOverride": true,
    "esModuleInterop": true,
    "moduleResolution": "Bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"],
      "@features/*": ["src/features/*"],
      "@lib/*": ["src/lib/*"],
      "@shared/*": ["src/shared/*"]
    }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

#### 3. Vite Configuration

**File:** `vite.config.ts`

**Intent:** Configure Vite with React plugin, alias paths, API proxy for local Spring Boot backend.

**Contract:** Standard Vite config with:
- React plugin for JSX/TSX support
- Path aliases matching tsconfig.json
- Dev server proxy to `http://localhost:8080/api` (Spring Boot backend)
- Code-splitting manual chunks for vendor libraries

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@features': path.resolve(__dirname, './src/features'),
      '@lib': path.resolve(__dirname, './src/lib'),
      '@shared': path.resolve(__dirname, './src/shared'),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api'),
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom'],
          'router': ['react-router-dom'],
          'radix-ui': ['@radix-ui/react-dialog', '@radix-ui/react-label', '@radix-ui/react-select'],
        },
      },
    },
  },
})
```

#### 4. Tailwind CSS Setup

**File:** `tailwind.config.js`

**Intent:** Configure Tailwind with default color palette and Radix UI plugin.

**Contract:** Standard Tailwind config with content paths, default theme, no custom colors yet (MVP uses Tailwind defaults).

```javascript
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

**File:** `postcss.config.js`

**Intent:** Wire Tailwind into the CSS build pipeline.

**Contract:** Standard PostCSS config with Tailwind and Autoprefixer.

```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

#### 5. ESLint & Prettier Configuration

**File:** `.eslintrc.cjs`

**Intent:** Enforce code quality and catch common mistakes.

**Contract:** Standard ESLint config for React + TypeScript. Rules prioritize readability and bug prevention over style.

```javascript
module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react/recommended',
    'plugin:react-hooks/recommended',
  ],
  ignorePatterns: ['dist', '.eslintrc.cjs'],
  parser: '@typescript-eslint/parser',
  plugins: ['react-refresh'],
  rules: {
    'react-refresh/only-export-components': [
      'warn',
      { allowConstantExport: true },
    ],
    'react/react-in-jsx-scope': 'off', // React 17+ JSX transform
  },
}
```

**File:** `.prettierrc`

**Intent:** Auto-format code for consistency.

**Contract:** Standard Prettier config (2-space indent, single quotes, trailing commas).

```json
{
  "semi": true,
  "trailingComma": "es5",
  "singleQuote": true,
  "printWidth": 100,
  "tabWidth": 2
}
```

#### 6. Git Hooks (Pre-commit Linting)

**File:** `.husky/pre-commit`

**Intent:** Run ESLint before committing — prevents bad code from reaching git history.

**Contract:** Hook script that runs `npm run lint`. Commit fails if linting fails.

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

npm run lint
```

**Setup command (run once during bootstrap):**
```bash
npx husky install
npx husky add .husky/pre-commit "npm run lint"
```

#### 7. Folder Structure

**File paths to create (empty for now):**
```
src/
├── main.tsx                 ← Entry point
├── App.tsx                  ← Root component
├── index.css                ← Tailwind directives
├── features/
│   ├── auth/
│   │   ├── hooks/
│   │   ├── context/
│   │   ├── api.ts
│   │   └── types.ts
│   ├── tasks/
│   ├── household/
│   └── (future slices)
├── lib/
│   ├── api/
│   │   └── client.ts
│   └── router/
│       └── ProtectedRoute.tsx
└── shared/
    ├── components/
    │   ├── Button.tsx
    │   ├── Input.tsx
    │   ├── Dialog.tsx
    │   └── TaskCard.tsx
    └── Layout.tsx
public/
├── index.html               ← HTML shell
```

#### 8. HTML Entry Point

**File:** `index.html`

**Intent:** HTML shell for the React app. Vite loads this and injects the React bundle.

**Contract:** Standard HTML with `<div id="root"></div>` placeholder and script tag pointing to `src/main.tsx`.

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>done yet?</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

#### 9. Tailwind Global Styles

**File:** `src/index.css`

**Intent:** Initialize Tailwind and set global styles.

**Contract:** Tailwind directives (@tailwind), no custom CSS yet (Tailwind defaults are sufficient).

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

/* Global reset / default styles can go here if needed */
```

#### 10. Entry Point (React)

**File:** `src/main.tsx`

**Intent:** Bootstrap React app and mount to DOM.

**Contract:** Standard React 18 entry point. Wraps App with StrictMode for development checks.

```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
```

#### 11. Root App Component (Placeholder)

**File:** `src/App.tsx`

**Intent:** Placeholder for router (will be replaced in Phase 5).

**Contract:** For Phase 1, just render a simple "App is loading..." message. Router wiring happens in Phase 5.

```typescript
export default function App() {
  return <div className="flex items-center justify-center h-screen text-xl">Loading app...</div>
}
```

### Success Criteria

#### Automated Verification

- [ ] 1.1 Dependencies install without errors: `npm install` completes successfully
- [ ] 1.2 Dev server starts: `npm run dev` runs without errors on `http://localhost:5173`
- [ ] 1.3 TypeScript compiles: `npm run typecheck` produces zero errors
- [ ] 1.4 ESLint passes: `npm run lint` produces zero errors
- [ ] 1.5 Production build succeeds: `npm run build` creates `dist/` folder with output files
- [ ] 1.6 Build output verified: `dist/index.html`, `dist/*.js`, and `dist/*.css` files exist

#### Manual Verification

- [ ] 1.7 Dev server hot reload works: Edit `src/App.tsx`, save, observe changes in browser instantly (no refresh needed)
- [ ] 1.8 Tailwind CSS loads: App renders with proper spacing/layout (use browser DevTools to confirm Tailwind classes applied)
- [ ] 1.9 No console errors: Open browser DevTools console, verify no JavaScript errors or warnings
- [ ] 1.10 Production build is reasonable size: `npm run build` output is <5MB total (before gzip)

**Implementation Note**: After completing Phase 1 and all automated verification passes, pause here for manual confirmation that dev server, hot reload, and build pipeline work as expected before proceeding to Phase 2.

---

## Phase 2: Context API + Auth State Management

### Overview

Create the auth state management layer using React Context + useReducer. Implement login/logout actions, token refresh logic (adapted from 10x-CLI pattern), and a custom useAuth() hook. Mock login endpoint to test the flow without backend calls.

### Changes Required

#### 1. Auth Types

**File:** `src/features/auth/types.ts`

**Intent:** Define TypeScript types for auth state and actions.

**Contract:** AuthState (user, token, refreshToken, expiresAt, isLoading, error), AuthAction (LOGIN_START, LOGIN_SUCCESS, LOGIN_ERROR, LOGOUT, REFRESH_TOKEN, etc.)

```typescript
export interface User {
  id: string
  email: string
  name?: string
  householdId?: string
}

export interface AuthState {
  user: User | null
  token: string | null
  refreshToken: string | null
  expiresAt: number | null
  isLoading: boolean
  isAuthenticated: boolean
  error: string | null
}

export type AuthAction =
  | { type: 'LOGIN_START' }
  | { type: 'LOGIN_SUCCESS'; payload: { user: User; token: string; refreshToken: string; expiresAt: number } }
  | { type: 'LOGIN_ERROR'; payload: string }
  | { type: 'LOGOUT' }
  | { type: 'REFRESH_TOKEN_START' }
  | { type: 'REFRESH_TOKEN_SUCCESS'; payload: { token: string; expiresAt: number } }
  | { type: 'REFRESH_TOKEN_ERROR' }
  | { type: 'CLEAR_ERROR' }
  | { type: 'RESTORE_SESSION'; payload: AuthState }
```

#### 2. Auth Reducer

**File:** `src/features/auth/reducer.ts`

**Intent:** Implement the state reducer that handles auth actions.

**Contract:** Pure function that takes (state, action) and returns new state. No side effects.

```typescript
import { AuthState, AuthAction } from './types'

const initialState: AuthState = {
  user: null,
  token: null,
  refreshToken: null,
  expiresAt: null,
  isLoading: false,
  isAuthenticated: false,
  error: null,
}

export function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'LOGIN_START':
      return { ...state, isLoading: true, error: null }
    case 'LOGIN_SUCCESS':
      return {
        ...state,
        user: action.payload.user,
        token: action.payload.token,
        refreshToken: action.payload.refreshToken,
        expiresAt: action.payload.expiresAt,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      }
    case 'LOGIN_ERROR':
      return { ...state, isLoading: false, error: action.payload, isAuthenticated: false }
    case 'LOGOUT':
      return { ...initialState }
    case 'REFRESH_TOKEN_SUCCESS':
      return {
        ...state,
        token: action.payload.token,
        expiresAt: action.payload.expiresAt,
      }
    case 'REFRESH_TOKEN_ERROR':
      return { ...initialState, error: 'Session expired. Please log in again.' }
    case 'CLEAR_ERROR':
      return { ...state, error: null }
    default:
      return state
  }
}

export { initialState }
```

#### 3. Auth Context

**File:** `src/features/auth/context/AuthContext.tsx`

**Intent:** Create Context that holds auth state and dispatch.

**Contract:** React Context with auth state and login/logout/refresh functions exposed.

```typescript
import { createContext, useContext, useReducer, useCallback, useEffect, ReactNode } from 'react'
import { authReducer, initialState } from '../reducer'
import { AuthState, User } from '../types'

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  refreshToken: () => Promise<void>
  clearError: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialState)

  // On mount, check if we have a valid session in localStorage
  useEffect(() => {
    const savedState = localStorage.getItem('authState')
    if (savedState) {
      try {
        const parsed = JSON.parse(savedState)
        if (parsed.token && parsed.expiresAt) {
          // Check if token is not expired
          if (parsed.expiresAt > Date.now()) {
            dispatch({ type: 'RESTORE_SESSION', payload: parsed })
          } else if (parsed.refreshToken) {
            // Try to refresh if refresh token exists
            refreshToken()
          }
        }
      } catch (err) {
        console.error('Failed to restore session:', err)
      }
    }
  }, [])

  // Save auth state to localStorage whenever it changes
  useEffect(() => {
    if (state.isAuthenticated) {
      localStorage.setItem('authState', JSON.stringify(state))
      // Also set token in a cookie for API requests
      if (state.token) {
        document.cookie = `authToken=${state.token}; path=/; SameSite=Strict`
      }
    } else {
      localStorage.removeItem('authState')
      document.cookie = 'authToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;'
    }
  }, [state.isAuthenticated, state.token])

  const login = useCallback(async (email: string, password: string) => {
    dispatch({ type: 'LOGIN_START' })
    try {
      // Mock endpoint for Phase 2 testing
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })

      if (!response.ok) {
        throw new Error('Login failed')
      }

      const data = await response.json()
      const expiresAt = Date.now() + 3600000 // 1 hour from now

      dispatch({
        type: 'LOGIN_SUCCESS',
        payload: {
          user: data.user || { id: '1', email, name: email },
          token: data.token || `mock-token-${Date.now()}`,
          refreshToken: data.refreshToken || `mock-refresh-${Date.now()}`,
          expiresAt,
        },
      })
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Login failed'
      dispatch({ type: 'LOGIN_ERROR', payload: message })
      throw error
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      // Attempt to call logout endpoint if available
      if (state.token) {
        await fetch('/api/auth/logout', {
          method: 'POST',
          headers: { Authorization: `Bearer ${state.token}` },
        }).catch(() => {
          // Silently fail if endpoint doesn't exist
        })
      }
    } finally {
      dispatch({ type: 'LOGOUT' })
    }
  }, [state.token])

  const refreshToken = useCallback(async () => {
    if (!state.refreshToken) {
      dispatch({ type: 'LOGOUT' })
      return
    }

    dispatch({ type: 'REFRESH_TOKEN_START' })
    try {
      const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: state.refreshToken }),
      })

      if (!response.ok) {
        throw new Error('Token refresh failed')
      }

      const data = await response.json()
      const expiresAt = Date.now() + 3600000 // 1 hour from now

      dispatch({
        type: 'REFRESH_TOKEN_SUCCESS',
        payload: {
          token: data.token,
          expiresAt,
        },
      })
    } catch (error) {
      dispatch({ type: 'REFRESH_TOKEN_ERROR' })
    }
  }, [state.refreshToken])

  const clearError = useCallback(() => {
    dispatch({ type: 'CLEAR_ERROR' })
  }, [])

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        logout,
        refreshToken,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
```

#### 4. Test Auth Flow (Manual)

**File:** `src/App.tsx` (update from Phase 1)

**Intent:** Temporary test component to verify auth context works.

**Contract:** Simple login form that dispatches to auth context. Used only for Phase 2 verification, removed in Phase 5 when router is added.

```typescript
import { useState } from 'react'
import { useAuth } from './features/auth/context/AuthContext'

export default function App() {
  const { isAuthenticated, user, login, logout, error, isLoading } = useAuth()
  const [email, setEmail] = useState('test@example.com')
  const [password, setPassword] = useState('password123')

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await login(email, password)
    } catch {
      // Error already in context
    }
  }

  if (isAuthenticated) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <h1 className="text-2xl font-bold mb-4">Welcome, {user?.email}!</h1>
          <button
            onClick={logout}
            className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
          >
            Logout
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex items-center justify-center h-screen bg-gray-100">
      <form onSubmit={handleLogin} className="bg-white p-8 rounded shadow-lg w-96">
        <h1 className="text-2xl font-bold mb-6">Login</h1>

        {error && <div className="mb-4 p-2 bg-red-100 text-red-700 rounded">{error}</div>}

        <div className="mb-4">
          <label className="block text-sm font-medium mb-2">Email</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded"
            placeholder="test@example.com"
          />
        </div>

        <div className="mb-6">
          <label className="block text-sm font-medium mb-2">Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded"
            placeholder="password123"
          />
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="w-full px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {isLoading ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </div>
  )
}
```

#### 5. Wrap App with AuthProvider

**File:** `src/main.tsx` (update)

**Intent:** Ensure AuthContext is available to entire app.

**Contract:** Wrap App component with AuthProvider before mounting to DOM.

```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { AuthProvider } from './features/auth/context/AuthContext'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>
)
```

### Success Criteria

#### Automated Verification

- [ ] 2.1 TypeScript compilation passes: `npm run typecheck` produces zero errors
- [ ] 2.2 No lint errors: `npm run lint` passes
- [ ] 2.3 App still runs: `npm run dev` starts without errors

#### Manual Verification

- [ ] 2.4 Login form renders: Navigate to `http://localhost:5173`, see login form with email/password inputs
- [ ] 2.5 Mock login works: Fill in form, click "Login", see "Welcome, test@example.com!" message (context updated)
- [ ] 2.6 Token saved: Open browser DevTools → Application → Cookies, see `authToken` cookie set
- [ ] 2.7 Session restored: Refresh page, still logged in (auth state restored from localStorage)
- [ ] 2.8 Logout works: Click "Logout", form reappears, cookies cleared
- [ ] 2.9 Error handling: Try login with invalid endpoint `/api/auth/nonexistent`, see error message displayed
- [ ] 2.10 No console errors: Browser DevTools console shows no JavaScript errors

**Implementation Note**: After Phase 2 manual verification passes, you have proven auth state management works. The auth context is ready for wiring into real Spring Boot endpoints in Phase 3. Pause here before moving to Phase 3.

---

## Phase 3: API Integration Layer

### Overview

Create a custom fetch wrapper (`useApi` hook + `client.ts` utility) with auth header injection, error handling, and token refresh logic. Implement domain-specific API modules (auth.ts, tasks.ts, household.ts) that wrap API calls. Test with mock Spring Boot endpoints.

This phase establishes the pattern that downstream slices (S-01, S-02, etc.) will use to talk to the backend.

### Changes Required

#### 1. API Client Utilities

**File:** `src/lib/api/client.ts`

**Intent:** Custom fetch wrapper with Bearer token injection, error discrimination, and retry logic.

**Contract:** Two functions:
- `apiClient<T>(path, options)` → Promise<ApiResult<T>>
- Type-safe error handling via discriminated union ApiResult

```typescript
import { useAuth } from '@features/auth/context/AuthContext'

// Discriminated union for type-safe error handling
export type ApiResult<T> =
  | { ok: true; data: T }
  | { ok: false; code: string; message: string; status: number }

interface ApiClientOptions extends RequestInit {
  headers?: Record<string, string>
}

/**
 * Custom fetch wrapper that:
 * - Injects Bearer token in Authorization header
 * - Discriminates error types (network vs 4xx vs 5xx)
 * - Retries on recoverable errors (5xx, network timeouts)
 * - Returns type-safe ApiResult<T>
 */
export async function apiClient<T>(
  path: string,
  options: ApiClientOptions = {}
): Promise<ApiResult<T>> {
  const token = localStorage.getItem('authState')
    ? JSON.parse(localStorage.getItem('authState')!).token
    : null

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  }

  const maxRetries = 3
  let lastError: Error | null = null

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      const response = await fetch(`/api${path}`, {
        ...options,
        headers,
        signal: AbortSignal.timeout(30000), // 30s timeout
      })

      // Success: 2xx status
      if (response.ok) {
        const data = await response.json()
        return { ok: true, data }
      }

      // 401 Unauthorized: Token expired, should refresh
      if (response.status === 401) {
        return {
          ok: false,
          code: 'UNAUTHORIZED',
          message: 'Session expired. Please log in again.',
          status: 401,
        }
      }

      // 4xx Client Error: Don't retry
      if (response.status < 500) {
        const errorData = await response.json().catch(() => ({}))
        return {
          ok: false,
          code: errorData.code || `HTTP_${response.status}`,
          message: errorData.message || `Request failed with status ${response.status}`,
          status: response.status,
        }
      }

      // 5xx Server Error: Retry after delay
      await new Promise((resolve) => setTimeout(resolve, Math.pow(2, attempt) * 1000))
      continue
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error))

      // Network error or timeout: Retry
      if (attempt < maxRetries - 1) {
        await new Promise((resolve) => setTimeout(resolve, Math.pow(2, attempt) * 1000))
        continue
      }
    }
  }

  // All retries exhausted
  return {
    ok: false,
    code: 'NETWORK_ERROR',
    message: lastError?.message || 'Network request failed',
    status: 0,
  }
}
```

#### 2. Auth API Module

**File:** `src/features/auth/api.ts`

**Intent:** Domain-specific API calls for auth (login, register, refresh, verify).

**Contract:** Functions that wrap apiClient and provide auth-specific endpoints.

```typescript
import { apiClient, ApiResult } from '@lib/api/client'
import { User } from './types'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  user: User
  token: string
  refreshToken: string
}

export async function loginApi(request: LoginRequest): Promise<ApiResult<LoginResponse>> {
  return apiClient('/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface RefreshTokenResponse {
  token: string
  expiresAt: number
}

export async function refreshTokenApi(
  request: RefreshTokenRequest
): Promise<ApiResult<RefreshTokenResponse>> {
  return apiClient('/auth/refresh', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export interface RegisterRequest {
  email: string
  password: string
  name: string
}

export interface RegisterResponse {
  user: User
  token: string
  refreshToken: string
}

export async function registerApi(request: RegisterRequest): Promise<ApiResult<RegisterResponse>> {
  return apiClient('/auth/register', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function logoutApi(): Promise<ApiResult<void>> {
  return apiClient('/auth/logout', {
    method: 'POST',
  })
}
```

#### 3. Tasks API Module

**File:** `src/features/tasks/api.ts`

**Intent:** API calls for task operations (create, read, update, delete, list).

**Contract:** Functions for CRUD operations on tasks. No implementation detail — just stubs for Phase 3, filled in when real backend endpoints exist.

```typescript
import { apiClient, ApiResult } from '@lib/api/client'

export interface Task {
  id: string
  title: string
  description?: string
  category: 'cleaning' | 'shopping' | 'laundry' | 'maintenance' | 'bills'
  dueDate?: string
  assignedTo: string
  status: 'pending' | 'completed'
  householdId: string
  createdAt: string
  updatedAt: string
}

export interface CreateTaskRequest {
  title: string
  description?: string
  category: Task['category']
  dueDate?: string
  assignedTo: string
}

export async function createTaskApi(request: CreateTaskRequest): Promise<ApiResult<Task>> {
  return apiClient('/tasks', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function listTasksApi(): Promise<ApiResult<Task[]>> {
  return apiClient('/tasks')
}

export async function getTaskApi(taskId: string): Promise<ApiResult<Task>> {
  return apiClient(`/tasks/${taskId}`)
}

export interface UpdateTaskRequest {
  title?: string
  description?: string
  category?: Task['category']
  dueDate?: string
  assignedTo?: string
  status?: Task['status']
}

export async function updateTaskApi(
  taskId: string,
  request: UpdateTaskRequest
): Promise<ApiResult<Task>> {
  return apiClient(`/tasks/${taskId}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  })
}

export async function deleteTaskApi(taskId: string): Promise<ApiResult<void>> {
  return apiClient(`/tasks/${taskId}`, {
    method: 'DELETE',
  })
}

export async function completeTaskApi(taskId: string): Promise<ApiResult<Task>> {
  return apiClient(`/tasks/${taskId}`, {
    method: 'PATCH',
    body: JSON.stringify({ status: 'completed' }),
  })
}
```

#### 4. Household API Module

**File:** `src/features/household/api.ts`

**Intent:** API calls for household operations (create, invite, join).

**Contract:** Functions for household CRUD.

```typescript
import { apiClient, ApiResult } from '@lib/api/client'

export interface Household {
  id: string
  name: string
  createdAt: string
  members: Array<{ userId: string; email: string; name?: string }>
}

export interface CreateHouseholdRequest {
  name: string
}

export async function createHouseholdApi(
  request: CreateHouseholdRequest
): Promise<ApiResult<Household>> {
  return apiClient('/households', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export interface InvitePartnerRequest {
  email: string
}

export interface InvitePartnerResponse {
  inviteId: string
  inviteLink: string
}

export async function invitePartnerApi(
  householdId: string,
  request: InvitePartnerRequest
): Promise<ApiResult<InvitePartnerResponse>> {
  return apiClient(`/households/${householdId}/invites`, {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export interface JoinHouseholdRequest {
  inviteCode: string
}

export async function joinHouseholdApi(
  request: JoinHouseholdRequest
): Promise<ApiResult<Household>> {
  return apiClient('/households/join', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function getHouseholdApi(householdId: string): Promise<ApiResult<Household>> {
  return apiClient(`/households/${householdId}`)
}
```

#### 5. Update Auth Context to Use Real API

**File:** `src/features/auth/context/AuthContext.tsx` (update login/logout/refreshToken)

**Intent:** Wire the auth context to use real API client instead of mocking.

**Contract:** login, logout, refreshToken now call the API modules defined above.

Update the login function:
```typescript
const login = useCallback(async (email: string, password: string) => {
  dispatch({ type: 'LOGIN_START' })
  try {
    const result = await loginApi({ email, password })

    if (!result.ok) {
      throw new Error(result.message)
    }

    const expiresAt = Date.now() + 3600000 // 1 hour from now

    dispatch({
      type: 'LOGIN_SUCCESS',
      payload: {
        user: result.data.user,
        token: result.data.token,
        refreshToken: result.data.refreshToken,
        expiresAt,
      },
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Login failed'
    dispatch({ type: 'LOGIN_ERROR', payload: message })
    throw error
  }
}, [])
```

### Success Criteria

#### Automated Verification

- [ ] 3.1 TypeScript compilation passes: `npm run typecheck` produces zero errors
- [ ] 3.2 No lint errors: `npm run lint` passes
- [ ] 3.3 App still runs: `npm run dev` starts without errors

#### Manual Verification

- [ ] 3.4 API client created: `src/lib/api/client.ts` exports `apiClient` function with correct type signature
- [ ] 3.5 API modules created: `src/features/auth/api.ts`, `src/features/tasks/api.ts`, `src/features/household/api.ts` all export typed functions
- [ ] 3.6 Auth context updated: Login/logout use real API client (will fail gracefully if backend not running)
- [ ] 3.7 Error handling works: Try login without backend running, see network error message in UI
- [ ] 3.8 Type safety verified: Hover over API function results in IDE, see correct `ApiResult<T>` type

**Implementation Note**: Phase 3 establishes the API integration pattern. When you run the app now without a Spring Backend running, login will fail with a network error — that's correct. In S-01/S-02 phases, you'll wire this to real Spring Boot endpoints. The pattern is proven and type-safe.

---

## Phase 4: UI Foundation (Radix + Tailwind)

### Overview

Create reusable UI component library using Radix UI primitives + Tailwind CSS. Focus on components needed by downstream slices: Button, Input, Dialog, TaskCard. Test components in isolation, verify keyboard navigation and accessibility.

### Changes Required

#### 1. Button Component

**File:** `src/shared/components/Button.tsx`

**Intent:** Reusable button component with multiple variants and sizes.

**Contract:** React component with variant prop (primary/secondary/danger), size prop (sm/md/lg), disabled state, and click handler.

```typescript
import { ButtonHTMLAttributes, ReactNode } from 'react'
import clsx from 'clsx'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  children: ReactNode
}

export function Button({
  variant = 'primary',
  size = 'md',
  className,
  ...props
}: ButtonProps) {
  const variantStyles = {
    primary: 'bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500',
    secondary: 'bg-gray-200 text-gray-800 hover:bg-gray-300 focus:ring-gray-500',
    danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500',
  }

  const sizeStyles = {
    sm: 'px-2 py-1 text-sm',
    md: 'px-4 py-2 text-base',
    lg: 'px-6 py-3 text-lg',
  }

  const baseStyles =
    'font-medium rounded-md transition focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed'

  return (
    <button
      className={clsx(baseStyles, variantStyles[variant], sizeStyles[size], className)}
      {...props}
    />
  )
}
```

#### 2. Input Component

**File:** `src/shared/components/Input.tsx`

**Intent:** Reusable input component with validation styling.

**Contract:** React input element with optional label, error state, and help text.

```typescript
import { InputHTMLAttributes, ReactNode } from 'react'
import clsx from 'clsx'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helpText?: ReactNode
}

export function Input({ label, error, helpText, className, ...props }: InputProps) {
  const inputStyles = clsx(
    'px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition',
    error ? 'border-red-500 bg-red-50' : 'border-gray-300'
  )

  return (
    <div>
      {label && <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>}
      <input className={clsx(inputStyles, className)} {...props} />
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
      {helpText && !error && <p className="mt-1 text-sm text-gray-500">{helpText}</p>}
    </div>
  )
}
```

#### 3. Dialog Component

**File:** `src/shared/components/Dialog.tsx`

**Intent:** Modal dialog using Radix UI primitives, styled with Tailwind.

**Contract:** Dialog component with open/onOpenChange props, title, and children content.

```typescript
import * as DialogPrimitive from '@radix-ui/react-dialog'
import { ReactNode } from 'react'

interface DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  children: ReactNode
}

export function Dialog({ open, onOpenChange, title, children }: DialogProps) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 bg-black bg-opacity-50 z-40" />

        <DialogPrimitive.Content className="fixed left-1/2 top-1/2 z-50 w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-lg bg-white p-6 shadow-lg focus:outline-none">
          <div className="flex justify-between items-center mb-4">
            <DialogPrimitive.Title className="text-lg font-semibold text-gray-900">
              {title}
            </DialogPrimitive.Title>
            <DialogPrimitive.Close asChild>
              <button
                className="text-gray-400 hover:text-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
                aria-label="Close dialog"
              >
                ✕
              </button>
            </DialogPrimitive.Close>
          </div>

          <DialogPrimitive.Description className="text-gray-600">{children}</DialogPrimitive.Description>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}
```

#### 4. TaskCard Component

**File:** `src/shared/components/TaskCard.tsx`

**Intent:** Reusable task display card for use in S-02 (task list) and S-06 (dashboard).

**Contract:** Displays task title, description, category badge, due date, assignee badge, and action buttons (complete, edit, delete).

```typescript
import { Task } from '@features/tasks/api'
import { Button } from './Button'

interface TaskCardProps {
  task: Task
  onComplete?: (taskId: string) => void
  onEdit?: (task: Task) => void
  onDelete?: (taskId: string) => void
  isLoading?: boolean
}

const categoryColors: Record<Task['category'], string> = {
  cleaning: 'bg-blue-100 text-blue-800',
  shopping: 'bg-green-100 text-green-800',
  laundry: 'bg-purple-100 text-purple-800',
  maintenance: 'bg-yellow-100 text-yellow-800',
  bills: 'bg-red-100 text-red-800',
}

export function TaskCard({
  task,
  onComplete,
  onEdit,
  onDelete,
  isLoading = false,
}: TaskCardProps) {
  const isCompleted = task.status === 'completed'

  return (
    <div
      className={clsx(
        'p-4 border rounded-lg shadow-sm hover:shadow-md transition bg-white',
        isCompleted && 'opacity-60'
      )}
    >
      {/* Header: Title + Category Badge */}
      <div className="flex justify-between items-start mb-2">
        <h3 className={clsx('font-semibold text-gray-900', isCompleted && 'line-through')}>
          {task.title}
        </h3>
        <span className={clsx('text-xs px-2 py-1 rounded-full font-medium', categoryColors[task.category])}>
          {task.category}
        </span>
      </div>

      {/* Description */}
      {task.description && <p className="text-sm text-gray-600 mb-3">{task.description}</p>}

      {/* Due Date + Assignee */}
      <div className="flex gap-4 text-sm text-gray-500 mb-4">
        {task.dueDate && (
          <span>{new Date(task.dueDate).toLocaleDateString()}</span>
        )}
        <span className="font-medium text-blue-600">
          {task.assignedTo === 'me' ? '👤 Me' : '👥 Partner'}
        </span>
      </div>

      {/* Action Buttons */}
      <div className="flex gap-2 justify-end">
        <Button
          variant="primary"
          size="sm"
          onClick={() => onComplete?.(task.id)}
          disabled={isLoading}
          aria-label={`Mark ${task.title} complete`}
        >
          {isCompleted ? '✓ Done' : 'Complete'}
        </Button>

        {onEdit && (
          <Button
            variant="secondary"
            size="sm"
            onClick={() => onEdit(task)}
            disabled={isLoading}
            aria-label={`Edit ${task.title}`}
          >
            Edit
          </Button>
        )}

        {onDelete && (
          <Button
            variant="danger"
            size="sm"
            onClick={() => onDelete(task.id)}
            disabled={isLoading}
            aria-label={`Delete ${task.title}`}
          >
            Delete
          </Button>
        )}
      </div>
    </div>
  )
}
```

#### 5. Component Library Index

**File:** `src/shared/components/index.ts`

**Intent:** Central export point for all shared components.

**Contract:** Export all components so downstream slices can import from `@shared/components`.

```typescript
export { Button } from './Button'
export { Input } from './Input'
export { Dialog } from './Dialog'
export { TaskCard } from './TaskCard'
```

#### 6. Test Components in App (Temporary)

**File:** `src/App.tsx` (update for Phase 4 testing)

**Intent:** Temporary page showing all components in action, used for Phase 4 verification.

```typescript
import { useState } from 'react'
import { Button } from '@shared/components/Button'
import { Input } from '@shared/components/Input'
import { Dialog } from '@shared/components/Dialog'
import { TaskCard } from '@shared/components/TaskCard'
import { Task } from '@features/tasks/api'

export default function App() {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')

  const mockTask: Task = {
    id: '1',
    title: 'Buy groceries',
    description: 'Milk, eggs, bread',
    category: 'shopping',
    dueDate: new Date(Date.now() + 86400000).toISOString(),
    assignedTo: 'me',
    status: 'pending',
    householdId: 'h1',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-4xl mx-auto space-y-8">
        <h1 className="text-3xl font-bold">Component Library</h1>

        {/* Buttons */}
        <section>
          <h2 className="text-xl font-bold mb-4">Buttons</h2>
          <div className="flex gap-4">
            <Button variant="primary">Primary</Button>
            <Button variant="secondary">Secondary</Button>
            <Button variant="danger">Danger</Button>
            <Button disabled>Disabled</Button>
          </div>
        </section>

        {/* Inputs */}
        <section>
          <h2 className="text-xl font-bold mb-4">Inputs</h2>
          <div className="space-y-4 max-w-xs">
            <Input label="Email" type="email" placeholder="test@example.com" value={email} onChange={(e) => setEmail(e.target.value)} />
            <Input label="Email with Error" error="Email is required" />
            <Input label="Email with Help" helpText="We'll never share your email" type="email" />
          </div>
        </section>

        {/* Dialog */}
        <section>
          <h2 className="text-xl font-bold mb-4">Dialog</h2>
          <Button onClick={() => setDialogOpen(true)}>Open Dialog</Button>

          <Dialog open={dialogOpen} onOpenChange={setDialogOpen} title="Example Dialog">
            <p>This is a dialog component using Radix UI and Tailwind CSS.</p>
          </Dialog>
        </section>

        {/* Task Card */}
        <section>
          <h2 className="text-xl font-bold mb-4">Task Card</h2>
          <TaskCard
            task={mockTask}
            onComplete={(id) => console.log('Complete:', id)}
            onEdit={(task) => console.log('Edit:', task)}
            onDelete={(id) => console.log('Delete:', id)}
          />
        </section>
      </div>
    </div>
  )
}
```

### Success Criteria

#### Automated Verification

- [ ] 4.1 TypeScript compilation passes: `npm run typecheck` produces zero errors
- [ ] 4.2 No lint errors: `npm run lint` passes
- [ ] 4.3 App runs: `npm run dev` starts without errors

#### Manual Verification

- [ ] 4.4 Component showcase loads: Navigate to `http://localhost:5173`, see all components rendered
- [ ] 4.5 Buttons work: Click each button, verify click handlers work, disabled button is inactive
- [ ] 4.6 Inputs functional: Type in email input, see text update; try error input, see error styling
- [ ] 4.7 Dialog works: Click "Open Dialog", modal appears with backdrop; press Escape, modal closes
- [ ] 4.8 Task card displays: See mock task with title, category badge, due date, action buttons
- [ ] 4.9 Keyboard navigation: Tab through all interactive elements, all have visible focus indicators
- [ ] 4.10 Accessibility: Use screen reader (NVDA/JAWS/VoiceOver), hear descriptions of buttons and interactive elements

**Implementation Note**: After Phase 4 manual verification, your component library is ready. These components will be reused throughout S-01, S-02, S-06, S-07. The temporary component showcase will be removed in Phase 5 when the router is added.

---

## Phase 5: React Router + Protected Routes

### Overview

Wire React Router v6 into the app. Set up route structure with protected routes, lazy loading, and code-splitting. Create Layout component (header, sidebar, main content area). Build placeholder pages for login, dashboard, and settings. Test auth-based navigation (login → dashboard → logout).

This is the final phase of the scaffold. After this, you have a complete foundation ready for S-01, S-02, etc.

### Changes Required

#### 1. Router Configuration

**File:** `src/router.tsx`

**Intent:** Define all routes with lazy loading and error handling.

**Contract:** React Router configuration with public (login, register) and protected (dashboard, tasks, settings) route groups.

```typescript
import { lazy, Suspense } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from '@lib/router/ProtectedRoute'
import Layout from '@shared/Layout'
import { Button } from '@shared/components'

// Lazy-loaded pages (code-splitting)
const LoginPage = lazy(() => import('@features/auth/pages/LoginPage'))
const RegisterPage = lazy(() => import('@features/auth/pages/RegisterPage'))
const DashboardPage = lazy(() => import('@features/tasks/pages/DashboardPage'))
const SettingsPage = lazy(() => import('@features/auth/pages/SettingsPage'))

const SuspenseWrapper = ({ children }: { children: React.ReactNode }) => (
  <Suspense
    fallback={
      <div className="flex items-center justify-center h-screen">
        <p className="text-gray-500">Loading...</p>
      </div>
    }
  >
    {children}
  </Suspense>
)

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    errorElement: <ErrorPage />,
    children: [
      // Public routes
      {
        path: 'login',
        element: (
          <SuspenseWrapper>
            <LoginPage />
          </SuspenseWrapper>
        ),
      },
      {
        path: 'register',
        element: (
          <SuspenseWrapper>
            <RegisterPage />
          </SuspenseWrapper>
        ),
      },

      // Protected routes
      {
        path: 'dashboard',
        element: (
          <ProtectedRoute>
            <SuspenseWrapper>
              <DashboardPage />
            </SuspenseWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'settings',
        element: (
          <ProtectedRoute>
            <SuspenseWrapper>
              <SettingsPage />
            </SuspenseWrapper>
          </ProtectedRoute>
        ),
      },

      // Redirect root to dashboard if authenticated, login otherwise
      {
        path: '',
        element: <Navigate to="/dashboard" replace />,
      },

      // Catch-all
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
])

function ErrorPage() {
  return (
    <div className="flex items-center justify-center h-screen">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-red-600 mb-2">Error</h1>
        <p className="text-gray-600">Something went wrong. Please try again.</p>
      </div>
    </div>
  )
}

function NotFoundPage() {
  return (
    <div className="flex items-center justify-center h-screen">
      <div className="text-center">
        <h1 className="text-3xl font-bold mb-2">404</h1>
        <p className="text-gray-600 mb-4">Page not found</p>
        <Button variant="primary" onClick={() => (window.location.href = '/dashboard')}>
          Go Home
        </Button>
      </div>
    </div>
  )
}
```

#### 2. Protected Route Component

**File:** `src/lib/router/ProtectedRoute.tsx`

**Intent:** Wrapper component that redirects to login if not authenticated.

**Contract:** Shows loading spinner while auth state is being checked, redirects to login if not authenticated, renders children if authenticated.

```typescript
import { useAuth } from '@features/auth/context/AuthContext'
import { Navigate } from 'react-router-dom'
import { ReactNode } from 'react'

interface ProtectedRouteProps {
  children: ReactNode
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <p className="text-gray-500">Loading...</p>
      </div>
    )
  }

  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}
```

#### 3. Layout Component

**File:** `src/shared/Layout.tsx`

**Intent:** Wraps all pages with header, sidebar, and main content area. Persistent across route changes.

**Contract:** Uses React Router `<Outlet />` to render page content. Hides header/sidebar on public pages (login, register).

```typescript
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
```

#### 4. Header Component

**File:** `src/shared/Header.tsx`

**Intent:** Top navigation bar with logo and user menu.

**Contract:** Shows app logo on left, user email and logout button on right.

```typescript
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
    <header className="bg-white border-b border-gray-200 shadow-sm">
      <div className="px-6 py-4 flex justify-between items-center">
        <Link to="/" className="text-2xl font-bold text-blue-600">
          done yet?
        </Link>

        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">{user?.email}</span>
          <Button variant="secondary" size="sm" onClick={handleLogout}>
            Logout
          </Button>
        </div>
      </div>
    </header>
  )
}
```

#### 5. Sidebar Component

**File:** `src/shared/Sidebar.tsx`

**Intent:** Left sidebar with navigation links.

**Contract:** Links to dashboard and settings. Highlights active link.

```typescript
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
```

#### 6. Login Page

**File:** `src/features/auth/pages/LoginPage.tsx`

**Intent:** Login form page. Redirects to dashboard on successful login.

**Contract:** Form with email/password inputs, submit button, error display, and link to register page.

```typescript
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Input } from '@shared/components/Input'
import { Button } from '@shared/components/Button'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const navigate = useNavigate()
  const { login, error, isLoading, clearError } = useAuth()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    clearError()

    try {
      await login(email, password)
      navigate('/dashboard')
    } catch {
      // Error is in context
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
      <div className="w-full max-w-md">
        <form
          onSubmit={handleSubmit}
          className="bg-white rounded-lg shadow-lg p-8 space-y-6"
        >
          <div className="text-center">
            <h1 className="text-3xl font-bold text-gray-900">done yet?</h1>
            <p className="text-gray-600 mt-2">Sign in to your account</p>
          </div>

          {error && (
            <div className="p-3 bg-red-100 border border-red-400 text-red-700 rounded text-sm">
              {error}
            </div>
          )}

          <Input
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="test@example.com"
            required
          />

          <Input
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />

          <Button
            type="submit"
            variant="primary"
            disabled={isLoading}
            className="w-full"
          >
            {isLoading ? 'Signing in...' : 'Sign In'}
          </Button>

          <p className="text-center text-sm text-gray-600">
            Don't have an account?{' '}
            <Link to="/register" className="text-blue-600 hover:underline font-medium">
              Sign up
            </Link>
          </p>
        </form>
      </div>
    </div>
  )
}
```

#### 7. Dashboard Page (Placeholder)

**File:** `src/features/tasks/pages/DashboardPage.tsx`

**Intent:** Main dashboard page. Shows placeholder for "Today's tasks" (will be fully implemented in S-06).

**Contract:** Protected page. Shows user's household and task summary.

```typescript
import { useAuth } from '@features/auth/context/AuthContext'

export default function DashboardPage() {
  const { user } = useAuth()

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Welcome, {user?.email}!</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-2">📊 Dashboard</h2>
          <p className="text-gray-600">Today's tasks will appear here in S-06.</p>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-2">👥 Household</h2>
          <p className="text-gray-600">Household setup and invites will appear here in S-01.</p>
        </div>
      </div>
    </div>
  )
}
```

#### 8. Settings Page (Placeholder)

**File:** `src/features/auth/pages/SettingsPage.tsx`

**Intent:** Settings page. Placeholder for user preferences (will be expanded later).

**Contract:** Protected page. Shows user profile info and preferences.

```typescript
import { useAuth } from '../context/AuthContext'

export default function SettingsPage() {
  const { user } = useAuth()

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Settings</h1>

      <div className="bg-white p-6 rounded-lg shadow max-w-lg">
        <h2 className="text-xl font-semibold mb-4">User Profile</h2>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700">Email</label>
            <p className="mt-1 text-gray-900">{user?.email}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">Name</label>
            <p className="mt-1 text-gray-900">{user?.name || 'Not set'}</p>
          </div>
        </div>

        <p className="mt-6 text-sm text-gray-500">More settings coming in future versions.</p>
      </div>
    </div>
  )
}
```

#### 9. Register Page (Placeholder)

**File:** `src/features/auth/pages/RegisterPage.tsx`

**Intent:** Registration page. Placeholder for S-01 (new-user-setup will expand this).

**Contract:** Form with email, password, name fields. Links to login.

```typescript
import { Link } from 'react-router-dom'
import { Input } from '@shared/components/Input'
import { Button } from '@shared/components/Button'

export default function RegisterPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-lg shadow-lg p-8 space-y-6">
          <div className="text-center">
            <h1 className="text-3xl font-bold text-gray-900">done yet?</h1>
            <p className="text-gray-600 mt-2">Create your account</p>
          </div>

          <p className="text-center text-sm text-gray-600">
            Registration will be fully implemented in S-01. For now, use login with any email/password.
          </p>

          <Button variant="primary" disabled className="w-full">
            Sign Up (Coming Soon)
          </Button>

          <p className="text-center text-sm text-gray-600">
            Already have an account?{' '}
            <Link to="/login" className="text-blue-600 hover:underline font-medium">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
```

#### 10. Update App.tsx to Use Router

**File:** `src/App.tsx` (update)

**Intent:** Replace test component with actual RouterProvider.

**Contract:** App wraps RouterProvider with router from router.tsx.

```typescript
import { RouterProvider } from 'react-router-dom'
import { router } from './router'

export default function App() {
  return <RouterProvider router={router} />
}
```

#### 11. Update main.tsx

**File:** `src/main.tsx` (update)

**Intent:** Wrap entire app with AuthProvider (needed for routes to access useAuth).

**Contract:** AuthProvider wraps RouterProvider so all routes have access to auth context.

```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { AuthProvider } from './features/auth/context/AuthContext'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>
)
```

### Success Criteria

#### Automated Verification

- [ ] 5.1 TypeScript compilation passes: `npm run typecheck` produces zero errors
- [ ] 5.2 No lint errors: `npm run lint` passes
- [ ] 5.3 Production build succeeds: `npm run build` completes without errors
- [ ] 5.4 Build output reasonable: `dist/` folder is <5MB (before gzip)

#### Manual Verification

- [ ] 5.5 App starts on fresh URL: Navigate to `http://localhost:5173`, redirected to `/login` (not authenticated)
- [ ] 5.6 Login flow works: Fill in login form, click "Sign In", redirected to `/dashboard`
- [ ] 5.7 Navigation works: Click sidebar links, navigate between `/dashboard` and `/settings` without page reload (React Router working)
- [ ] 5.8 Hot reload works: Edit a component, save, changes appear instantly without losing app state
- [ ] 5.9 Logout works: Click "Logout" in header, redirected to `/login`, session cleared
- [ ] 5.10 Page reload preserves session: On `/dashboard`, refresh page, still logged in (session restored from localStorage)
- [ ] 5.11 Lazy loading works: Open browser DevTools Network tab, when navigating to a route, observe new JS chunk loading
- [ ] 5.12 Protected routes work: Try to access `/dashboard` by typing URL while logged out, redirected to login
- [ ] 5.13 No console errors: All pages load without JavaScript errors or warnings

**Implementation Note**: After Phase 5 manual verification passes, your frontend scaffold is **complete and ready for downstream slices**. Every major system is in place:
- ✅ Vite dev server with HMR
- ✅ React Router with lazy loading
- ✅ Context API auth state management
- ✅ Custom fetch wrapper + API integration
- ✅ Reusable UI component library (Button, Input, Dialog, TaskCard)
- ✅ Protected routes and layout
- ✅ TypeScript strict mode, ESLint, Prettier

You can now proceed to `/10x-implement S-01 phase 1` (registration UI) or S-02 (task CRUD UI) and build features directly. The foundation is proven and production-ready.

---

## Testing Strategy

### Unit Tests (Hooks & Utilities)

Test files live in `src/**/*.test.ts` or `src/**/*.test.tsx`. Use Bun's test runner (configured in `tsconfig.json`).

**Example: useAuth hook test**

```typescript
// src/features/auth/hooks/useAuth.test.ts
import { describe, it, expect, beforeEach } from 'bun:test'
import { renderHook, act } from '@testing-library/react'
import { useAuth } from './useAuth'

describe('useAuth', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('should restore session from localStorage', () => {
    const mockState = {
      isAuthenticated: true,
      user: { id: '1', email: 'test@example.com' },
      token: 'mock-token',
    }
    localStorage.setItem('authState', JSON.stringify(mockState))

    const { result } = renderHook(() => useAuth())

    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user?.email).toBe('test@example.com')
  })
})
```

### Manual Testing (UI Components & Flows)

For this scaffold, manual testing is primary (no component snapshot tests). Test in browser:

1. **Dev server**: `npm run dev` → browse app
2. **Each page**: Login → Dashboard → Settings → Logout
3. **Keyboard navigation**: Tab through all interactive elements
4. **Responsive**: Resize browser, check layout on desktop and mobile widths
5. **Console**: DevTools console should show zero errors

### Build Verification

```bash
npm run build     # Ensure production build succeeds
npm run preview   # Preview production build locally
```

## Performance Considerations

- **Code-splitting:** Pages are lazy-loaded; only the login bundle downloads initially. Dashboard, settings, etc. load on-demand.
- **Bundle size:** Target <200KB gzipped for initial load. Vite's tree-shaking and Tailwind purging keep it small.
- **Network requests:** Vite proxy (via `vite.config.ts`) routes `/api` requests to `http://localhost:8080`. Zero network latency in dev.

## Migration Notes

- **Existing landing page:** Currently at `public/index.html` (or similar). This React app replaces it entirely. Vite will serve `index.html` as the entry point.
- **Backend API:** Spring Boot runs on `http://localhost:8080`. Vite proxy forwards `/api/` requests there. No CORS setup needed in dev.
- **Static assets:** Place images, fonts, etc. in `public/` folder. Vite copies them as-is to `dist/` on build.

## References

- **Auth pattern:** Adapted from `src/lib/auth-guard.ts` (10x-CLI token refresh pattern)
- **API error handling:** Adapted from `src/lib/api-client.ts` (10x-CLI discriminated union pattern)
- **TypeScript config:** Copied from 10x-CLI `tsconfig.json` (strict mode)
- **React Router v6 docs:** https://reactrouter.com/
- **Tailwind CSS docs:** https://tailwindcss.com/
- **Radix UI docs:** https://www.radix-ui.com/

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands.

### Phase 1: Vite + TypeScript + Build Foundation

#### Automated

- [x] 1.1 Dependencies install without errors — 305799f
- [x] 1.2 Dev server starts successfully — 305799f
- [x] 1.3 TypeScript compiles with zero errors — 305799f
- [x] 1.4 ESLint passes all checks — 305799f
- [x] 1.5 Production build succeeds — 305799f
- [x] 1.6 Build output verified (dist/ contains expected files) — 305799f

#### Manual

- [x] 1.7 Dev server hot reload works — 305799f
- [x] 1.8 Tailwind CSS loads and styles app — 305799f
- [x] 1.9 No console errors — 305799f
- [x] 1.10 Production build is reasonable size (<5MB total) — 305799f

### Phase 2: Context API + Auth State Management

#### Automated

- [ ] 2.1 TypeScript compilation passes
- [ ] 2.2 ESLint passes all checks
- [ ] 2.3 App runs without errors

#### Manual

- [ ] 2.4 Login form renders correctly
- [ ] 2.5 Mock login flow works (context updates)
- [ ] 2.6 Auth token saved to cookies/localStorage
- [ ] 2.7 Session restored on page refresh
- [ ] 2.8 Logout clears session and cookies
- [ ] 2.9 Error messages display correctly
- [ ] 2.10 No console errors

### Phase 3: API Integration Layer

#### Automated

- [ ] 3.1 TypeScript compilation passes
- [ ] 3.2 ESLint passes all checks
- [ ] 3.3 App runs without errors

#### Manual

- [ ] 3.4 API client module created and exported
- [ ] 3.5 API domain modules (auth, tasks, household) created
- [ ] 3.6 Auth context updated to use real API client
- [ ] 3.7 Error handling works (network errors display)
- [ ] 3.8 Type safety verified (IDE shows correct types)

### Phase 4: UI Foundation (Radix + Tailwind)

#### Automated

- [ ] 4.1 TypeScript compilation passes
- [ ] 4.2 ESLint passes all checks
- [ ] 4.3 App runs without errors

#### Manual

- [ ] 4.4 Component showcase loads
- [ ] 4.5 Buttons display all variants and sizes correctly
- [ ] 4.6 Inputs work with labels, error states, help text
- [ ] 4.7 Dialog opens/closes correctly
- [ ] 4.8 Task card displays all information
- [ ] 4.9 All interactive elements keyboard-navigable
- [ ] 4.10 Accessibility verified (screen reader works)

### Phase 5: React Router + Protected Routes

#### Automated

- [ ] 5.1 TypeScript compilation passes
- [ ] 5.2 ESLint passes all checks
- [ ] 5.3 Production build succeeds
- [ ] 5.4 Build output is reasonable size

#### Manual

- [ ] 5.5 Login page loads at startup
- [ ] 5.6 Login flow navigates to dashboard
- [ ] 5.7 Sidebar navigation works
- [ ] 5.8 Hot reload preserves app state
- [ ] 5.9 Logout redirects to login
- [ ] 5.10 Session persists across page refresh
- [ ] 5.11 Lazy loading works (network tab shows chunks)
- [ ] 5.12 Protected routes redirect when logged out
- [ ] 5.13 No console errors on any page
