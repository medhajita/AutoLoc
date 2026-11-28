import { useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../api.js'

const JOURS = ['L', 'M', 'M', 'J', 'V', 'S', 'D']
const pad = (n) => String(n).padStart(2, '0')
// Dates en heure locale (toISOString décalerait d'un jour autour de minuit)
const iso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const parse = (s) => new Date(`${s}T00:00:00`)
const debutMois = (d) => new Date(d.getFullYear(), d.getMonth(), 1)
const ajouterMois = (d, n) => new Date(d.getFullYear(), d.getMonth() + n, 1)
const court = (s) => parse(s).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })

/** Niveau de disponibilité d'un jour : beaucoup (vert), peu (bleu), complet (rouge). */
function niveau(info) {
  if (!info) return 'inconnu'
  if (info.libres === 0) return 'complet'
  return info.libres / info.total >= 0.8 ? 'beaucoup' : 'peu'
}

function Mois({ mois, dispo, aujourdhui, debut, fin, survol, onPick, onHover }) {
  const premier = new Date(mois.getFullYear(), mois.getMonth(), 1)
  const decalage = (premier.getDay() + 6) % 7 // semaine commençant le lundi
  const nbJours = new Date(mois.getFullYear(), mois.getMonth() + 1, 0).getDate()
  const cases = [...Array(decalage).fill(null), ...Array.from({ length: nbJours }, (_, i) => i + 1)]
  const bout = fin || (debut && survol && survol >= debut ? survol : '')

  return (
    <div className="cal-mois">
      <div className="cal-titre">{mois.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })}</div>
      <div className="cal-grille cal-semaine" aria-hidden="true">
        {JOURS.map((j, i) => (
          <span key={i}>{j}</span>
        ))}
      </div>
      <div className="cal-grille">
        {cases.map((n, i) => {
          if (n === null) return <span key={`v${i}`} />
          const jour = iso(new Date(mois.getFullYear(), mois.getMonth(), n))
          const info = dispo[jour]
          const passe = jour < aujourdhui
          const complet = niveau(info) === 'complet'
          const inactif = passe || complet
          const extremite = jour === debut || jour === fin
          const dansPlage = debut && bout && jour > debut && jour < bout
          const classes = ['cal-jour']
          if (inactif) classes.push(passe ? 'passe' : 'complet')
          else classes.push(niveau(info))
          if (extremite) classes.push('choisi')
          if (dansPlage) classes.push('plage')
          const libelle = passe
            ? ''
            : info
              ? `, ${info.libres} véhicule${info.libres > 1 ? 's' : ''} disponible${info.libres > 1 ? 's' : ''} sur ${info.total}`
              : ''
          return (
            <button
              type="button"
              key={jour}
              className={classes.join(' ')}
              disabled={inactif}
              aria-label={`${parse(jour).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' })}${libelle}`}
              aria-pressed={extremite}
              onClick={() => onPick(jour)}
              onMouseEnter={() => onHover(jour)}
            >
              {n}
            </button>
          )
        })}
      </div>
    </div>
  )
}

/**
 * Sélecteur de période à deux mois. Chaque jour est coloré selon le nombre de véhicules libres
 * (données réelles de l'agence) ; les jours passés ou complets ne sont pas sélectionnables.
 */
export default function DateRangePicker({ value, onChange }) {
  const { debut, fin } = value
  const aujourdhui = useMemo(() => iso(new Date()), [])
  const [ouvert, setOuvert] = useState(false)
  const [mois, setMois] = useState(() => debutMois(debut ? parse(debut) : new Date()))
  const [dispo, setDispo] = useState({})
  const [survol, setSurvol] = useState('')
  const [erreur, setErreur] = useState('')
  const racine = useRef(null)

  // Disponibilités des deux mois affichés
  useEffect(() => {
    if (!ouvert) return
    const du = iso(mois)
    const au = iso(new Date(mois.getFullYear(), mois.getMonth() + 2, 0))
    api(`/espace/disponibilites?debut=${du}&fin=${au}`, { anonymous: true })
      .then((jours) => {
        setErreur('')
        setDispo((prev) => ({ ...prev, ...Object.fromEntries(jours.map((j) => [j.date, j])) }))
      })
      .catch((e) => setErreur(e.message))
  }, [ouvert, mois])

  // Fermeture au clic extérieur et à Échap
  useEffect(() => {
    if (!ouvert) return
    const dehors = (e) => racine.current && !racine.current.contains(e.target) && setOuvert(false)
    const echap = (e) => e.key === 'Escape' && setOuvert(false)
    document.addEventListener('mousedown', dehors)
    document.addEventListener('keydown', echap)
    return () => {
      document.removeEventListener('mousedown', dehors)
      document.removeEventListener('keydown', echap)
    }
  }, [ouvert])

  function traverseUnJourComplet(du, au) {
    return Object.values(dispo).some((j) => j.date > du && j.date < au && j.libres === 0)
  }

  function choisir(jour) {
    if (!debut || fin) {
      onChange({ debut: jour, fin: '' }) // nouvelle sélection
    } else if (jour < debut || traverseUnJourComplet(debut, jour)) {
      onChange({ debut: jour, fin: '' }) // on repart de ce jour
    } else {
      onChange({ debut, fin: jour })
      setOuvert(false)
    }
  }

  const resume =
    debut && fin
      ? `${court(debut)} → ${court(fin)}`
      : debut
        ? `${court(debut)} → choisissez le retour`
        : 'Choisir les dates'

  return (
    <div className="cal-wrap" ref={racine}>
      <button
        type="button"
        className={`cal-trigger${debut ? ' rempli' : ''}`}
        aria-haspopup="dialog"
        aria-expanded={ouvert}
        onClick={() => setOuvert(!ouvert)}
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
          <rect x="3" y="5" width="18" height="16" rx="3" />
          <path d="M8 3v4M16 3v4M3 10h18" />
        </svg>
        <span>{resume}</span>
      </button>

      {ouvert && (
        <div className="cal-popover" role="dialog" aria-label="Choisir les dates de location">
          <div className="cal-nav">
            <button
              type="button"
              className="cal-fleche"
              aria-label="Mois précédent"
              disabled={mois <= debutMois(new Date())}
              onClick={() => setMois(ajouterMois(mois, -1))}
            >
              ‹
            </button>
            <button type="button" className="cal-fleche" aria-label="Mois suivant" onClick={() => setMois(ajouterMois(mois, 1))}>
              ›
            </button>
          </div>
          <div className="cal-mois-deux" onMouseLeave={() => setSurvol('')}>
            {[0, 1].map((n) => (
              <Mois
                key={n}
                mois={ajouterMois(mois, n)}
                dispo={dispo}
                aujourdhui={aujourdhui}
                debut={debut}
                fin={fin}
                survol={survol}
                onPick={choisir}
                onHover={setSurvol}
              />
            ))}
          </div>
          {erreur && <p className="msg-error">{erreur}</p>}
          <div className="cal-pied">
            <div className="cal-legende">
              <span><i className="pt beaucoup" />Large choix</span>
              <span><i className="pt peu" />Peu de véhicules</span>
              <span><i className="pt complet" />Complet</span>
            </div>
            {(debut || fin) && (
              <button type="button" className="btn-text" onClick={() => onChange({ debut: '', fin: '' })}>
                Effacer
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
