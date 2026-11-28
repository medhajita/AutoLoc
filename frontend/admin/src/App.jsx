import { useCallback, useEffect, useState } from 'react'
import { api, clearAuth, hasAuth, setAuth } from './api.js'
import Dashboard from './pages/Dashboard.jsx'
import Vehicules from './pages/Vehicules.jsx'
import Clients from './pages/Clients.jsx'
import Reservations from './pages/Reservations.jsx'
import Contrats from './pages/Contrats.jsx'

const PAGES = [
  ['dashboard', 'Tableau de bord', Dashboard],
  ['reservations', 'Réservations', Reservations],
  ['contrats', 'Contrats & paiements', Contrats],
  ['vehicules', 'Véhicules', Vehicules],
  ['clients', 'Clients', Clients],
]

function Brand() {
  return (
    <div className="brand">
      Auto<span>Loc</span> <small>ADMIN</small>
    </div>
  )
}

function Login({ onLogin, notice }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    try {
      const me = await api('/me', { auth: btoa(`${username}:${password}`) })
      if (!me.roles.some((r) => r === 'ROLE_ADMIN' || r === 'ROLE_USER')) {
        throw new Error('Ce compte est un compte client : rendez-vous sur /client/')
      }
      setAuth(username, password)
      onLogin(me)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login">
      <form className="card login-card" onSubmit={submit}>
        <div className="brand dark-text">
          Auto<span>Loc</span> <small>ADMIN</small>
        </div>
        <p className="muted">Connexion au back-office</p>
        {notice && <p className="error">{notice}</p>}
        <label>
          Utilisateur
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
        </label>
        <label>
          Mot de passe
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        {error && <p className="error">{error}</p>}
        <button className="primary" disabled={busy}>Se connecter</button>
      </form>
    </div>
  )
}

export default function App() {
  const [me, setMe] = useState(null)
  const [page, setPage] = useState('dashboard')
  const [checking, setChecking] = useState(hasAuth())
  const [pending, setPending] = useState(0)

  useEffect(() => {
    if (!hasAuth()) return
    api('/me')
      .then(setMe)
      .catch(() => clearAuth())
      .finally(() => setChecking(false))
  }, [])

  useEffect(() => {
    const expired = () => setMe(null)
    window.addEventListener('auth-expired', expired)
    return () => window.removeEventListener('auth-expired', expired)
  }, [])

  const refreshBadge = useCallback(() => {
    api('/dashboard')
      .then((d) => setPending(d.reservationsEnAttente ?? 0))
      .catch(() => {})
  }, [])

  useEffect(() => {
    if (!me) return
    refreshBadge()
    window.addEventListener('reservations-changed', refreshBadge)
    return () => window.removeEventListener('reservations-changed', refreshBadge)
  }, [me, refreshBadge])

  if (checking) return <p className="muted center">Chargement…</p>
  if (!me) return <Login onLogin={setMe} />

  const logout = () => {
    clearAuth()
    setMe(null)
  }

  const isAdmin = me.roles.includes('ROLE_ADMIN')
  const Current = PAGES.find(([key]) => key === page)[2]

  return (
    <div className="shell">
      <aside className="sidebar">
        <Brand />
        <nav>
          {PAGES.map(([key, label]) => (
            <button key={key} className={key === page ? 'nav on' : 'nav'} onClick={() => setPage(key)}>
              <span>{label}</span>
              {key === 'reservations' && pending > 0 && <span className="count">{pending}</span>}
            </button>
          ))}
        </nav>
        <div className="me">
          <span className="me-avatar">{me.username.slice(0, 1).toUpperCase()}</span>
          <div>
            <strong>{me.username}</strong>
            <div>{isAdmin ? 'Administrateur' : 'Employé'}</div>
          </div>
          <button className="me-out" onClick={logout} title="Déconnexion">Quitter</button>
        </div>
      </aside>
      <main className="content">
        <Current isAdmin={isAdmin} goTo={setPage} />
      </main>
    </div>
  )
}
