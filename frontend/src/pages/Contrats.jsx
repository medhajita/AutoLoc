import { useState } from 'react'
import { api, money } from '../api.js'
import { useList } from '../hooks.js'

function PaiementForm({ contrat, onDone, onError }) {
  const [montant, setMontant] = useState('')
  const [mode, setMode] = useState('CARTE_BANCAIRE')

  async function submit(e) {
    e.preventDefault()
    try {
      await api(`/contrats/${contrat.idContrat}/paiements`, {
        method: 'POST',
        body: { montant: Number(montant), modePaiement: mode },
      })
      setMontant('')
      onDone()
    } catch (err) {
      onError(err.message)
    }
  }

  return (
    <form className="inline" onSubmit={submit}>
      <input type="number" min="0.01" step="0.01" placeholder="Montant (€)" value={montant}
        onChange={(e) => setMontant(e.target.value)} required />
      <select value={mode} onChange={(e) => setMode(e.target.value)}>
        {['CARTE_BANCAIRE', 'ESPECES', 'VIREMENT', 'CHEQUE'].map((m) => <option key={m}>{m}</option>)}
      </select>
      <button className="primary">Ajouter un paiement</button>
    </form>
  )
}

export default function Contrats({ isAdmin }) {
  const contrats = useList('/contrats')
  const [error, setError] = useState('')

  async function remove(path, message) {
    if (!confirm(message)) return
    try {
      await api(path, { method: 'DELETE' })
      setError('')
      contrats.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <h2>Contrats & paiements</h2>
      <p className="muted">Les contrats se créent depuis une réservation confirmée.</p>
      {(error || contrats.error) && <p className="error">{error || contrats.error}</p>}
      {contrats.items.map((c) => {
        const reste = Number(c.montantTotal) - Number(c.totalPaye)
        return (
          <div className="card" key={c.idContrat}>
            <div className="card-head">
              <div>
                <strong>Contrat #{c.idContrat}</strong>
                <div className="muted">{c.clientNom} — {c.vehiculeLabel} — signé le {c.dateSignature}</div>
              </div>
              <div className="right">
                <div>Total : <strong>{money(c.montantTotal)}</strong></div>
                <div>Payé : {money(c.totalPaye)} · Reste : <strong className={reste > 0 ? 'warn' : 'ok'}>{money(reste)}</strong></div>
              </div>
            </div>
            <table className="sub">
              <tbody>
                {c.paiements.map((p) => (
                  <tr key={p.idPaiement}>
                    <td>{new Date(p.datePaiement).toLocaleDateString('fr-FR')}</td>
                    <td>{p.modePaiement}</td>
                    <td>{money(p.montant)}</td>
                    <td className="actions">
                      <button className="danger" onClick={() => remove(`/contrats/${c.idContrat}/paiements/${p.idPaiement}`, 'Supprimer ce paiement ?')}>Retirer</button>
                    </td>
                  </tr>
                ))}
                {c.paiements.length === 0 && <tr><td colSpan="4" className="muted">Aucun paiement</td></tr>}
              </tbody>
            </table>
            <PaiementForm contrat={c} onDone={() => { setError(''); contrats.reload() }} onError={setError} />
            {isAdmin && (
              <div className="actions">
                <button className="danger" onClick={() => remove(`/contrats/${c.idContrat}`, 'Supprimer le contrat et ses paiements ?')}>Supprimer le contrat</button>
              </div>
            )}
          </div>
        )
      })}
      {!contrats.loading && contrats.items.length === 0 && <p className="muted">Aucun contrat</p>}
    </>
  )
}
