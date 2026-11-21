# Eventia

Aplicación Android desarrollada en Kotlin con Jetpack Compose y Material 3. El proyecto utiliza Gradle con Kotlin DSL.

## Requisitos

- Android Studio con el SDK de Android API 37 instalado.
- Un emulador o dispositivo físico con Android 13 (API 33) o superior.
- Para la primera ejecución del Día 1, selecciona un dispositivo con Google Play Services o un emulador con imagen Google Play.

## Proyecto y navegación base

- ID de aplicación `com.example.eventia`.
- Navigation Compose presenta Explorar, Mapa, Favoritos y Perfil. El grafo reserva `events/{id}` y `venues/{id}` con argumentos numéricos para conectar los detalles cuando exista contenido.
- Explorar consulta las categorías públicas de `https://eventia.jmacboy.dev/api/` mediante un ViewModel con StateFlow, un repositorio y Retrofit con Gson. Hilt conecta las dependencias.
- Room, DataStore y Coil están declarados en el catálogo de versiones para los flujos que los usarán. Las pantallas de Mapa, Favoritos, Perfil y detalle son destinos base.

## Cómo ejecutar el proyecto

1. Abre la carpeta del repositorio en Android Studio con **File > Open**.
2. Espera a que termine la sincronización de Gradle. Si falta el SDK, instálalo desde **Tools > SDK Manager**.
3. Selecciona un emulador o conecta un dispositivo con la depuración USB activada.
4. Selecciona la configuración `app` y pulsa **Run**.

## Compilación y pruebas

Ejecuta estos comandos desde la raíz del repositorio:

| Tarea | Windows (PowerShell) | macOS / Linux |
| --- | --- | --- |
| Compilar el APK de depuración | `.\gradlew.bat assembleDebug` | `./gradlew assembleDebug` |
| Ejecutar pruebas unitarias | `.\gradlew.bat testDebugUnitTest` | `./gradlew testDebugUnitTest` |
| Ejecutar pruebas instrumentadas | `.\gradlew.bat connectedDebugAndroidTest` | `./gradlew connectedDebugAndroidTest` |

Las pruebas instrumentadas requieren un emulador en ejecución o un dispositivo conectado. El APK de depuración se genera en `app/build/outputs/apk/debug/`.

## Estructura del proyecto

- `app/src/main/`: código fuente, recursos y manifiesto de la aplicación.
- `app/src/test/`: pruebas unitarias locales.
- `app/src/androidTest/`: pruebas instrumentadas en Android.
- `gradle/libs.versions.toml`: catálogo de versiones y dependencias.
