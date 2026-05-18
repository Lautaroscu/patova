# NumGuard / Patova — Ficha de Tarea: AGENTE C (Payments Specialist)

* **Rol:** Especialista de Pagos e Integración Premium
* **Rama Git Asignada:** `feature/payments`
* **Directorio de Trabajo:** 
  * Backend: `[backend/src/numguard/services/mp/](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/backend/src/numguard/services/mp)`
  * Android: `[android/app/src/main/java/ar/com/numguard/ui/premium/](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/android/app/src/main/java/ar/com/numguard/ui/premium)`
* **Tecnologías:** FastAPI, Python, Mercado Pago REST API, Jetpack Compose, Room (Local cache).

---

## 🎯 1. Objetivo General
Implementar un sistema de monetización por suscripción estable, robusto y altamente seguro integrado con Mercado Pago (Sandbox inicial), procesando webhooks de forma idempotente en el backend y diseñando un paywall premium de alta conversión en la aplicación Android.

---

## 📜 2. Contratos de API Obligatorios
Debes implementar y respetar estrictamente los siguientes contratos de APIs.

### Contrato: Crear Preferencia de Pago (`POST /api/v1/payments/create-preference`)
* **Request Payload (de Android a Backend):**
```json
{
  "plan_id": "premium_monthly",
  "email": "user@example.com"
}
```
* **Response Payload (de Backend a Android):**
```json
{
  "preference_id": "123456789-abcde-1234-5678-abcdef",
  "init_point": "https://www.mercadopago.com.ar/sandbox/checkout/start?pref_id=..."
}
```

### Contrato: Estado de Suscripción del Usuario (`GET /api/v1/subscriptions/me`)
* **Response Payload:**
```json
{
  "user_id": "usr_998877",
  "is_premium": true,
  "subscription": {
    "id": "sub_112233",
    "status": "ACTIVE",
    "started_at": "2026-05-18T00:00:00Z",
    "expires_at": "2026-06-18T00:00:00Z",
    "provider": "MERCADO_PAGO",
    "renewal_enabled": true
  }
}
```

---

## 🛠️ 3. Plan de Tareas Step-by-Step

### Paso C.1: Creación de Preferencias MP (Backend)
1. Integrar la API REST de Mercado Pago en `backend/src/numguard/services/mp/client.py`.
2. Implementar el endpoint `POST /api/v1/payments/create-preference` para recibir la intención de compra del usuario, dar de alta la orden en Mercado Pago y retornar la URL de Sandbox (`init_point`).
3. Registrar la suscripción inicial en la tabla `subscriptions` en estado `PENDING`.

### Paso C.2: Webhook Receiver e Idempotencia (Backend)
1. Implementar el endpoint del Webhook `POST /api/v1/payments/webhook/mp` en [api/payments.py](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/backend/src/numguard/api/payments.py).
2. **Medidas de Seguridad Críticas:**
   * **Re-validación:** NUNCA confíes en los datos recibidos en el payload bruto. Al recibir un ID de pago del webhook, realiza una petición directa HTTP *server-to-server* al API oficial de Mercado Pago para verificar que el estado sea efectivamente `approved` y el monto coincida.
   * **Idempotencia:** Registrar cada `event_id` procesado en Redis o PostgreSQL. Si llega un evento duplicado, responder con código `200 OK` inmediatamente sin re-procesar ni duplicar la suscripción.
   * **Actualización Asíncrona:** Una vez verificado el pago, actualizar el estado del usuario en la tabla `subscriptions` a `ACTIVE` y setear el `expires_at`.

### Paso C.3: Interfaz de Paywall Premium (Android)
1. Diseñar y programar una pantalla premium para el Paywall en [ar.com.numguard.ui.premium.PaywallScreen](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/android/app/src/main/java/ar/com/numguard/ui/premium/PaywallScreen.kt) en Jetpack Compose.
2. Incorporar un diseño premium: fondos con desenfoque de cristal (glassmorphism), gradientes armónicos dorados/violetas, listado comparativo de features gratis vs premium, y botón interactivo para iniciar la suscripción.
3. El botón debe abrir el `init_point` de Mercado Pago utilizando `CustomTabs` o el navegador del dispositivo para completar el checkout de forma segura.

### Paso C.4: Cache de Estado Premium Offline (Android)
1. Guardar localmente el estado premium de forma encriptada en la base de datos Room o DataStore local.
2. Permitir que la app funcione en modo Premium offline por un máximo de **7 días**. Si no hay internet transcurridos los 7 días, exigir sincronización con el servidor antes de seguir permitiendo features premium.

---

## ✅ 4. Definición de DONE
Para que tu rama `feature/payments` pueda ser integrada a `main`:
1. **Sandbox Flow End-to-End:** Realizar una compra completa simulada utilizando las tarjetas de prueba de Mercado Pago en Sandbox, verificando que se active la suscripción en el backend y se habilite el UI Premium en Android al instante.
2. **Replay Attack Security:** Probar el endpoint del webhook ante múltiples llamadas idénticas y verificar que se maneje de forma idempotente sin duplicar registros en base de datos.
3. **Grace Period Test:** Simular desconexión por 8 días y comprobar que la aplicación deshabilite temporalmente las features premium hasta que se reconecte y verifique con el servidor.
4. **Paywall Responsiveness:** Validar que la interfaz visual de Paywall no tenga overflow en pantallas pequeñas (ej: dispositivos de 4.7 pulgadas) y sea responsiva.
