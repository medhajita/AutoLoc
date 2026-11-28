import { Logo } from './Brand.jsx'

function initials(me) {
  return (me.username ?? '?').slice(0, 2).toUpperCase()
}

export default function Header({ me, view, go, onLogout }) {
  const link = (key, label) => (
    <a
      href={`#/${key}`}
      className={view === key ? 'active' : ''}
      onClick={(e) => {
        e.preventDefault()
        go(key)
      }}
    >
      {label}
    </a>
  )

  return (
    <header className="topbar">
      <div className="topbar-left">
        <Logo onClick={() => go('home')} />
        <nav>
          {link('home', 'Réserver')}
          {me && link('mes', 'Mes réservations')}
          {me && link('profil', 'Mon profil')}
        </nav>
      </div>
      <div className="topbar-right">
        {me ? (
          <>
            <span className="avatar">{initials(me)}</span>
            <span className="who">{me.username}</span>
            <button className="btn-text" onClick={onLogout}>Déconnexion</button>
          </>
        ) : (
          <>
            <button className="btn-text" onClick={() => go('login')}>Se connecter</button>
            <button className="btn-dark" onClick={() => go('register')}>Créer un compte</button>
          </>
        )}
      </div>
    </header>
  )
}
