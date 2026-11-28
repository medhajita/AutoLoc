import { useState } from 'react'
import { api, setAuth } from '../api.js'

const EMPTY = { prenom: '', nom: '', email: '', telephone: '', numPermis: '', motDePasse: '' }

export default function Auth({ mode, go, onLogin }) {
  const register = mode === 'register'
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [reg, setReg] = useState(EMPTY)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function connect(user, pass) {
    const me = await api('/me', { auth: btoa(`${user}:${pass}`) })
    setAuth(user, pass)
    onLogin(me)
  }

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      if (register) {
        await api('/espace/inscription', { method: 'POST', body: reg, anonymous: true })
        await connect(reg.email.trim().toLowerCase(), reg.motDePasse)
      } else {
        await connect(username, password)
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const set = (k) => (e) => setReg({ ...reg, [k]: e.target.value })

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={submit}>
        <h1>{register ? 'Créer votre compte' : 'Connexion'}</h1>
        <p className="muted">
          {register ? 'Réservez et gérez vos locations en ligne.' : 'Retrouvez vos réservations.'}
        </p>
        {register ? (
          <>
            <div className="two">
              <label>Prénom<input value={reg.prenom} onChange={set('prenom')} autoFocus required /></label>
              <label>Nom<input value={reg.nom} onChange={set('nom')} required /></label>
            </div>
            <label>Email<input type="email" value={reg.email} onChange={set('email')} required /></label>
            <div className="two">
              <label>Téléphone<input value={reg.telephone} onChange={set('telephone')} /></label>
              <label>N° de permis<input value={reg.numPermis} onChange={set('numPermis')} required /></label>
            </div>
            <label>
              Mot de passe (8 caractères min.)
              <input type="password" minLength={8} value={reg.motDePasse} onChange={set('motDePasse')} required />
            </label>
          </>
        ) : (
          <>
            <label>
              Email
              <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
            </label>
            <label>
              Mot de passe
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            </label>
          </>
        )}
        {error && <p className="msg-error">{error}</p>}
        <button className="btn-primary wide" disabled={busy}>
          {register ? 'Créer mon compte' : 'Se connecter'}
        </button>
        <button type="button" className="btn-text" onClick={() => go(register ? 'login' : 'register')}>
          {register ? 'Déjà un compte ? Se connecter' : 'Nouveau client ? Créer un compte'}
        </button>
      </form>
    </div>
  )
}
