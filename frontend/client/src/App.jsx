import { useEffect, useState } from 'react'
import { api, clearAuth, hasAuth } from './api.js'
import Header from './components/Header.jsx'
import Accueil from './pages/Accueil.jsx'
import Auth from './pages/Auth.jsx'
import MesReservations from './pages/MesReservations.jsx'
import Profil from './pages/Profil.jsx'

export default function App() {
  const [me, setMe] = useState(null)
  const [checking, setChecking] = useState(hasAuth())
  const [view, setView] = useState('home')
  const [dates, setDates] = useState({ debut: '', fin: '' })
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (!hasAuth()) return
    api('/me')
      .then(setMe)
      .catch(() => clearAuth())
      .finally(() => setChecking(false))
  }, [])

  useEffect(() => {
    const expired = () => {
      setMe(null)
      setView('login')
    }
    window.addEventListener('auth-expired', expired)
    return () => window.removeEventListener('auth-expired', expired)
  }, [])

  const go = (v) => {
    setNotice('')
    setView(v)
    window.scrollTo(0, 0)
  }
  const logout = () => {
    clearAuth()
    setMe(null)
    go('home')
  }

  if (checking) return <p className="muted center">Chargement…</p>

  // Le site client est réservé aux comptes CLIENT ; le personnel utilise /admin/.
  const staff = me && !me.roles.includes('ROLE_CLIENT')

  let page
  if (staff) {
    page = (
      <main className="page narrow">
        <h1>Espace réservé aux clients</h1>
        <p className="muted">
          Le compte « {me.username} » est un compte du personnel. Utilisez le back-office : <a href="/admin/">/admin/</a>.
        </p>
      </main>
    )
  } else if (view === 'login' || view === 'register') {
    page = (
      <Auth
        mode={view}
        go={go}
        onLogin={(user) => {
          setMe(user)
          go('home')
        }}
      />
    )
  } else if (me && view === 'mes') {
    page = <MesReservations notice={notice} />
  } else if (me && view === 'profil') {
    page = <Profil />
  } else {
    page = (
      <Accueil
        me={me}
        go={go}
        dates={dates}
        setDates={setDates}
        onReserved={() => {
          setView('mes')
          setNotice("Réservation enregistrée, en attente de confirmation par l'agence.")
        }}
      />
    )
  }

  return (
    <>
      <Header me={me} view={view} go={go} onLogout={logout} />
      {page}
    </>
  )
}
