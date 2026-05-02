# Beta cerrada NumGuard

## Objetivo

Validar que NumGuard bloquea llamadas spam reales manteniendo una tasa de falsos positivos inferior al 2%. El foco es evaluar el modelo de scoring, la experiencia del usuario de screening y la capacidad del equipo para operar el servicio en condiciones reales acotadas.

## Zona inicial

CABA y primer cordon del GBA (Vicente Lopez, San Isidro, Moron, Lomas de Zamora, Quilmes, Avellaneda, Lanus, La Matanza). La seleccion se basa en:

- Alta densidad de lineas moviles.
- Mezcla de trafico residencial y comercial.
- Cobertura de los prefijos ENACOM ya cargados en la base.

Si los resultados son positivos, expandir a AMBA completo en la siguiente fase.

## Participantes

### Fase 1: internos (semana 1)

20 usuarios del equipo de desarrollo y colaboradores directos.

Objetivo: detectar bugs criticos, validar el flujo de instalacion y verificar que el screening no interfiere con llamadas normales.

### Fase 2: externos (semanas 2-4)

200 usuarios reclutados por invitacion directa (amigos, familiares, comunidad tech local). No publicar en stores ni redes abiertas.

## Criterios de inclusion

- Android 10 (API 29) o superior.
- Aceptan configurar la app como screening service por defecto.
- Aceptan reportar feedback (falso positivo, spam confirmado) al menos una vez durante la beta.
- Linea movil argentina activa.
- Aceptan los terminos de privacidad de la beta.

## Criterios de exclusion

- Usuarios con ROMs modificadas o root.
- Dispositivos sin Google Play Services (puede afectar WorkManager / notificaciones).
- Menores de 18 anos.

## Duracion

2 a 4 semanas. Revision semanal de metricas. Criterios de salida definidos en el ultimo apartado de este documento.

Si en la semana 1 hay mas de 5 falsos positivos S0/S1, pausar la beta y ajustar thresholds.

## Canal de soporte

- Email: `soporte@numguard.com.ar` (crear antes de lanzar).
- Formulario de feedback desde la app (endpoint `POST /v1/feedback`).
- Grupo de WhatsApp/Telegram interno solo para el equipo de desarrollo.

Durante la beta no hay SLA formal de respuesta, pero se apunta a:

| Severidad | Tiempo de primera respuesta |
| --------- | --------------------------- |
| S0        | < 1 hora (horario habil)   |
| S1        | < 4 horas                  |
| S2        | < 24 horas                 |

Las severidades estan definidas en [INCIDENTES_FALSOS_POSITIVOS.md](./INCIDENTES_FALSOS_POSITIVOS.md).

## Procedimiento de instalacion APK

### Paso 1: obtener el APK

El equipo entrega el APK por un canal seguro (link privado con expiracion, Google Drive con acceso restringido, o Firebase App Distribution si se configura). No publicar en tiendas publicas.

Para generar el APK:

```bash
cd android
./gradlew assembleDebug   # Para beta interna
# o
./gradlew assembleRelease # Firmado con keystore de beta
```

### Paso 2: transferir al dispositivo

- Por cable USB + `adb install`.
- Por descarga directa desde el link (requiere habilitar "Origenes desconocidos").
- Por email o mensajeria si el tamanio lo permite.

Comando ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Paso 3: configurar la app

1. Abrir NumGuard.
2. Ir a Ajustes de la app.
3. Configurar URL de la API beta: `https://beta-api.numguard.com.ar`.
4. Ingresar API Key de beta (provee el equipo).
5. Activar "NumGuard" como app de screening por defecto:
   - Ajustes > Aplicaciones > Aplicaciones predeterminadas > App de identificacion de llamadas y spam > NumGuard.

### Paso 4: verificar

1. Recibir una llamada de prueba (el equipo provee numeros de test).
2. Verificar que la app muestra notificacion con verdict.
3. Verificar que la llamada aparece en Historial.

## Procedimiento de desinstalacion y desactivacion

### Desactivar el screening

1. Ajustes > Aplicaciones > Aplicaciones predeterminadas > App de identificacion de llamadas y spam.
2. Seleccionar "Ninguna" o la app anterior.
3. Verificar que las llamadas entrantes ya no muestran overlay de NumGuard.

### Desinstalar la app

1. Ajustes > Aplicaciones > NumGuard > Desinstalar.
2. Confirmar eliminacion de datos.

### Rollback post-beta

Si al finalizar la beta el usuario quiere eliminar sus datos:
- Solicitar por email a `soporte@numguard.com.ar` la eliminacion de reportes y feedback asociados a su device_id.
- El equipo ejecuta un script de eliminacion (ver [PRIVACIDAD_DATOS.md](./PRIVACIDAD_DATOS.md)).

## Metricas recolectadas durante la beta

Ver [METRICAS_MVP.md](./METRICAS_MVP.md) para el detalle completo.

## Criterios de salida

La beta cerrada se considera exitosa y habilita el pase a beta publica si:

1. **Seguridad**: No hay incidentes S0 abiertos al cierre.
2. **Precision**: Falsos positivos confirmados < 2% del total de bloqueos.
3. **Performance**: p95 de latencia API < 200 ms durante trafico real (no sintetico).
4. **Retencion**: Al menos 20 usuarios activos durante 7 dias consecutivos.
5. **Eficacia**: Se bloquean llamadas spam reales verificadas (al menos 50 bloqueos con al menos 1 reporte de spam confirmado por usuario distinto).
6. **Operabilidad**: El equipo de soporte puede resolver incidentes de falso positivo sin tocar codigo manualmente en cada caso.

Si algun criterio no se cumple, iterar los thresholds o el modelo antes de abrir a beta publica.
