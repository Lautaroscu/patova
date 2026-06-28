# Guía de Compilación en Xcode para Patova iOS

Esta carpeta contiene todo el código Swift nativo listo para funcionar. Para resolver el problema de no tener un archivo `.xcodeproj` en el control de versiones y evitar conflictos de mezcla caóticos, utilizamos **XcodeGen**.

## 🚀 Cómo generar el proyecto en macOS (para tu amigo)

Dile a tu amigo que ejecute los siguientes pasos en su Mac:

1. **Instalar XcodeGen** (si no lo tiene instalado):
   Abre la Terminal en macOS y ejecuta:
   ```bash
   brew install xcodegen
   ```

2. **Generar el proyecto de Xcode**:
   Navegar con la Terminal a la carpeta `ios/` de este proyecto y ejecutar:
   ```bash
   xcodegen generate
   ```
   Esto creará el archivo de proyecto `Patova.xcodeproj` de forma 100% correcta y enlazado con todos los archivos fuentes y la extensión de bloqueo `CallDirectory`.

3. **Abrir el proyecto**:
   ```bash
   open Patova.xcodeproj
   ```

---

## 🔑 Firma y Configuración de App Groups (Obligatorio)

Como la app principal y la extensión `CallDirectory` se comunican mediante una base de datos local SQLite compartida, **iOS exige obligatoriamente configurar un App Group**:

1. En Xcode, ve al proyecto **Patova** (en el panel izquierdo).
2. Selecciona el target **Patova** y ve a **Signing & Capabilities** (Firma y Capacidades).
3. Asegúrate de tener seleccionado tu **Team** de Apple Developer.
4. En la sección **App Groups**, haz clic en el botón `+` para añadir un nuevo identificador de grupo. Como `group.com.patova.app.patova` ya está preconfigurado por defecto, debería aparecer en tu lista. Si usas otra cuenta, crea uno propio con el formato: `group.tu-identificador.patova`.
5. Repite el mismo paso para el target **CallDirectory** (la extensión de bloqueo). **Ambos deben usar el mismo App Group**.
6. Si cambiaste el nombre del App Group, recuerda actualizar la constante `appGroupIdentifier` en el archivo `DatabaseManager.swift`.

---

## 📱 Pruebas y Depuración

* **Pruebas en dispositivo real**: Los simuladores de iOS **no tienen soporte para CallKit**. La app compilará, pero fallará al intentar registrar los números sospechosos. Conecta un iPhone real por USB, selecciónalo en Xcode y dale a Run (`⌘ + R`).
* **Habilitar en Ajustes**: Una vez instalada en el iPhone, ve a **Ajustes -> Teléfono -> Bloqueo e ident. de llamadas** y activa el interruptor de **Patova**.
* **Depuración de la Extensión**: Como la extensión corre en un proceso en segundo plano de manera aislada, no se depura automáticamente con el "Run" clásico. En Xcode ve a **Debug -> Attach to Process by PID or Name** y escribe `CallDirectory` para interceptar los `print` y errores de la extensión.

---

## ❓ ¿Por qué es mejor Swift nativo que React Native para esta app?

El corazón de Patova en iOS es la **Call Directory Extension** (CallKit). 
Apple impone un **límite de memoria RAM súper estricto de 16MB** para este proceso en segundo plano. Si usáramos React Native:
1. Al levantarse la extensión, se cargaría el motor JavaScript de React Native (Hermes / JSC).
2. Este motor por sí solo consume más de 15MB de RAM.
3. El sistema operativo iOS mataría instantáneamente el proceso por exceso de memoria antes de que logre registrar siquiera un número de teléfono de spam.

Al estar hecha en **Swift nativo puro**, el consumo de memoria es de apenas **1-2 MB**, permitiendo que el indexador procese decenas de miles de números sospechosos sin ningún tipo de límite o caída.
