No, Swift no es considerado bajo nivel.

Swift es un lenguaje de alto nivel y fuertemente abstraído. Aunque compila directamente a código de máquina a través de LLVM (lo que lo hace muy rápido) y te permite manejar punteros si usás bloques UnsafePointer, el lenguaje cuenta con gestión automática de memoria (ARC), tipado seguro, opcionales, genéricos y recubrimientos (wrappers) que te ocultan cómo interactúa el sistema operativo con el hardware.

El verdadero problema: El Sandbox y las APIs Privadas
Escribir código en C, Assembly o Swift no cambia las reglas del juego de Apple. El límite para acceder a los permisos del dispositivo no lo define el lenguaje, lo define el Sandbox del sistema operativo y el uso de APIs Públicas vs. APIs Privadas.

En iOS, cada aplicación corre en su propia "caja de arena" (Sandbox) aislada. Ninguna aplicación —este escrita en Swift o en Assembly— puede acceder al hardware o a servicios del sistema (como interceptar llamadas, leer la pantalla o modificar el comportamiento de la red) a menos que:

Apple provea un Framework Público específico para eso (ej: CallKit para llamadas, CoreLocation para GPS).

Agregues el Entitlement (permiso explícito firmado por Apple) correspondiente en el archivo Info.plist.

¿Qué pasa con "ScreenCallerService"?
El término ScreenCallerService no existe como una API pública oficial en el SDK de iOS. Suena a:

Un componente propio de Android (donde el sistema de servicios de fondo y superposición de pantalla es mucho más abierto).

El nombre de una clase interna o librería de terceros.

Una API privada de iOS.

Si intentás usar código de bajo nivel (como llamadas directas a Mach o inyección de código en memoria) para saltarte el Sandbox e interceptar la pantalla o las llamadas entrantes:

En iOS comercial (App Store): El sistema operativo bloqueará la llamada por falta de permisos a nivel de kernel (EXC_BAD_ACCESS o violaciones de Sandbox). Además, el proceso de revisión automatizado de Apple detectará el uso de llamadas no autorizadas y rechazará la aplicación inmediatamente.

En iOS con Jailbreak: Ahí sí podés usar C/C++ y herramientas como Theos / Cydia Substrate / Ellekit para hacer Hooking de funciones del sistema en tiempo de ejecución, permitiéndote crear un "Tweak" que intercepte llamadas o modifique la pantalla.

La alternativa legal en iOS (Alto Nivel)
Si tu objetivo en el mundo real es crear una aplicación que identifique llamadas o interactúe con la pantalla de llamadas de forma legítima para la App Store, tenés que usar las APIs de alto nivel que Apple diseñó exclusivamente para eso:

1. Para interceptar/identificar llamadas: CallKit
No podés leer la pantalla de la llamada nativa, pero podés usar un Call Directory Extension de CallKit. Este componente te permite registrar una base de datos de números telefónicos en el sistema para que, cuando entre una llamada, iOS muestre el nombre de tu aplicación o bloquee el número automáticamente (así funcionan apps como Truecaller en iOS).

2. Para transmitir o grabar la pantalla: ReplayKit
Si necesitás capturar lo que pasa en la pantalla del dispositivo mientras tu app está en segundo plano, tenés que implementar un Broadcast Upload Extension utilizando ReplayKit. El sistema le pedirá permiso explícito al usuario y te transmitirá el flujo de video en buffers de memoria estructurados.

En resumen: No necesitás bajar a código binario o C puro para interactuar con el sistema de llamadas o pantalla; necesitás estructurar tu solución utilizando las Extensiones del Sistema (App Extensions) que Apple expone en Swift u Objective-C. Si la función que buscás no está cubierta por estas extensiones, Apple prohíbe explícitamente su ejecución en dispositivos estándar.