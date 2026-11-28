import { useMemo, useState } from 'react'
import { api, dateCourte, money, nbJours, notifyReservationsChanged, STATUT } from '../api.js'
import { useList } from '../hooks.js'

const EMPTY = { clientId: '', vehiculeId: '', dateDebut: '', dateFin: '' }
const FILTRES = [
  ['TOUTES', 'Toutes', () => true],
  ['EN_ATTENTE', 'En attente', (r) => r.statut === 'EN_ATTENTE'],
  ['CONFIRMEE', 'Confirmées', (r) => r.statut === 'CONFIRMEE'],
  ['ANNULEE', 'Annulées', (r) => r.statut === 'ANNULEE'],
]

export default function Reservations({ isAdmin }) {
  const reservations = useList('/reservations')
  const clients = useList('/clients')
  const vehicules = useList('/vehicules')
  const [form, setForm] = useState(EMPTY)
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState('')
  const [filtre, setFiltre] = useState('TOUTES')
  const [recherche, setRecherche] = useState('')
  const [selectedId, setSelectedId] = useState(null)

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })
  const vehiculeParId = useMemo(
    () => Object.fromEntries(vehicules.items.map((v) => [v.idVehicule, v])),
    [vehicules.items],
  )
  const total = (r) => Number(vehiculeParId[r.vehiculeId]?.tarifJournalier ?? 0) * nbJours(r.dateDebut, r.dateFin)

  async function run(action) {
    try {
      await action()
      setError('')
      reservations.reload()
      notifyReservationsChanged()
    } catch (err) {
      setError(err.message)
    }
  }

  const submit = (e) => {
    e.preventDefault()
    run(async () => {
      await api('/reservations', {
        method: 'POST',
        body: { ...form, clientId: Number(form.clientId), vehiculeId: Number(form.vehiculeId) },
      })
      setForm(EMPTY)
      setShowForm(false)
    })
  }

  const statut = (r, s) => run(() => api(`/reservations/${r.idReservation}/statut?statut=${s}`, { method: 'PATCH' }))
  const creerContrat = (r) => run(() => api('/contrats', { method: 'POST', body: { reservationId: r.idReservation } }))
  const remove = (r) =>
    confirm('Supprimer définitivement cette réservation ?') &&
    run(async () => {
      await api(`/reservations/${r.idReservation}`, { method: 'DELETE' })
      setSelectedId(null)
    })

  const f = FILTRES.find(([k]) => k === filtre)[2]
  const q = recherche.trim().toLowerCase()
  const lignes = reservations.items
    .filter(f)
    .filter((r) => !q || `${r.clientNom} ${r.vehiculeLabel}`.toLowerCase().includes(q))
  const selected = reservations.items.find((r) => r.idReservation === selectedId)
  const aTraiter = reservations.items.filter((r) => r.statut === 'EN_ATTENTE').length

  return (
    <>
      <div className="page-head">
        <div>
          <h2>Réservations</h2>
          <div className="muted small">
            {reservations.items.length} réservation(s) · {aTraiter} à traiter
          </div>
        </div>
        <button className="primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Fermer' : '+ Nouvelle réservation'}
        </button>
      </div>

      {showForm && (
        <form className="card form" onSubmit={submit}>
          <h3>Nouvelle réservation</h3>
          <div className="row">
            <label>Client
              <select value={form.clientId} onChange={set('clientId')} required>
                <option value="">— choisir —</option>
                {clients.items.map((c) => <option key={c.idClient} value={c.idClient}>{c.prenom} {c.nom}</option>)}
              </select>
            </label>
            <label>Véhicule
              <select value={form.vehiculeId} onChange={set('vehiculeId')} required>
                <option value="">— choisir —</option>
                {vehicules.items.filter((v) => v.statut !== 'MAINTENANCE').map((v) => (
                  <option key={v.idVehicule} value={v.idVehicule}>{v.marque} {v.modele} ({v.immatriculation})</option>
                ))}
              </select>
            </label>
            <label>Début<input type="date" value={form.dateDebut} onChange={set('dateDebut')} required /></label>
            <label>Fin<input type="date" value={form.dateFin} onChange={set('dateFin')} required /></label>
          </div>
          <div className="actions"><button className="primary">Réserver</button></div>
        </form>
      )}

      <div className="toolbar">
        <input
          className="search"
          placeholder="Rechercher un client ou un véhicule"
          aria-label="Rechercher"
          value={recherche}
          onChange={(e) => setRecherche(e.target.value)}
        />
        {FILTRES.map(([key, label, fn]) => (
          <button key={key} className={key === filtre ? 'filter on' : 'filter'} onClick={() => setFiltre(key)}>
            {label} · {reservations.items.filter(fn).length}
          </button>
        ))}
      </div>

      {(error || reservations.error) && <p className="error">{error || reservations.error}</p>}

      <div className="grid12">
        <section className="panel flush span8">
          <div className="table-wrap">
            <table className="dense">
              <thead>
                <tr><th>N°</th><th>Client</th><th>Véhicule</th><th>Période</th><th className="num">Total</th><th>Statut</th></tr>
              </thead>
              <tbody>
                {lignes.map((r) => (
                  <tr
                    key={r.idReservation}
                    className={`${r.statut === 'EN_ATTENTE' ? 'row-wait' : ''} ${r.idReservation === selectedId ? 'row-sel' : ''}`}
                    onClick={() => setSelectedId(r.idReservation)}
                  >
                    <td className="muted">#{r.idReservation}</td>
                    <td><strong>{r.clientNom}</strong></td>
                    <td>{r.vehiculeLabel}</td>
                    <td>{dateCourte(r.dateDebut)} → {dateCourte(r.dateFin)}</td>
                    <td className="num"><strong>{money(total(r))}</strong></td>
                    <td><span className={`badge ${r.statut}`}>{STATUT[r.statut]}</span></td>
                  </tr>
                ))}
                {!reservations.loading && lignes.length === 0 && (
                  <tr><td colSpan="6" className="muted">Aucune réservation</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <aside className="panel span4">
          {!selected ? (
            <p className="muted small">Sélectionnez une réservation pour voir le détail et agir.</p>
          ) : (
            <>
              <div className="panel-head">
                <h3>Réservation #{selected.idReservation}</h3>
                <span className={`badge ${selected.statut}`}>{STATUT[selected.statut]}</span>
              </div>
              <dl className="details">
                <dt>Client</dt><dd><strong>{selected.clientNom}</strong></dd>
                <dt>Véhicule</dt><dd>{selected.vehiculeLabel}</dd>
                <dt>Période</dt><dd>{dateCourte(selected.dateDebut)} → {dateCourte(selected.dateFin)}</dd>
                <dt>Durée</dt>
                <dd>
                  {nbJours(selected.dateDebut, selected.dateFin)} j ×{' '}
                  {money(vehiculeParId[selected.vehiculeId]?.tarifJournalier)}
                </dd>
                <dt>Total</dt><dd><strong>{money(total(selected))}</strong></dd>
                <dt>Contrat</dt><dd>{selected.contratId ? `#${selected.contratId}` : '—'}</dd>
              </dl>
              <div className="btn-col">
                {selected.statut === 'EN_ATTENTE' && (
                  <button className="success" onClick={() => statut(selected, 'CONFIRMEE')}>Confirmer la réservation</button>
                )}
                {selected.statut === 'CONFIRMEE' && !selected.contratId && (
                  <button className="outline" onClick={() => creerContrat(selected)}>Créer le contrat</button>
                )}
                {selected.statut === 'CONFIRMEE' && (
                  <button className="outline" onClick={() => statut(selected, 'TERMINEE')}>Terminer la location</button>
                )}
                {(selected.statut === 'EN_ATTENTE' || selected.statut === 'CONFIRMEE') && (
                  <button className="danger-outline" onClick={() => statut(selected, 'ANNULEE')}>Annuler</button>
                )}
                {isAdmin && <button className="danger-outline" onClick={() => remove(selected)}>Supprimer définitivement</button>}
              </div>
              <p className="muted tiny">
                Le contrat ne peut être créé qu'une fois la réservation confirmée. La suppression définitive est réservée au rôle ADMIN.
              </p>
            </>
          )}
        </aside>
      </div>
    </>
  )
}
