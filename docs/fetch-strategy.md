# Stratégies de chargement JPA — AutoLoc

## 1. LAZY vs EAGER : principe

| Critère | `FetchType.LAZY` | `FetchType.EAGER` |
|---|---|---|
| Chargement | À la première utilisation de la collection/association | Immédiatement avec l'entité parente (JOIN) |
| Requêtes SQL | 1 requête initiale + N requêtes si accès aux relations | 1 requête (ou JOIN) systématique |
| Risque | `LazyInitializationException` hors session | Sur-chargement et N+1 si mal utilisé |
| Usage recommandé | Collections (`@OneToMany`, `@ManyToMany`) | Associations scalaires critiques (`@ManyToOne` léger) |

## 2. Comportement par défaut JPA

| Annotation | Défaut JPA |
|---|---|
| `@ManyToOne` | `EAGER` |
| `@OneToOne` | `EAGER` |
| `@OneToMany` | `LAZY` |
| `@ManyToMany` | `LAZY` |

> Dans AutoLoc, **tous les fetch sont explicitement déclarés `LAZY`** pour éviter le chargement non maîtrisé, y compris sur les `@ManyToOne` (surcharge du défaut JPA).

## 3. Associations implémentées

### 3.1 Contrat / Paiement — `@OneToMany` / `@ManyToOne` bidirectionnelle

```
Contrat (1) ──────────────── (*) Paiement
           mappedBy="contrat"
```

- **Côté propriétaire** : `Paiement.contrat` — porte la FK `contrat_id`.
- **Côté inverse** : `Contrat.paiements` — `mappedBy="contrat"`.
- **Cascade** : `CascadeType.ALL` — toute opération sur un `Contrat` se propage aux `Paiement` associés (persist, merge, remove, refresh, detach).
- **orphanRemoval = true** — un `Paiement` retiré de `contrat.paiements` est automatiquement supprimé en base.
- **Fetch** : `LAZY` des deux côtés.

```java
// Contrat.java
@OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
private List<Paiement> paiements = new ArrayList<>();

// Paiement.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "contrat_id", nullable = false)
private Contrat contrat;
```

**Effet cascade ALL + orphanRemoval :**
```java
Contrat c = em.find(Contrat.class, 1L);
c.getPaiements().clear();         // → DELETE FROM paiement WHERE contrat_id = 1
em.remove(c);                     // → DELETE FROM contrat WHERE id_contrat = 1
```

### 3.2 Agence / Vehicule — `@OneToMany` / `@ManyToOne` bidirectionnelle

```
Agence (1) ──────────────── (*) Vehicule
          mappedBy="agence"
```

- FK `agence_id` sur la table `vehicule`.
- Pas de cascade : un véhicule peut exister sans agence (nullable).
- Fetch `LAZY` des deux côtés.

### 3.3 Agence / Employe — `@OneToMany` / `@ManyToOne` bidirectionnelle

```
Agence (1) ──────────────── (*) Employe
          mappedBy="agence"
```

- FK `agence_id` sur la table `employe`.
- Pas de cascade : un employé peut être affecté à une agence indépendamment.
- Fetch `LAZY` des deux côtés.

### 3.4 Vehicule / Equipement — `@ManyToMany` bidirectionnelle

```
Vehicule (*) ──────────────── (*) Equipement
             vehicule_equipement
```

- **Table de jointure** : `vehicule_equipement` (colonnes `vehicule_id`, `equipement_id`).
- **Côté propriétaire** : `Vehicule.equipements` — porte le `@JoinTable`.
- **Côté inverse** : `Equipement.vehicules` — `mappedBy="equipements"`.
- Fetch `LAZY` des deux côtés.

```java
// Vehicule.java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "vehicule_equipement",
    joinColumns = @JoinColumn(name = "vehicule_id"),
    inverseJoinColumns = @JoinColumn(name = "equipement_id")
)
private List<Equipement> equipements = new ArrayList<>();

// Equipement.java
@ManyToMany(mappedBy = "equipements", fetch = FetchType.LAZY)
private List<Vehicule> vehicules = new ArrayList<>();
```

## 4. Schéma généré

```sql
-- FK Paiement → Contrat
ALTER TABLE paiement ADD COLUMN contrat_id BIGINT NOT NULL;
ALTER TABLE paiement ADD CONSTRAINT fk_paiement_contrat FOREIGN KEY (contrat_id) REFERENCES contrat(id_contrat);

-- FK Vehicule → Agence
ALTER TABLE vehicule ADD COLUMN agence_id BIGINT;
ALTER TABLE vehicule ADD CONSTRAINT fk_vehicule_agence FOREIGN KEY (agence_id) REFERENCES agence(id_agence);

-- FK Employe → Agence
ALTER TABLE employe ADD COLUMN agence_id BIGINT;
ALTER TABLE employe ADD CONSTRAINT fk_employe_agence FOREIGN KEY (agence_id) REFERENCES agence(id_agence);

-- Table de jointure Vehicule ↔ Equipement
CREATE TABLE vehicule_equipement (
    vehicule_id   BIGINT NOT NULL,
    equipement_id BIGINT NOT NULL,
    PRIMARY KEY (vehicule_id, equipement_id),
    FOREIGN KEY (vehicule_id)   REFERENCES vehicule(id_vehicule),
    FOREIGN KEY (equipement_id) REFERENCES equipement(id_equipement)
);
```

> Avec `spring.jpa.hibernate.ddl-auto=update`, Hibernate génère ces colonnes/tables automatiquement au démarrage.

## 5. Recommandations

- Toujours accéder aux collections `LAZY` **dans une transaction active** pour éviter `LazyInitializationException`.
- Utiliser des **JPQL JOIN FETCH** ou **EntityGraph** quand plusieurs niveaux d'associations sont nécessaires en une seule requête.
- Éviter `FetchType.EAGER` sur les collections : risque de chargement de milliers de lignes de façon implicite.
