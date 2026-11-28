const KEY = 'autoloc_auth'

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
