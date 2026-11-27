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
