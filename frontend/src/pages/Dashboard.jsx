import { useEffect, useState } from 'react'
import { api, money } from '../api.js'

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/dashboard').then(setData).catch((e) => setError(e.message))
  }, [])

  if (error) return <p className="error">{error}</p>
  if (!data) return <p className="muted">Chargement…</p>

  const cards = [
    ['Véhicules', data.vehicules],
    ['Disponibles', data.vehiculesDisponibles],
    ['Clients', data.clients],
    ['Réservations en attente', data.reservationsEnAttente],
    ['Réservations confirmées', data.reservationsConfirmees],
    ['Contrats', data.contrats],
    ['Revenus encaissés', money(data.revenus)],
  ]

  return (
    <>
      <h2>Tableau de bord</h2>
      <div className="grid">
        {cards.map(([label, value]) => (
          <div className="card stat" key={label}>
            <span className="muted">{label}</span>
            <strong>{value}</strong>
          </div>
        ))}
      </div>
    </>
  )
}
