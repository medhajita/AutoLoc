import { useCallback, useEffect, useState } from 'react'
import { api, money } from '../api.js'

const today = () => new Date().toISOString().slice(0, 10)
const days = (d1, d2) => Math.round((new Date(d2) - new Date(d1)) / 86400000) + 1
const STATUT = { EN_ATTENTE: 'En attente', CONFIRMEE: 'Confirmée', ANNULEE: 'Annulée', TERMINEE: 'Terminée' }

function Reserver({ onDone }) {
  const [debut, setDebut] = useState('')
  const [fin, setFin] = useState('')
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const datesOk = debut && fin && fin >= debut

  useEffect(() => {
    const query = datesOk ? `?debut=${debut}&fin=${fin}` : ''
    api(`/espace/vehicules${query}`)
      .then((v) => {
        setItems(v)
        setError('')
      })
      .catch((e) => setError(e.message))
  }, [debut, fin, datesOk])

  async function reserver(v) {
    setBusy(true)
    try {
      await api('/espace/reservations', {
        method: 'POST',
        body: { vehiculeId: v.idVehicule, dateDebut: debut, dateFin: fin },
      })
      onDone()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h2>Réserver un véhicule</h2>
      <div className="card form">
        <div className="row">
          <label>Du
            <input type="date" min={today()} value={debut} onChange={(e) => setDebut(e.target.value)} />
          </label>
          <label>Au
            <input type="date" min={debut || today()} value={fin} onChange={(e) => setFin(e.target.value)} />
          </label>
        </div>
        {!datesOk && <p className="muted">Choisissez vos dates pour voir les véhicules disponibles et le prix.</p>}
        {debut && fin && fin < debut && <p className="error">La date de fin précède la date de début.</p>}
        {error && <p className="error">{error}</p>}
      </div>
      <div className="grid wide">
        {items.map((v) => (
          <div className="card" key={v.idVehicule}>
            <div className="card-head">
              <h3>{v.marque} {v.modele}</h3>
              <span className="pill">{v.categorie}</span>
            </div>
            <p className="muted">{v.agenceNom ?? 'Agence AutoLoc'}</p>
            <p><strong>{money(v.tarifJournalier)}</strong> / jour</p>
            {datesOk && (
              <p>Total {days(debut, fin)} j : <strong>{money(v.tarifJournalier * days(debut, fin))}</strong></p>
            )}
            <button
              className="primary"
              disabled={!datesOk || !v.disponible || busy}
              onClick={() => reserver(v)}
            >
              {datesOk && !v.disponible ? 'Indisponible' : 'Réserver'}
            </button>
          </div>
        ))}
      </div>
    </>
  )
}

function MesReservations({ notice }) {
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(null)
  const [dates, setDates] = useState({ dateDebut: '', dateFin: '' })

  const load = useCallback(() => {
    api('/espace/reservations').then(setItems).catch((e) => setError(e.message))
  }, [])
  useEffect(load, [load])

  async function run(action) {
    try {
      await action()
      setError('')
      setEditing(null)
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  const annuler = (r) =>
    confirm('Annuler cette réservation ?') &&
    run(() => api(`/espace/reservations/${r.idReservation}/annuler`, { method: 'PATCH' }))
  const enregistrer = (r) =>
    run(() => api(`/espace/reservations/${r.idReservation}`, { method: 'PUT', body: dates }))

  return (
    <>
      <h2>Mes réservations</h2>
      {notice && <p className="ok">{notice}</p>}
      {error && <p className="error">{error}</p>}
      {items.length === 0 ? (
        <p className="muted">Vous n'avez pas encore de réservation.</p>
      ) : (
        <table>
          <thead>
            <tr><th>Véhicule</th><th>Période</th><th>Durée</th><th>Total</th><th>Statut</th><th></th></tr>
          </thead>
          <tbody>
            {items.map((r) => (
              <tr key={r.idReservation}>
                <td>{r.vehiculeLabel}</td>
                <td>
                  {editing === r.idReservation ? (
                    <span className="inline">
                      <input type="date" min={today()} value={dates.dateDebut}
                        onChange={(e) => setDates({ ...dates, dateDebut: e.target.value })} />
                      <input type="date" min={dates.dateDebut || today()} value={dates.dateFin}
                        onChange={(e) => setDates({ ...dates, dateFin: e.target.value })} />
                    </span>
                  ) : (
                    <>{r.dateDebut} → {r.dateFin}</>
                  )}
                </td>
                <td>{r.nbJours} j</td>
                <td>{money(r.total)}</td>
                <td><span className={`pill ${r.statut}`}>{STATUT[r.statut]}</span></td>
                <td className="actions">
                  {editing === r.idReservation ? (
                    <>
                      <button className="primary" onClick={() => enregistrer(r)}>Enregistrer</button>
                      <button onClick={() => setEditing(null)}>Annuler</button>
                    </>
                  ) : (
                    <>
                      {r.modifiable && (
                        <button onClick={() => {
                          setEditing(r.idReservation)
                          setDates({ dateDebut: r.dateDebut, dateFin: r.dateFin })
                        }}>Modifier les dates</button>
                      )}
                      {r.annulable && <button className="danger" onClick={() => annuler(r)}>Annuler</button>}
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}

function Profil() {
  const [p, setP] = useState(null)
  useEffect(() => {
    api('/espace/moi').then(setP).catch(() => {})
  }, [])
  if (!p) return <p className="muted">Chargement…</p>
  return (
    <>
      <h2>Mon profil</h2>
      <div className="card">
        <p><strong>{p.prenom} {p.nom}</strong></p>
        <p className="muted">{p.email}{p.telephone ? ` · ${p.telephone}` : ''}</p>
        <p>Permis : {p.numPermis}</p>
        <p className="muted">Inscrit le {p.dateInscription}</p>
      </div>
    </>
  )
}

export default function EspaceClient({ me, onLogout }) {
  const [tab, setTab] = useState('reserver')
  const [notice, setNotice] = useState('')

  return (
    <div className="layout">
      <header>
        <strong className="brand">AutoLoc</strong>
        <nav>
          <button className={tab === 'reserver' ? 'tab active' : 'tab'} onClick={() => setTab('reserver')}>
            Réserver
          </button>
          <button className={tab === 'mes' ? 'tab active' : 'tab'} onClick={() => { setNotice(''); setTab('mes') }}>
            Mes réservations
          </button>
          <button className={tab === 'profil' ? 'tab active' : 'tab'} onClick={() => setTab('profil')}>
            Mon profil
          </button>
        </nav>
        <div className="user">
          <span>{me.username} <span className="badge">CLIENT</span></span>
          <button onClick={onLogout}>Déconnexion</button>
        </div>
      </header>
      <main>
        {tab === 'reserver' && (
          <Reserver onDone={() => { setNotice('Réservation enregistrée, en attente de confirmation par l\'agence.'); setTab('mes') }} />
        )}
        {tab === 'mes' && <MesReservations notice={notice} />}
        {tab === 'profil' && <Profil />}
      </main>
    </div>
  )
}
