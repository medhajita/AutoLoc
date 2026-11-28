const KEY = 'autoloc_admin_auth'

export const hasAuth = () => sessionStorage.getItem(KEY) !== null
export const setAuth = (user, pass) => sessionStorage.setItem(KEY, btoa(`${user}:${pass}`))
export const clearAuth = () => sessionStorage.removeItem(KEY)

export async function api(path, { method = 'GET', body, auth } = {}) {
  const token = auth ?? sessionStorage.getItem(KEY)
  const res = await fetch(`/api${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest',
      ...(token ? { Authorization: `Basic ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (res.status === 401) {
    if (!auth) window.dispatchEvent(new Event('auth-expired'))
    throw new Error('Identifiants invalides ou session expirée')
  }
  if (res.status === 403) throw new Error('Action réservée à un administrateur')
  if (!res.ok) {
    let message = `Erreur ${res.status}`
    try {
      const data = await res.json()
      if (data.message) message = data.message
    } catch {
      /* corps vide */
    }
    throw new Error(message)
  }
  return res.status === 204 ? null : res.json()
}

export const money = (n) =>
  new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(n ?? 0)

export const nbJours = (d1, d2) => Math.round((new Date(d2) - new Date(d1)) / 86400000) + 1

export const dateCourte = (iso) =>
  new Date(`${iso}T00:00:00`).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })

export const STATUT = { EN_ATTENTE: 'EN ATTENTE', CONFIRMEE: 'CONFIRMÉE', ANNULEE: 'ANNULÉE', TERMINEE: 'TERMINÉE' }

/** Annonce aux autres écrans (pastille du menu, tableau de bord) qu'une réservation a changé. */
export const notifyReservationsChanged = () => window.dispatchEvent(new Event('reservations-changed'))
