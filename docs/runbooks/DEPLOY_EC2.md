# Deploy en EC2

Runbook para desplegar NumGuard API en una instancia EC2 con Docker.

## Prerequisitos

- Instancia EC2 (Amazon Linux 2023 o Ubuntu 22.04), tipo `t3.small` minimo.
- Security group con puertos: `22` (SSH), `80` (HTTP), `443` (HTTPS).
- Dominio apuntando a la IP publica de la instancia.
- Acceso SSH configurado.

## 1. Crear instancia

```bash
# AWS CLI (ejemplo, no ejecutar sin autorizacion)
aws ec2 run-instances \
  --image-id ami-0abcdef1234567890 \
  --instance-type t3.small \
  --key-name numguard-key \
  --security-group-ids sg-xxxxxxxx \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=numguard-api}]'
```

## 2. Instalar Docker

### Amazon Linux 2023

```bash
sudo dnf update -y
sudo dnf install docker -y
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user
```

### Ubuntu 22.04

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ubuntu
```

Salir y volver a entrar para que aplique el grupo `docker`.

Verificar:

```bash
docker --version
docker compose version
```

## 3. Clonar repositorio

```bash
git clone https://github.com/tu-org/numguard.git /opt/numguard
cd /opt/numguard
```

## 4. Configurar `.env` en servidor

Crear `/opt/numguard/backend/.env` con las variables de produccion:

```bash
cp backend/.env.example backend/.env
nano backend/.env
```

Variables minimas a modificar:

```env
APP_ENV=production
DATABASE_URL=postgresql+asyncpg://numguard:SECURE_PASSWORD@postgres:5432/numguard
REDIS_URL=redis://redis:6379/0
NUMGUARD_API_KEY=sk-<random-32-bytes>
LOG_LEVEL=WARNING
SENTRY_DSN=https://...@o0.ingest.sentry.io/0
```

> Ver [ENV_VARS.md](./ENV_VARS.md) para el detalle completo.

## 5. Levantar compose prod

```bash
cd /opt/numguard

docker compose \
  -f infra/docker-compose.yml \
  -f infra/docker-compose.prod.example.yml \
  up -d --build

docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.example.yml ps
```

## 6. Configurar Nginx

Si se usa el nginx del compose:

```bash
docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.example.yml restart nginx
```

Para Nginx en el host (alternativa):

```bash
sudo apt-get install -y nginx
sudo cp infra/nginx/numguard-api.conf /etc/nginx/sites-available/numguard
sudo ln -s /etc/nginx/sites-available/numguard /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 7. Configurar dominio y SSL

Con Certbot (usar servidor web temporal o flag standalone):

```bash
sudo snap install certbot --classic
sudo certbot --nginx -d api.numguard.com.ar
sudo certbot renew --dry-run
```

Agregar redirect HTTP -> HTTPS en el server block 80.

## 8. Verificar `/health`

```bash
curl -f http://localhost:8000/health
# {"status":"ok","service":"numguard-api","version":"v1"}

curl -f https://api.numguard.com.ar/health
```

## 9. Correr migraciones

```bash
# Dentro del directorio backend
cd /opt/numguard/backend

# Si se tiene Python local
python -m alembic upgrade head

# O desde dentro del contenedor
docker compose -f infra/docker-compose.yml exec api \
  python -m alembic upgrade head
```

## 10. Rollback manual

Si un deploy sale mal:

```bash
cd /opt/numguard

# Volver al commit anterior
git checkout <commit-estable>

# Rebuild y restart
docker compose \
  -f infra/docker-compose.yml \
  -f infra/docker-compose.prod.example.yml \
  up -d --build

# O si la migracion fue el problema
docker compose exec api python -m alembic downgrade -1
```

## 11. Monitoreo basico

```bash
# Logs de la API
docker compose -f infra/docker-compose.yml logs -f api

# Estado de los contenedores
docker compose -f infra/docker-compose.yml ps

# Metricas Prometheus
curl http://localhost:8000/metrics
```
