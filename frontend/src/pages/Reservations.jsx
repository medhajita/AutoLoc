import { useState } from 'react'
import { api } from '../api.js'
import { useList } from '../hooks.js'

const EMPTY = { clientId: '', vehiculeId: '', dateDebut: '', dateFin: '' }

export default function Reservations({ isAdmin }) {
  const reservations = useList('/reservations')
  const clients = useList('/clients')
  const vehicules = useList('/vehicules')
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function run(action) {
    try {
      await action()
      setError('')
      reservations.reload()
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
    })
  }

  const statut = (r, s) => run(() => api(`/reservations/${r.idReservation}/statut?statut=${s}`, { method: 'PATCH' }))
  const creerContrat = (r) => run(() => api('/contrats', { method: 'POST', body: { reservationId: r.idReservation } }))
  const remove = (r) => confirm('Supprimer cette réservation ?') &&
    run(() => api(`/reservations/${r.idReservation}`, { method: 'DELETE' }))

  return (
    <>
      <h2>Réservations</h2>
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
        {error && <p className="error">{error}</p>}
        <div className="actions"><button className="primary">Réserver</button></div>
      </form>

      {reservations.error && <p className="error">{reservations.error}</p>}
      <table>
        <thead>
          <tr><th>#</th><th>Client</th><th>Véhicule</th><th>Période</th><th>Statut</th><th>Contrat</th><th /></tr>
        </thead>
        <tbody>
          {reservations.items.map((r) => (
            <tr key={r.idReservation}>
              <td>{r.idReservation}</td>
              <td>{r.clientNom}</td>
              <td>{r.vehiculeLabel}</td>
              <td>{r.dateDebut} → {r.dateFin}</td>
              <td><span className={`pill ${r.statut}`}>{r.statut}</span></td>
              <td>{r.contratId ? `#${r.contratId}` : '—'}</td>
              <td className="actions">
                {r.statut === 'EN_ATTENTE' && <button onClick={() => statut(r, 'CONFIRMEE')}>Confirmer</button>}
                {r.statut === 'CONFIRMEE' && !r.contratId && <button onClick={() => creerContrat(r)}>Créer le contrat</button>}
                {r.statut === 'CONFIRMEE' && <button onClick={() => statut(r, 'TERMINEE')}>Terminer</button>}
                {(r.statut === 'EN_ATTENTE' || r.statut === 'CONFIRMEE') && <button onClick={() => statut(r, 'ANNULEE')}>Annuler</button>}
                {isAdmin && <button className="danger" onClick={() => remove(r)}>Supprimer</button>}
              </td>
            </tr>
          ))}
          {!reservations.loading && reservations.items.length === 0 && <tr><td colSpan="7" className="muted">Aucune réservation</td></tr>}
        </tbody>
      </table>
    </>
  )
}
