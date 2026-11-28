import { useEffect, useState } from 'react'
import { api, clearAuth, hasAuth, setAuth } from './api.js'
import Dashboard from './pages/Dashboard.jsx'
import Vehicules from './pages/Vehicules.jsx'
import Clients from './pages/Clients.jsx'
import Reservations from './pages/Reservations.jsx'
import Contrats from './pages/Contrats.jsx'
import EspaceClient from './espace/EspaceClient.jsx'

const PAGES = [
  ['dashboard', 'Tableau de bord', Dashboard],
  ['vehicules', 'Véhicules', Vehicules],
  ['clients', 'Clients', Clients],
  ['reservations', 'Réservations', Reservations],
  ['contrats', 'Contrats & paiements', Contrats],
]

const EMPTY_REGISTER = { prenom: '', nom: '', email: '', telephone: '', numPermis: '', motDePasse: '' }

function Login({ onLogin }) {
  const [mode, setMode] = useState('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [reg, setReg] = useState(EMPTY_REGISTER)
  const [error, setError] = useState('')

  async function connect(user, pass) {
    const auth = btoa(`${user}:${pass}`)
    const me = await api('/me', { auth })
    setAuth(user, pass)
    onLogin(me)
  }

  async function submit(e) {
    e.preventDefault()
    try {
      if (mode === 'login') {
        await connect(username, password)
      } else {
        await api('/espace/inscription', { method: 'POST', body: reg, anonymous: true })
        await connect(reg.email.trim().toLowerCase(), reg.motDePasse)
      }
    } catch (err) {
      setError(err.message)
    }
  }

  const setR = (k) => (e) => setReg({ ...reg, [k]: e.target.value })
  const switchMode = (m) => {
    setMode(m)
    setError('')
  }

  return (
    <div className="login">
      <form className="card" onSubmit={submit}>
        <h1>AutoLoc</h1>
        <p className="muted">
          {mode === 'login' ? 'Connexion à la plateforme de location' : 'Créer votre compte client'}
        </p>
        {mode === 'login' ? (
          <>
            <label>
              Utilisateur ou email
              <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
            </label>
            <label>
              Mot de passe
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            </label>
          </>
        ) : (
          <>
            <label>Prénom<input value={reg.prenom} onChange={setR('prenom')} autoFocus required /></label>
            <label>Nom<input value={reg.nom} onChange={setR('nom')} required /></label>
            <label>Email<input type="email" value={reg.email} onChange={setR('email')} required /></label>
            <label>Téléphone<input value={reg.telephone} onChange={setR('telephone')} /></label>
            <label>N° de permis<input value={reg.numPermis} onChange={setR('numPermis')} required /></label>
            <label>
              Mot de passe (8 caractères min.)
              <input type="password" minLength={8} value={reg.motDePasse} onChange={setR('motDePasse')} required />
            </label>
          </>
        )}
        {error && <p className="error">{error}</p>}
        <button className="primary">{mode === 'login' ? 'Se connecter' : 'Créer mon compte'}</button>
        <button type="button" className="link" onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}>
          {mode === 'login' ? 'Nouveau client ? Créer un compte' : 'Déjà un compte ? Se connecter'}
        </button>
      </form>
    </div>
  )
}

export default function App() {
  const [me, setMe] = useState(null)
  const [page, setPage] = useState('dashboard')
  const [checking, setChecking] = useState(hasAuth())

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

  if (checking) return <p className="muted center">Chargement…</p>
  if (!me) return <Login onLogin={setMe} />

  const logout = () => {
    clearAuth()
    setMe(null)
  }

  if (me.roles.includes('ROLE_CLIENT')) return <EspaceClient me={me} onLogout={logout} />

  const isAdmin = me.roles.includes('ROLE_ADMIN')
  const Current = PAGES.find(([key]) => key === page)[2]

  return (
    <div className="layout">
      <header>
        <strong className="brand">AutoLoc</strong>
        <nav>
          {PAGES.map(([key, label]) => (
            <button key={key} className={key === page ? 'tab active' : 'tab'} onClick={() => setPage(key)}>
              {label}
            </button>
          ))}
        </nav>
        <div className="user">
          <span>
            {me.username} <span className="badge">{isAdmin ? 'ADMIN' : 'USER'}</span>
          </span>
          <button onClick={logout}>Déconnexion</button>
        </div>
      </header>
      <main>
        <Current isAdmin={isAdmin} />
      </main>
    </div>
  )
}
