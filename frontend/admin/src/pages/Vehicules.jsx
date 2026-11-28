import { useState } from 'react'
import { api, money } from '../api.js'
import { useList } from '../hooks.js'

const EMPTY = {
  immatriculation: '',
  marque: '',
  modele: '',
  categorie: 'CITADINE',
  tarifJournalier: '',
  statut: 'DISPONIBLE',
  agenceId: '',
}

export default function Vehicules({ isAdmin }) {
  const vehicules = useList('/vehicules')
  const agences = useList('/agences')
  const [form, setForm] = useState(EMPTY)
  const [editId, setEditId] = useState(null)
  const [error, setError] = useState('')

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    try {
      const body = {
        ...form,
        tarifJournalier: Number(form.tarifJournalier),
        agenceId: form.agenceId === '' ? null : Number(form.agenceId),
      }
      if (editId) await api(`/vehicules/${editId}`, { method: 'PUT', body })
      else await api('/vehicules', { method: 'POST', body })
      setForm(EMPTY)
      setEditId(null)
      setError('')
      vehicules.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  function edit(v) {
    setEditId(v.idVehicule)
    setForm({ ...v, agenceId: v.agenceId ?? '' })
  }

  async function remove(v) {
    if (!confirm(`Supprimer ${v.marque} ${v.modele} ?`)) return
    try {
      await api(`/vehicules/${v.idVehicule}`, { method: 'DELETE' })
      setError('')
      vehicules.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <h2>Véhicules</h2>
      <form className="card form" onSubmit={submit}>
        <h3>{editId ? 'Modifier le véhicule' : 'Nouveau véhicule'}</h3>
        <div className="row">
          <label>Immatriculation<input value={form.immatriculation} onChange={set('immatriculation')} required /></label>
          <label>Marque<input value={form.marque} onChange={set('marque')} required /></label>
          <label>Modèle<input value={form.modele} onChange={set('modele')} required /></label>
        </div>
        <div className="row">
          <label>Catégorie
            <select value={form.categorie} onChange={set('categorie')}>
              {['CITADINE', 'BERLINE', 'SUV', 'UTILITAIRE'].map((c) => <option key={c}>{c}</option>)}
            </select>
          </label>
          <label>Tarif / jour (€)<input type="number" min="0.01" step="0.01" value={form.tarifJournalier} onChange={set('tarifJournalier')} required /></label>
          <label>Statut
            <select value={form.statut} onChange={set('statut')}>
              {['DISPONIBLE', 'LOUE', 'MAINTENANCE'].map((c) => <option key={c}>{c}</option>)}
            </select>
          </label>
          <label>Agence
            <select value={form.agenceId} onChange={set('agenceId')}>
              <option value="">— aucune —</option>
              {agences.items.map((a) => <option key={a.idAgence} value={a.idAgence}>{a.nom}</option>)}
            </select>
          </label>
        </div>
        {error && <p className="error">{error}</p>}
        <div className="actions">
          <button className="primary">{editId ? 'Enregistrer' : 'Ajouter'}</button>
          {editId && <button type="button" onClick={() => { setEditId(null); setForm(EMPTY) }}>Annuler</button>}
        </div>
      </form>

      {vehicules.error && <p className="error">{vehicules.error}</p>}
      <table>
        <thead>
          <tr><th>Immatriculation</th><th>Véhicule</th><th>Catégorie</th><th>Tarif / jour</th><th>Statut</th><th>Agence</th><th /></tr>
        </thead>
        <tbody>
          {vehicules.items.map((v) => (
            <tr key={v.idVehicule}>
              <td>{v.immatriculation}</td>
              <td>{v.marque} {v.modele}</td>
              <td>{v.categorie}</td>
              <td>{money(v.tarifJournalier)}</td>
              <td><span className={`pill ${v.statut}`}>{v.statut}</span></td>
              <td>{v.agenceNom ?? '—'}</td>
              <td className="actions">
                <button onClick={() => edit(v)}>Modifier</button>
                {isAdmin && <button className="danger" onClick={() => remove(v)}>Supprimer</button>}
              </td>
            </tr>
          ))}
          {!vehicules.loading && vehicules.items.length === 0 && <tr><td colSpan="7" className="muted">Aucun véhicule</td></tr>}
        </tbody>
      </table>
    </>
  )
}
