import { useCallback, useEffect, useState } from 'react'
import { api, dateFr, money, today } from '../api.js'

const STATUT = { EN_ATTENTE: 'En attente', CONFIRMEE: 'Confirmée', ANNULEE: 'Annulée', TERMINEE: 'Terminée' }
const ONGLETS = [
  ['avenir', 'À venir', (r) => r.statut === 'EN_ATTENTE' || r.statut === 'CONFIRMEE'],
  ['terminees', 'Terminées', (r) => r.statut === 'TERMINEE'],
  ['annulees', 'Annulées', (r) => r.statut === 'ANNULEE'],
]

export default function MesReservations({ notice }) {
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const [onglet, setOnglet] = useState('avenir')
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
  const enregistrer = () =>
    run(() => api(`/espace/reservations/${editing.idReservation}`, { method: 'PUT', body: dates }))

  const filtre = ONGLETS.find(([k]) => k === onglet)[2]
  const visibles = items.filter(filtre)

  return (
    <main className="page two-cols">
      <div className="col-main">
        <div>
          <h1>Mes réservations</h1>
          <p className="muted">Modifiez les dates ou annulez tant que la location n'a pas commencé.</p>
        </div>
        {notice && <p className="msg-ok">{notice}</p>}
        {error && <p className="msg-error">{error}</p>}
        <div className="chips">
          {ONGLETS.map(([key, label, f]) => (
            <button key={key} className={key === onglet ? 'chip on' : 'chip'} onClick={() => setOnglet(key)}>
              {label} · {items.filter(f).length}
            </button>
          ))}
        </div>
        {visibles.length === 0 && <p className="muted">Aucune réservation dans cette catégorie.</p>}
        {visibles.map((r) => (
          <article className="res-card" key={r.idReservation}>
            <div className="res-top">
              <div className="res-info">
                <div className="res-title">
                  <h2>{r.vehiculeLabel}</h2>
                  <span className={`pill st-${r.statut}`}>{STATUT[r.statut]}</span>
                </div>
                <div className="muted">
                  Du {dateFr(r.dateDebut)} au {dateFr(r.dateFin)} · {r.nbJours} jour{r.nbJours > 1 ? 's' : ''}
                </div>
              </div>
              <div className="res-total">
                <div className="muted small">Total</div>
                <div className="total">{money(r.total)}</div>
              </div>
            </div>
            <div className="res-actions">
              {r.modifiable && (
                <button
                  className="btn-outline"
                  onClick={() => {
                    setEditing(r)
                    setDates({ dateDebut: r.dateDebut, dateFin: r.dateFin })
                  }}
                >
                  Modifier les dates
                </button>
              )}
              {r.annulable && (
                <button className="btn-outline danger" onClick={() => annuler(r)}>
                  Annuler
                </button>
              )}
              {!r.modifiable && !r.annulable && r.statut !== 'ANNULEE' && r.statut !== 'TERMINEE' && (
                <span className="muted small">Contrat signé : contactez l’agence pour toute modification.</span>
              )}
              {r.statut === 'EN_ATTENTE' && <span className="muted small">En attente de confirmation par l’agence.</span>}
            </div>
          </article>
        ))}
      </div>

      <aside className="col-side">
        {editing && (
          <div className="panel">
            <h3>Modifier les dates</h3>
            <div className="muted small">{editing.vehiculeLabel}</div>
            <label>
              Du
              <input type="date" min={today()} value={dates.dateDebut} onChange={(e) => setDates({ ...dates, dateDebut: e.target.value })} />
            </label>
            <label>
              Au
              <input type="date" min={dates.dateDebut || today()} value={dates.dateFin} onChange={(e) => setDates({ ...dates, dateFin: e.target.value })} />
            </label>
            <button className="btn-primary wide" onClick={enregistrer}>Enregistrer</button>
            <button className="btn-text" onClick={() => setEditing(null)}>Fermer</button>
          </div>
        )}
        <div className="panel dark">
          <h3>Besoin d'aide ?</h3>
          <p>Une réservation avec contrat signé ne peut plus être annulée en ligne : contactez votre agence.</p>
        </div>
      </aside>
    </main>
  )
}
