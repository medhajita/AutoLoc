import { useCallback, useEffect, useState } from 'react'
import { api, dateCourte, money, nbJours, notifyReservationsChanged } from '../api.js'
import { useList } from '../hooks.js'

export default function Dashboard({ goTo }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const reservations = useList('/reservations')
  const vehicules = useList('/vehicules')

  const load = useCallback(() => {
    api('/dashboard').then(setData).catch((e) => setError(e.message))
    reservations.reload()
  }, [reservations.reload]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(load, [load])

  const prix = Object.fromEntries(vehicules.items.map((v) => [v.idVehicule, Number(v.tarifJournalier)]))

  async function decide(r, statut) {
    try {
      await api(`/reservations/${r.idReservation}/statut?statut=${statut}`, { method: 'PATCH' })
      notifyReservationsChanged()
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  if (error && !data) return <p className="error">{error}</p>
  if (!data) return <p className="muted">Chargement…</p>

  const aTraiter = reservations.items.filter((r) => r.statut === 'EN_ATTENTE')
  const kpis = [
    ['Véhicules', data.vehicules, `${data.vehiculesDisponibles} disponibles`, ''],
    ['Réservations en attente', data.reservationsEnAttente, 'À confirmer', 'wait'],
    ['Réservations confirmées', data.reservationsConfirmees, `${data.contrats} contrat(s)`, 'ok'],
    ['Revenus encaissés', money(data.revenus), `${data.clients} clients`, ''],
  ]

  return (
    <>
      <div className="page-head">
        <div>
          <h2>Tableau de bord</h2>
          <div className="muted small">
            {new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
          </div>
        </div>
        <button className="primary" onClick={() => goTo('reservations')}>+ Nouvelle réservation</button>
      </div>
      {error && <p className="error">{error}</p>}

      <div className="kpis">
        {kpis.map(([label, value, note, tone]) => (
          <div className="kpi" key={label}>
            <span className="muted">{label}</span>
            <strong>{value}</strong>
            <span className={`note ${tone}`}>{note}</span>
          </div>
        ))}
      </div>

      <section className="panel">
        <h3>À traiter</h3>
        {aTraiter.length === 0 && <p className="muted small">Aucune réservation en attente.</p>}
        <div className="todo-grid">
          {aTraiter.map((r) => (
            <div className="todo" key={r.idReservation}>
              <div className="todo-head">
                <strong>{r.vehiculeLabel}</strong>
                <span className="badge EN_ATTENTE">EN ATTENTE</span>
              </div>
              <div className="muted small">
                {r.clientNom} · {dateCourte(r.dateDebut)} → {dateCourte(r.dateFin)} ·{' '}
                {money((prix[r.vehiculeId] ?? 0) * nbJours(r.dateDebut, r.dateFin))}
              </div>
              <div className="btn-row">
                <button className="success" onClick={() => decide(r, 'CONFIRMEE')}>Confirmer</button>
                <button className="danger-outline" onClick={() => decide(r, 'ANNULEE')}>Refuser</button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </>
  )
}
