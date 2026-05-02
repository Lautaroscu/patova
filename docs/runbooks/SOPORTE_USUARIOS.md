# Soporte a usuarios (beta cerrada)

## Canales

| Canal                                   | Publico objetivo | Uso                                           |
| --------------------------------------- | ---------------- | --------------------------------------------- |
| Email `soporte@numguard.com.ar`         | Usuarios beta    | Reportes de falsos positivos, consultas, bajas |
| Formulario POST `/v1/feedback`          | Desde la app     | Feedback de llamada (spam / falso positivo)    |
| Grupo Telegram/WhatsApp interno        | Equipo dev       | Coordinacion de incidentes, alertas            |

## Niveles de soporte

Al ser beta cerrada sin SLA contractual, se opera con mejores esfuerzos. Para tener orden interno:

| Nivel          | Quien atiende          | Que cubre                                          |
| -------------- | ---------------------- | -------------------------------------------------- |
| N1 (primer contacto) | Persona designada del equipo | Clasificar reporte, responder al usuario, escalar si es S0/S1 |
| N2 (tecnico)   | Desarrollador          | Analizar scoring, revisar logs, ajustar thresholds, invalidar cache |
| N3 (dev lead)  | Tech lead              | Decisiones de revertir deploy, pausar beta, modificar modelo |

## Flujo de atencion

```
Usuario reporta (email o feedback app)
    -> N1 clasifica severidad
        -> S0: escala a N2 + N3 inmediatamente
        -> S1: escala a N2 en < 4 horas habiles
        -> S2: registra y escala a N2 en < 24 horas
    -> N2 analiza (logs, scoring, cache)
    -> N2 resuelve o escala a N3 si requiere cambio de codigo/deploy
    -> N1 notifica al usuario la resolucion
    -> Se registra el aprendizaje en INCIDENTES_FALSOS_POSITIVOS.md
```

## Tiempos de respuesta (mejor esfuerzo)

| Severidad  | Primera respuesta | Resolucion esperada |
| ---------- | ----------------- | -------------------- |
| S0         | < 1 hora          | < 4 horas           |
| S1         | < 4 horas         | < 24 horas          |
| S2         | < 24 horas        | < 72 horas          |

Horario habil: Lunes a Viernes 9-18hs (ART). Fuera de horario solo se atienden S0 si hay guardia designada.

## Plantilla de respuesta a usuario

### Falso positivo

```
Hola [nombre],

Gracias por reportar. Confirmamos que la llamada del numero [numero]
fue clasificada incorrectamente como spam.

Ya ajustamos el scoring para que no vuelva a ocurrir. El cambio
toma efecto de inmediato (no necesitas actualizar la app).

Si vuelve a pasar, por favor respondenos a este mail.

Saludos,
Equipo NumGuard
```

### Reporte de spam confirmado

```
Hola [nombre],

Gracias por reportar el numero [numero] como spam. Tu reporte
ayuda a mejorar la deteccion para todos los usuarios.

Recibiras menos llamadas de este tipo a medida que el sistema
aprende.

Saludos,
Equipo NumGuard
```

### Baja de datos

```
Hola [nombre],

Procesamos tu solicitud de eliminacion de datos. En 48 horas habiles
tus reportes y feedback asociados a tu dispositivo seran eliminados
de nuestros sistemas.

Si tenes mas consultas, escribinos.

Saludos,
Equipo NumGuard
```

## Herramientas del agente N1

- Dashboard admin (`/admin`): ver ultimos reportes, feedback, metricas.
- Acceso a logs (si esta configurado): `docker compose logs api`.
- Acceso a Redis (invalidar cache): `docker compose exec redis redis-cli`.
- Correo electronico para respuesta.
- Este runbook y [INCIDENTES_FALSOS_POSITIVOS.md](./INCIDENTES_FALSOS_POSITIVOS.md).

## Escalamiento a N2

Criterios para escalar:

1. El usuario reporta un falso positivo y el numero NO aparece en logs.
2. El scoring muestra un score alto pero el usuario insiste que es legitimo.
3. Hay 3 o mas reportes del mismo numero en menos de 24 horas.
4. La app crashea (reportar con logs de dispositivo si se puede obtener).
5. La API devuelve errores 5xx en `/v1/validate`.

## Registro de incidentes

Cada incidente que requiera intervencion manual debe registrarse en un log compartido (Google Sheet, Notion o issue tracker) con:

- Fecha y hora.
- Numero involucrado (enmascarado si es publico, hash si es para registro interno).
- Severidad.
- Accion tomada.
- Tiempo de resolucion.
- Aprendizaje / mejora propuesta.

Revisar este registro en la retrospectiva semanal de la beta.
