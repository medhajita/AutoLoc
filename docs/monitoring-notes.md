# AutoLoc — Monitoring V1 (Task 9)

## Composants

- Spring Boot Actuator expose `health` et `prometheus`.
- Micrometer publie les métriques JVM et HTTP au format Prometheus.
- Prometheus collecte l’API toutes les 15 secondes.
- Grafana est fourni par Docker Compose sur le port hôte `3001` afin de ne pas
  entrer en conflit avec AutoLoc Task Launcher sur le port `3000`.
- MySQL, l’API, Prometheus et Grafana partagent le réseau privé créé par Docker
  Compose. Aucun nom de conteneur global n’est imposé.

## Sécurité et secrets

Seuls `/actuator/health` et `/actuator/prometheus` sont publics. Les autres
routes `/api/**` conservent les règles HTTP Basic de TASK-08. Les mots de passe
MySQL et Grafana sont fournis par `MYSQL_ROOT_PASSWORD` et
`GRAFANA_ADMIN_PASSWORD` ; ils ne sont pas enregistrés dans le dépôt.

## Démarrage

```powershell
$env:MYSQL_ROOT_PASSWORD = '<mot-de-passe-local>'
$env:GRAFANA_ADMIN_PASSWORD = '<mot-de-passe-local>'
docker compose up --build -d
```

Endpoints par défaut :

- `http://127.0.0.1:8080/actuator/health`
- `http://127.0.0.1:8080/actuator/prometheus`
- `http://127.0.0.1:9090/targets`
- `http://127.0.0.1:3001`

## Validation réelle du 27 novembre 2026

La collecte a été testée avec des ports hôtes alternatifs pour cohabiter avec
les services locaux existants : API `8081`, Prometheus `9091` et MySQL `3307`.
La configuration Grafana du Compose a été validée séparément ; son port par
défaut reste `3001`.

Résultats observés :

- Actuator health : `UP`
- `/actuator/prometheus` : HTTP `200`
- métriques JVM présentes : oui
- cible Prometheus `autoloc-api` : `UP`
- erreur du dernier scrape : aucune
- tests Maven : 8 réussis, 0 échec, 0 erreur

![Cible AutoLoc UP dans Prometheus](prometheus-target-up.png)

Arrêt de la stack :

```powershell
docker compose down
```
