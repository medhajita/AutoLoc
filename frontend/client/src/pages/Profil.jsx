import { useEffect, useState } from 'react'
import { api, dateFr } from '../api.js'

export default function Profil() {
  const [p, setP] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/espace/moi').then(setP).catch((e) => setError(e.message))
  }, [])

  return (
    <main className="page narrow">
      <h1>Mon profil</h1>
      {error && <p className="msg-error">{error}</p>}
      {!p && !error && <p className="muted">Chargement…</p>}
      {p && (
        <div className="panel">
          <h2>
            {p.prenom} {p.nom}
          </h2>
          <p className="muted">
            {p.email}
            {p.telephone ? ` · ${p.telephone}` : ''}
          </p>
          <p>Permis : {p.numPermis}</p>
          <p className="muted">Inscrit le {dateFr(p.dateInscription)}</p>
        </div>
      )}
    </main>
  )
}
