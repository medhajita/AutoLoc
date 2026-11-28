const KEY = 'autoloc_client_auth'

export const hasAuth = () => sessionStorage.getItem(KEY) !== null
export const setAuth = (user, pass) => sessionStorage.setItem(KEY, btoa(`${user}:${pass}`))
export const clearAuth = () => sessionStorage.removeItem(KEY)

export async function api(path, { method = 'GET', body, auth, anonymous = false } = {}) {
  const token = anonymous ? null : (auth ?? sessionStorage.getItem(KEY))
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
    if (!auth && !anonymous) window.dispatchEvent(new Event('auth-expired'))
    throw new Error('Identifiants invalides ou session expirée')
  }
  if (res.status === 403) throw new Error('Action non autorisée pour ce compte')
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
  new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(n ?? 0)

export const today = () => new Date().toISOString().slice(0, 10)

export const nbJours = (d1, d2) => Math.round((new Date(d2) - new Date(d1)) / 86400000) + 1

export const dateFr = (iso) =>
  new Date(`${iso}T00:00:00`).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })
