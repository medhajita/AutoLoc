import { useState } from 'react'
import { api } from '../api.js'
import { useList } from '../hooks.js'

const EMPTY = { nom: '', prenom: '', email: '', telephone: '', numPermis: '' }

export default function Clients({ isAdmin }) {
  const clients = useList('/clients')
  const [form, setForm] = useState(EMPTY)
  const [editId, setEditId] = useState(null)
  const [error, setError] = useState('')

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    try {
      if (editId) await api(`/clients/${editId}`, { method: 'PUT', body: form })
      else await api('/clients', { method: 'POST', body: form })
      setForm(EMPTY)
      setEditId(null)
      setError('')
      clients.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  async function remove(c) {
    if (!confirm(`Supprimer ${c.prenom} ${c.nom} ?`)) return
    try {
      await api(`/clients/${c.idClient}`, { method: 'DELETE' })
      setError('')
      clients.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <h2>Clients</h2>
      <form className="card form" onSubmit={submit}>
        <h3>{editId ? 'Modifier le client' : 'Nouveau client'}</h3>
        <div className="row">
          <label>Nom<input value={form.nom} onChange={set('nom')} required /></label>
          <label>Prénom<input value={form.prenom} onChange={set('prenom')} required /></label>
          <label>Email<input type="email" value={form.email} onChange={set('email')} required /></label>
        </div>
        <div className="row">
          <label>Téléphone<input value={form.telephone ?? ''} onChange={set('telephone')} /></label>
          <label>N° de permis<input value={form.numPermis} onChange={set('numPermis')} required /></label>
        </div>
        {error && <p className="error">{error}</p>}
        <div className="actions">
          <button className="primary">{editId ? 'Enregistrer' : 'Ajouter'}</button>
          {editId && <button type="button" onClick={() => { setEditId(null); setForm(EMPTY) }}>Annuler</button>}
        </div>
      </form>

      {clients.error && <p className="error">{clients.error}</p>}
      <table>
        <thead>
          <tr><th>Client</th><th>Email</th><th>Téléphone</th><th>Permis</th><th>Inscrit le</th><th /></tr>
        </thead>
        <tbody>
          {clients.items.map((c) => (
            <tr key={c.idClient}>
              <td>{c.prenom} {c.nom}</td>
              <td>{c.email}</td>
              <td>{c.telephone ?? '—'}</td>
              <td>{c.numPermis}</td>
              <td>{c.dateInscription}</td>
              <td className="actions">
                <button onClick={() => { setEditId(c.idClient); setForm({ ...c, telephone: c.telephone ?? '' }) }}>Modifier</button>
                {isAdmin && <button className="danger" onClick={() => remove(c)}>Supprimer</button>}
              </td>
            </tr>
          ))}
          {!clients.loading && clients.items.length === 0 && <tr><td colSpan="6" className="muted">Aucun client</td></tr>}
        </tbody>
      </table>
    </>
  )
}
