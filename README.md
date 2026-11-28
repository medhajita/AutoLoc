# AutoLoc

## Monitoring local

Le profil Docker Compose démarre MySQL, l’API, Prometheus et Grafana. Les mots
de passe doivent être fournis par variables d’environnement et ne sont pas
stockés dans le dépôt.

```powershell
$env:MYSQL_ROOT_PASSWORD = '<mot-de-passe-local>'
$env:GRAFANA_ADMIN_PASSWORD = '<mot-de-passe-local>'
docker compose up --build -d
```

Services exposés par défaut :

- API : http://127.0.0.1:8080
- Health : http://127.0.0.1:8080/actuator/health
- Métriques : http://127.0.0.1:8080/actuator/prometheus
- Prometheus : http://127.0.0.1:9090/targets
- Grafana : http://127.0.0.1:3001
- MySQL hôte : `127.0.0.1:3307`

Les ports peuvent être modifiés avec `API_PORT`, `PROMETHEUS_PORT`,
`GRAFANA_PORT` et `MYSQL_PORT`. Arrêt : `docker compose down`.

## Interface web (React + Vite)

L'API JSON (`/api/vehicules`, `/api/clients`, `/api/reservations`, `/api/contrats`, `/api/dashboard`)
est consommée par `frontend/`.

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173 (proxy /api -> http://localhost:8080)
```

Compte du personnel : `admin` / `admin123` (rôles USER + ADMIN, seul autorisé à supprimer). Comptes clients de démonstration : `marie.dupont@autoloc.com` / `client123` (et 7 autres en `@autoloc.com`).

### Espace client

Les clients s'inscrivent eux-mêmes depuis l'écran de connexion (« Nouveau client ? Créer un compte »),
puis peuvent réserver un véhicule (disponibilité selon les dates), modifier les dates d'une réservation
en attente et annuler leurs réservations. Un client ne voit que ses propres réservations
(API `/api/espace/**`, rôle `CLIENT`, sans accès au back-office).

Compte client de démonstration : `marie.dupont@example.com` / `client123`.

## Sites web, CI/CD et déploiement en ligne

Une seule application Spring Boot sert les deux interfaces :

| URL | Interface | Code |
|---|---|---|
| `/client/` | Site client (réserver, gérer ses réservations) | `frontend/client` |
| `/admin/` | Back-office (tableau de bord, réservations, contrats, véhicules, clients) | `frontend/admin` |
| `/api/**` | API REST (HTTP Basic) | `src/main/java` |

En local : `npm run dev` dans `frontend/client` (port 5173) ou `frontend/admin` (port 5174), l'API sur le port 8080.

### CI / CD (GitHub Actions)

- `ci-client.yml` / `ci-admin.yml` : build Vite de chaque interface (PR et push `main` / `dev_*`).
- `ci-backend.yml` : tests Maven (H2) et build de l'image Docker complète.
- `cd.yml` (merge sur `main`) : tests, publication de l'image `ghcr.io/<owner>/autoloc`, puis appel du Deploy Hook Render.

### Variables d'environnement du service en ligne (Render)

| Variable | Rôle |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC MySQL (TiDB Cloud : `jdbc:mysql://<hôte>:4000/<base>?sslMode=VERIFY_IDENTITY`) |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Identifiants de la base |
| `AUTOLOC_ADMIN_PASSWORD` | Mot de passe du compte `admin` (défaut local : `admin123`) |
| `AUTOLOC_DEMO_CLIENT_PASSWORD` | Mot de passe du client de démo `marie.dupont@example.com` |

Secret GitHub à créer : `RENDER_DEPLOY_HOOK_URL` (Render > Settings > Deploy Hook).
