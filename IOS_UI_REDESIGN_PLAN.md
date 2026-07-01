# Plan: llevar la identidad "patovica" a la app iOS

Contexto: ya se aplico el rediseño (paleta lima + negro-verdoso + mascota
"patovica") en Android. Este documento es el plan para replicarlo en la app
iOS (`ios/`, SwiftUI), para retomarlo en otra sesion.

## Diagnostico del estado actual de iOS

A diferencia de Android (que estaba en azul/navy generico), iOS **ya arranco
mas cerca** de la identidad de marca, pero de forma inconsistente:

- Cada vista (`DashboardView.swift`, `NetworkView.swift`, `MiPatovaView.swift`,
  `SuccessCelebrationView.swift`) redeclara sus propios colores locales
  (`private let patovaGreen`, `patovaDark`, `patovaCard`, `patovaSurface`,
  etc.) en vez de usar un theme centralizado. Hay duplicacion.
- El verde usado (`Color(red: 0.00, green: 0.78, blue: 0.33)`, un verde
  esmeralda/kelly) **no es el mismo lima** del landing/mascota
  (`oklch(0.82 0.17 150)` ~ `#B4E33D`, usado como `BrandLime` en Android).
  Hay que decidir si unificar a ese lima exacto o mantener este verde como
  variante iOS (recomendado: unificar, para que Android/iOS/landing se vean
  como la misma marca).
- `MainTabView.swift` todavia tiene el tab bar activo en **azul**
  (`UIColor(red: 0.18, green: 0.48, blue: 0.96, alpha: 1.0)`) - el mismo
  "azul premium" generico que se reemplazo en Android. Pendiente de cambiar.
- `NetworkView.swift` (pantalla "Red") **ya tiene un globo 3D real** hecho con
  SceneKit (`globeMaterial`, `wireMaterial`, lineas ~340-360) con colores
  verdes. A diferencia de Android, ahi no hace falta agregar un motor 3D
  nuevo (Filament/SceneView) - solo revisar que el material del globo use el
  lima final una vez decidido.
- **No existe ningun asset ni vista de la mascota "patovica"** en iOS (cero
  resultados al buscar "mascot"/"patovica" en el codigo). Hay que construirla
  nativa en SwiftUI (Shape/Path), equivalente a `PatovaMascot.kt` en Android
  (circulo de cabeza + banda de anteojos + hombros/brazos cruzados, todo en
  un solo color tintable).
- No hay una pantalla equivalente a `BlockedCallScreen.kt` de Android: iOS
  bloquea llamadas a nivel de sistema via CallKit (`CallDirectoryHandler.swift`,
  una extension sin UI propia), asi que no hay pantalla de "llamada
  bloqueada" que mostrar. El momento de marca mas parecido/"celebratorio"
  que existe hoy es `SuccessCelebrationView.swift` (se muestra al activar
  Premium, con un icono de escudo+check en verde) - buen candidato para
  poner ahi la mascota en vez del icono generico de SF Symbols.

## Plan de trabajo (mismo orden que se uso en Android)

1. **Centralizar la paleta.** Crear `ios/PatovaColors.swift` con los colores
   de marca (lima, negro-verdoso en 3-4 tonos de superficie, texto, y los
   semanticos: rojo peligro, ambar warning, dorado premium ya usado en
   Paywall). Reemplazar los `private let patovaGreen = ...` duplicados en
   cada vista por referencias a este archivo central, igual que se hizo con
   `Color.kt` en Android (nombres estables, valores centralizados).
2. **Arreglar el tab bar.** En `MainTabView.swift`, cambiar el azul
   `(0.18, 0.48, 0.96)` del item seleccionado por el lima de marca.
3. **Mascota nativa en SwiftUI.** Crear `ios/PatovaMascotView.swift`: un
   `Shape`/`Path` con cabeza + banda de anteojos + hombros cruzados,
   parametrizable por color y tamaño (mismo diseño que `PatovaMascot.kt`,
   solo trasladado a SwiftUI `Path`).
4. **Aplicar la mascota en los momentos de marca:**
   - `SuccessCelebrationView.swift`: reemplazar el icono de escudo+check por
     la mascota dentro del badge circular.
   - Header de `NetworkView.swift` ("Red"): mismo tratamiento que se le dio
     al ícono grande en Android (badge circular con la mascota o mantener el
     globo 3D ya existente, a definir).
5. **Revisar el globo 3D de `NetworkView.swift`** y recolorear los
   materiales (`globeMaterial`, `wireMaterial`) al lima final una vez
   unificada la paleta.
6. **Pasada de verificacion:** confirmar que no queden los verdes/azules
   viejos hardcodeados sueltos (grep de los valores RGB viejos), y que cada
   archivo `.swift` tocado compile (no hay forma de compilar Swift en este
   entorno - haria falta abrir el proyecto en Xcode para confirmar).

## Archivos involucrados

- `ios/PatovaColors.swift` (nuevo)
- `ios/PatovaMascotView.swift` (nuevo)
- `ios/MainTabView.swift`
- `ios/DashboardView.swift`
- `ios/NetworkView.swift`
- `ios/MiPatovaView.swift`
- `ios/SuccessCelebrationView.swift`
- `ios/PaywallView.swift` (revisar si tambien tiene azules/verdes sueltos que
  conviene centralizar, sin tocar su paleta dorada de VIP que es intencional)

## Pendiente de decidir con Lautaro antes de arrancar

- Unificar el verde de iOS al lima exacto de Android/landing, o dejarlo como
  variante propia de iOS (mas cercano a lo que ya tiene hoy, cambio mas
  chico).
- Que hacer en el header de "Red": ¿mascota en badge, o dejar el globo 3D
  que ya existe y solo recolorearlo?
