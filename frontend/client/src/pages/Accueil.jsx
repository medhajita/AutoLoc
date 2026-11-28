import { useEffect, useMemo, useState } from 'react'
import { api, money, nbJours, today } from '../api.js'
const cap = (s) => s.charAt(0) + s.slice(1).toLowerCase()
const ETAPES = [
  ['Choisissez vos dates', 'Indiquez le début et la fin de votre location : seuls les véhicules libres sont proposés.'],
  ['Réservez en ligne', "Créez votre compte, vérifiez le prix total et validez. L'agence confirme votre réservation."],
  ['Gérez vos réservations', "Modifiez les dates ou annulez depuis votre espace, tant que la location n'a pas commencé."],
]

export default function Accueil({ me, go, dates, setDates, onReserved }) {
  const { debut, fin } = dates
  const [vehicules, setVehicules] = useState([])
  const [categorie, setCategorie] = useState('Toutes')
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)

  const datesOk = Boolean(debut && fin && fin >= debut)
  const jours = datesOk ? nbJours(debut, fin) : 0

  useEffect(() => {
    const query = datesOk ? `?debut=${debut}&fin=${fin}` : ''
    api(`/espace/vehicules${query}`, { anonymous: true })
      .then((v) => {
        setVehicules(v)
        setError('')
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [debut, fin, datesOk])

  const categories = useMemo(() => ['Toutes', ...new Set(vehicules.map((v) => v.categorie))], [vehicules])
  const visibles = vehicules.filter((v) => categorie === 'Toutes' || v.categorie === categorie)
  const agence = vehicules.find((v) => v.agenceNom)?.agenceNom ?? 'AutoLoc'

  async function reserver(v) {
    setInfo('')
    if (!datesOk) {
      setInfo('Choisissez d’abord vos dates pour réserver.')
      document.getElementById('recherche')?.scrollIntoView({ behavior: 'smooth' })
      return
    }
    if (!me) {
      go('login')
      return
    }
    setBusy(true)
    try {
      await api('/espace/reservations', {
        method: 'POST',
        body: { vehiculeId: v.idVehicule, dateDebut: debut, dateFin: fin },
      })
      onReserved()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <section className="hero">
        <div className="hero-text">
          <h1>
            Votre voiture,
            <br />
            quand vous voulez.
          </h1>
          <p>
            Choisissez vos dates, comparez les véhicules disponibles et réservez en quelques clics. Modification et
            annulation en ligne.
          </p>
          <button
            type="button"
            className="btn-dark"
            onClick={() => document.getElementById('recherche')?.scrollIntoView({ behavior: 'smooth' })}
          >
            Choisir mes dates
          </button>
        </div>
        <img className="hero-car" src={`${import.meta.env.BASE_URL}hero.webp`} alt="Voiture de sport blanche" />
      </section>

      <section className="section-pad" id="recherche">
        <form className="search" onSubmit={(e) => e.preventDefault()}>
          <label>
            Du
            <input type="date" min={today()} value={debut} onChange={(e) => setDates({ debut: e.target.value, fin: fin < e.target.value ? '' : fin })} />
          </label>
          <label>
            Au
            <input type="date" min={debut || today()} value={fin} onChange={(e) => setDates({ debut, fin: e.target.value })} />
          </label>
          <label>
            Catégorie
            <select value={categorie} onChange={(e) => setCategorie(e.target.value)}>
              {categories.map((c) => (
                <option key={c} value={c}>{cap(c)}</option>
              ))}
            </select>
          </label>
          <button
            type="button"
            className="btn-primary"
            onClick={() => document.getElementById('catalogue')?.scrollIntoView({ behavior: 'smooth' })}
          >
            Rechercher
          </button>
        </form>
        {debut && fin && fin < debut && <p className="msg-error">La date de fin précède la date de début.</p>}
      </section>

      <section className="section-pad catalogue" id="catalogue">
        <div className="section-head">
          <div>
            <h2>Nos véhicules</h2>
            <p className="muted">
              {datesOk
                ? `${jours} jour${jours > 1 ? 's' : ''} de location · prix total affiché pour votre période`
                : 'Choisissez vos dates pour voir la disponibilité et le prix total'}
            </p>
          </div>
          <div className="chips">
            {categories.map((c) => (
              <button key={c} className={c === categorie ? 'chip on' : 'chip'} onClick={() => setCategorie(c)}>
                {c === 'Toutes' ? 'Tous' : cap(c)}
              </button>
            ))}
          </div>
        </div>
        {info && <p className="msg-error">{info}</p>}
        {error && <p className="msg-error">{error}</p>}
        {loading && <p className="muted">Chargement des véhicules…</p>}
        {!loading && visibles.length === 0 && !error && <p className="muted">Aucun véhicule dans cette catégorie.</p>}
        <div className="cards">
          {visibles.map((v) => (
            <article className="car-card" key={v.idVehicule}>
              <div className="car-body">
                <span className="pill pill-blue">{cap(v.categorie)}</span>
                <div className="car-title">
                  <h3>
                    {v.marque} {v.modele}
                  </h3>
                  <span className="muted">{money(v.tarifJournalier)} / jour</span>
                </div>
                <div className="car-bottom">
                  {datesOk ? (
                    <div>
                      <div className="muted small">{`Total ${jours} j`}</div>
                      <div className="total">{money(v.tarifJournalier * jours)}</div>
                    </div>
                  ) : (
                    <span />
                  )}
                  {datesOk && !v.disponible ? (
                    <span className="pill pill-grey">Indisponible</span>
                  ) : (
                    <button className="btn-primary" disabled={busy} onClick={() => reserver(v)}>
                      Réserver
                    </button>
                  )}
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="section-pad" id="etapes">
        <h2>Réserver en 3 étapes</h2>
        <div className="steps">
          {ETAPES.map(([titre, texte], i) => (
            <div className="step" key={titre}>
              <span className="step-n">{i + 1}</span>
              <h3>{titre}</h3>
              <p>{texte}</p>
            </div>
          ))}
        </div>
      </section>

      {!me && (
        <section className="band">
          <div>
            <h2>{agence}</h2>
            <p>Réservez et gérez vos locations depuis votre espace personnel.</p>
          </div>
          <button className="btn-white" onClick={() => go('register')}>
            Créer mon compte gratuitement
          </button>
        </section>
      )}

      <footer className="footer">
        <span>© AutoLoc · Projet ECE Msc IA</span>
        <span>Conditions de location · Contact</span>
      </footer>
    </>
  )
}
