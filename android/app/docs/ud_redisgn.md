# Instrucciones de Implementación UI en Jetpack Compose (PATOVA)

El objetivo es **replicar las 3 pantallas clave** mostradas en el mockup (`patova_app_mockup.html` / imagen), pero **implementándolas de forma nativa en Android usando Jetpack Compose**. 

A continuación se detallan los requisitos técnicos y de diseño que debes aplicar al crear los Composable functions para estas pantallas.

## 1. Tema y Paleta de Colores (Compose)
Debes crear o actualizar el `MaterialTheme` (o un sistema de diseño custom) con los siguientes colores en formato ARGB (`Color(0xFF...)`):

*   **Background (Fondo principal):** `Color(0xFF0A0F1E)`
*   **Surface (Tarjetas/Contenedores):** `Color(0xFF080D1A)` o `Color(0xFF161B22)`
*   **Text Primary:** `Color(0xFFE8F0FF)`
*   **Text Secondary:** `Color(0xFF8BA8CC)` o `Color(0xFF4A6080)`
*   **Colores Semánticos / Acentos:**
    *   **Danger/Blocked (Rojo):** `Color(0xFFE24B4A)`
    *   **Safe/Active (Verde):** `Color(0xFF1D9E75)`
    *   **Warning (Amarillo):** `Color(0xFFEF9F27)`
    *   **Premium/Action (Azul):** `Color(0xFF4A8CFF)`

## 2. Tipografía y Estilos
*   **Fuentes:** Usa `FontFamily.SansSerif` para la mayoría del texto. Para los números de teléfono (ej. `+54 11 4XXX-XXXX`), utiliza `FontFamily.Monospace` para lograr el efecto tabular.
*   **Pesos:** Usa `FontWeight.Medium` o `FontWeight.SemiBold` para títulos, números grandes y botones. Usa `FontWeight.Normal` para textos descriptivos y secundarios.
*   **Componentes UI:**
    *   **Píldoras (Tags):** Implementar con un `Surface` o `Box` con `RoundedCornerShape(percent = 50)` y un color de fondo con alfa (ej. `Color(0xFF1D9E75).copy(alpha = 0.15f)`).
    *   **Botones:** Usar `Button` para acciones principales (con `RoundedCornerShape(10.dp)`) y `OutlinedButton` o `TextButton` para acciones secundarias ("Permitir de todas formas").

## 3. Pantallas a Implementar (Composables)

Debes crear 3 Composables principales:

### A. `DashboardScreen` (Pantalla 1)
*   **Estructura:** Un `Scaffold` con un `BottomNavigation` o `NavigationBar` nativo de Compose en la parte inferior (iconos: Inicio, Historial, Red, Perfil).
*   **Header:** Logo y "Protección activa".
*   **Hero Section:** Un `Box` central con el ícono del escudo. **Importante:** El escudo debe ser un ícono limpio (relleno oscuro, borde azul) con un checkmark verde en el centro, sin líneas punteadas alrededor. Debajo, el texto gigante "47".
*   **Estadísticas:** Un `Row` que divida el ancho en 3 partes iguales (`weight(1f)`). Cada uno con un número de color (Rojo, Verde, Amarillo).
*   **Últimas llamadas:** Un `LazyColumn` o `Column` simple. Cada ítem (Row) debe tener: un ícono de estado con borde redondeado, una columna central con número de teléfono (monoespaciado) y estado, y el texto de tiempo a la derecha.

### B. `BlockedCallScreen` (Pantalla 2)
*   **Estructura:** `Column` centrada (`horizontalAlignment = Alignment.CenterHorizontally`).
*   **Elementos Visuales:** 
    *   Texto rojo "Llamada bloqueada".
    *   Escudo gigante rojo con una X.
    *   Número de teléfono en fuente Monospace.
    *   Píldora roja semi-transparente con el texto: **"Bot detectado - Base de spam PATOVA"** (Nota: usar "Base de spam", no "maestra").
    *   **Score de Riesgo:** Usar un `LinearProgressIndicator` modificado, con un 91% de progreso en color rojo, y el texto "91 / 100".
    *   Texto inferior: "Reportado por **2.647** usuarios PATOVA".
*   **Acciones:** Dos botones al final de la pantalla (ocupando `fillMaxWidth()`). El primero sólido ("Confirmar bloqueo"), el segundo de tipo Outline/Text ("Permitir de todas formas").

### C. `PremiumConversionScreen` (Pantalla 3)
*   **Estructura:** `Column` centrada.
*   **Elementos Visuales:**
    *   "Tu primera semana con PATOVA" (texto secundario).
    *   Número gigante azul "47".
    *   **Breakdown (Tarjeta):** Un `Card` o `Surface` oscuro con `RoundedCornerShape`. Dentro, un `Column` donde cada fila es un `Row` con `horizontalArrangement = Arrangement.SpaceBetween`. Los textos exactos a usar son:
        *   "País extranjero" (Valor: 23 rojo)
        *   "Vishing detectado" (Valor: 11 rojo)
        *   "Reiteradas no atendidas" (Valor: 5 rojo)
        *   "Reconocimiento de firma" (Valor: 4 rojo)
    *   Botón azul gigante "Activar PATOVA Premium".
    *   Textos descriptivos de precio y método de pago debajo del botón.

## Instrucción Final para el Agente
Por favor, genera el código de Jetpack Compose correspondiente a estas 3 pantallas (idealmente separadas en funciones Composable como `DashboardScreen()`, `BlockedCallScreen()`, y `PremiumScreen()`), aplicando la paleta de colores especificada y estructurando los layouts con `Column`, `Row`, `Box` y `Card`.