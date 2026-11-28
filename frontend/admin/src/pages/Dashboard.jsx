import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, dateCourte, money, nbJours, notifyReservationsChanged } from '../api.js'
import { useList } from '../hooks.js'

const FENETRE = 35 // jours affichés dans le planning
const addDays = (d, n) => new Date(d.getTime() + n * 86400000)
const iso = (d) => d.toISOString().slice(0, 10)

function Planning({ vehicules, reservations }) {
  const debut = useMemo(() => {
    const d = new Date()
    d.setHours(0, 0, 0, 0)
    return d
  }, [])
  const fin = addDays(debut, FENETRE - 1)
  const pct = (d) => ((new Date(`${d}T00:00:00`) - debut) / 86400000 / FENETRE) * 100

  const bar = (r) => {
    const s = Math.max(0, pct(r.dateDebut))
    const e = Math.min(100, pct(r.dateFin) + 100 / FENETRE)
    return e > s ? { left: `${s}%`, width: `${e - s}%` } : null
  }

  return (
    <section className="panel span8">
      <div className="panel-head">
        <h3>Planning du parc</h3>
        <span className="muted small">
          {dateCourte(iso(debut))} → {dateCourte(iso(fin))}
        </span>
      </div>
      {vehicules.map((v) => {
        const bars = reservations
          .filter((r) => r.vehiculeId === v.idVehicule && (r.statut === 'EN_ATTENTE' || r.statut === 'CONFIRMEE'))
          .map((r) => ({ r, pos: bar(r) }))
          .filter((b) => b.pos)
        return (
          <div className="plan-row" key={v.idVehicule}>
            <div className="plan-name">{v.marque} {v.modele}</div>
            <div className="plan-track">
              {v.statut === 'MAINTENANCE' && (
                <div className="plan-bar maint" style={{ left: 0, width: '100%' }}>En maintenance</div>
              )}
              {bars.map(({ r, pos }) => (
                <div key={r.idReservation} className={`plan-bar ${r.statut}`} style={pos}>
                  {r.clientNom}
                </div>
              ))}
            </div>
          </div>
        )
      })}
      <div className="legend">
        <span><i className="dot ok" />Confirmée</span>
        <span><i className="dot wait" />En attente</span>
        <span><i className="dot maint" />Maintenance</span>
      </div>
    </section>
  )
}

export default function Dashboard({ goTo }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const reservations = useList('/reservations')
  const vehicules = useList('/vehicules')
  const [prix, setPrix] = useState({})

  const load = useCallback(() => {
    api('/dashboard').then(setData).catch((e) => setError(e.message))
    reservations.reload()
  }, [reservations.reload]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(load, [load])
  useEffect(() => {
    setPrix(Object.fromEntries(vehicules.items.map((v) => [v.idVehicule, Number(v.tarifJournalier)])))
  }, [vehicules.items])

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

      <div className="grid12">
        <Planning vehicules={vehicules.items} reservations={reservations.items} />
        <section className="panel span4">
          <h3>À traiter</h3>
          {aTraiter.length === 0 && <p className="muted small">Aucune réservation en attente.</p>}
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
        </section>
      </div>
    </>
  )
}
