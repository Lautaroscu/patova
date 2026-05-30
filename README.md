# Patova — Escudo Inteligente contra el Spam Telefónico 🛡️🇦🇷

<p align="center">
  <img src="backend/src/numguard/admin/static/patova-icon.png" alt="Patova Logo" width="120" style="border-radius: 28px; box-shadow: 0 8px 24px rgba(0,0,0,0.3);"/>
</p>

**Patova** es la solución definitiva y de nivel industrial contra el spam telefónico, llamadas de telemarketing insoportables y estafas virtuales (vishing) en Argentina. Diseñado con una filosofía **offline-first y de privacidad por diseño**, Patova intercepta y corta el spam en menos de 50 milisegundos nativos, devolviéndote la paz a la hora de la siesta.

Este repositorio es un monorepo completo de nivel producción que incluye la aplicación nativa en Android, el backend escalable en FastAPI expuesto en Docker, observabilidad integrada, y el flujo completo de monetización por suscripciones.

---

## 🛠️ Arquitectura del Sistema

El ecosistema de Patova está dividido en tres capas principales que colaboran de forma asíncrona:

```mermaid
graph TD
    subgraph Cliente Android
        A[Incoming Call] -->|Intercepta| B[PatovaScreeningService]
        B -->|Consulta rápida <50ms| C[(SQLite Room Cache)]
        B -->|Respuesta| D{¿Es Spam?}
        D -->|Sí: Premium| E[Silenciar y Cortar]
        D -->|Sí: Free| F[Alerta Roja Visual]
        D -->|No/Unknown| G[Permitir llamada]
        H[BehaviorSyncWorker] -->|Sincronización incremental| C
    end

    subgraph Backend API (FastAPI)
        I[FastAPI App] -->|Escritura de Reportes| J[(PostgreSQL DB)]
        I -->|Cache & Rate Limit| K[(Redis Client)]
        H -->|HTTP GET /sync| I
    end

    subgraph Pasarela de Pagos
        L[Mercado Pago Sandbox] -->|Webhook de Aprobación| I
        L -->|Redirección HTTPS| M[Redirect Bridge Endpoint]
        M -->|Deep Link: patova://| A
    end
```

### 1. Aplicación Android (Kotlin Nativo)
*   **Interceptación en Tiempo Real:** Implementación del `CallScreeningService` nativo de Android, que evalúa la reputación de cada número entrante en milisegundos.
*   **Base de Datos Local (Offline-First):** Utiliza Room/SQLite local para almacenar la lista negra. No requiere conexión a internet para bloquear, ahorrando batería y protegiendo tu privacidad (nunca sube tu agenda al servidor).
*   **UI Jetpack Compose Premium:** Interfaz oscura ("Espacio Profundo") espectacular con detalles dorados y efectos cristalinos (glassmorphism) en el Paywall.

### 2. Backend API (FastAPI)
*   **Motor de FastAPI:** Escrito en Python 3.12 asíncrono, expuesto en contenedores Docker y configurado con Uvicorn para alta concurrencia.
*   **Base de datos Relacional (PostgreSQL):** Almacena dispositivos, reportes de spam, y los estados de suscripciones con Alembic para migraciones.
*   **Capa de Velocidad (Redis):** Maneja el límite de peticiones (rate limiting) por IP/Dispositivo y la idempotencia estricta en el procesamiento de webhooks de Mercado Pago.

### 3. Monetización & Redirect Bridge
*   **Pasarela Mercado Pago:** Integración completa de cobros para planes Mensual (\$1.000) y Anual (\$9.600) con sandbox y producción.
*   **Puente de Redirección (HTTPS Redirect Bridge):** Un endpoint especializado `GET /v1/payments/redirect` con una UI ultra estéticamente cuidada en HTML/JS que resuelve las limitaciones de los navegadores móviles y redirige de vuelta a la app usando deep links (`patova://checkout/{status}`).

---

## 💎 Planes de Blindaje (Free vs. Premium)

| Característica | Plan Básico (Gratis) | Plan Premium 👑 |
| :--- | :---: | :---: |
| **Identificación de Spam** | Básica | Avanzada + Inteligencia Artificial |
| **Base de Datos ENACOM** | Manual básica | Completa en tiempo real |
| **Comportamiento ante Spam** | Alerta visual (suena el celular) | **Bloqueo y rechazo silencioso inmediato** |
| **Protección contra Estafas** | ❌ No disponible | **Filtro Avanzado (Vishing)** |
| **Reportes Comunitarios** | Limitados a 3 diarios | **Ilimitados** |
| **Modo Offline Premium** | ❌ No disponible | **Hasta 7 días sin internet** |
| **Estadísticas de Ahorro** | ❌ No disponible | Dashboard total de tiempo salvado |

> [!TIP]
> **Estrategia de Anclaje de Precios:**
> *   **Plan Mensual ($1.000):** Menos de lo que sale un café al paso al mes para librarse de las llamadas spam.
> *   **Plan Anual ($9.600 - Ahorro 34%):** Equivale a menos de lo que cuesta un alfajor por mes para tener paz mental todo el año.

---

## 📂 Estructura del Monorepo

```
patova/
├── android/          # Proyecto Android nativo (Kotlin, Jetpack Compose, Room, Hilt)
├── backend/          # Backend de FastAPI (Python 3.12, Uvicorn, SQLAlchmey, Redis)
│   └── src/numguard/
│       ├── api/      # Endpoints v1 (Health, Spam, Sync, Payments, Config)
│       ├── models/   # Modelos SQLAlchemy (Subscription, WebhookEvent, Device)
│       ├── services/ # Clientes de integraciones (Mercado Pago client)
│       └── templates/# HTML premium para el Redirect Bridge y Landing Page
├── infra/            # Docker Compose, Prometheus, Grafana, Nginx config
└── scripts/          # Seed de bases de datos y scripts de utilidades
```

---

## 🚀 Guía de Desarrollo Local

### 1. Iniciar la Infraestructura Backend (Docker)
Asegurate de tener Docker instalado. Desde la raíz de la carpeta `infra`, levantá toda la pila de servicios:

```bash
cd infra
docker compose up -d --build
```
Esto encenderá automáticamente:
*   **FastAPI App** en `http://localhost:8000`
*   **PostgreSQL** en el puerto `5433`
*   **Redis** en el puerto `6379`
*   **Prometheus** en `http://localhost:9090`
*   **Grafana** en `http://localhost:3000` (User: `admin`, Pass: `numguard`)

### 2. Configurar el Túnel de Webhooks (ngrok)
Para probar los flujos de cobro y redirección desde el celular, necesitás exponer tu backend a HTTPS usando ngrok:

```bash
ngrok http 8000
```
Copiá la URL generada (ej: `https://65a5-152-170-2-32.ngrok-free.app`) y configurala en el archivo `backend/.env`:
```env
MP_WEBHOOK_BASE_URL=https://tu-subdominio.ngrok-free.app
```
Luego reiniciá la API para aplicar los cambios:
```bash
docker compose -f infra/docker-compose.yml up -d --build api
```

### 3. Compilar la Aplicación Android
Abrí el directorio `android/` en tu Android Studio o compilalo por consola usando Gradle:

```bash
cd android
./gradlew assembleDebug
```
Instalá el APK generado en tu celular o emulador, habilitá el permiso de **Aplicación de Identificación y Spam** en Ajustes del Sistema de Android, ¡y listo!

---

## 🔒 Privacidad y Cumplimiento
Patova cumple estrictamente con regulaciones de privacidad de datos locales. La aplicación **nunca almacena ni sube tu agenda personal de contactos, mensajes SMS o contenido de llamadas**. Todo el análisis e interceptación de llamadas ocurre localmente en el dispositivo. Solo los números reportados manualmente y de forma voluntaria alimentan de forma cifrada el motor de reputación colectiva de la API.
