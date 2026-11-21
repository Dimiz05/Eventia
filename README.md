# Eventia

Aplicación Android desarrollada en Kotlin con Jetpack Compose y Material 3. El proyecto utiliza Gradle con Kotlin DSL.

## Requisitos

- Android Studio con el SDK de Android API 37 instalado.
- Un emulador o dispositivo físico con Android 13 (API 33) o superior.
- Para la primera ejecución del Día 1, selecciona un dispositivo con Google Play Services o un emulador con imagen Google Play.

## Proyecto y navegación base

- ID de aplicación `com.example.eventia`.
- Navigation Compose presenta Explorar, Mapa, Favoritos y Perfil. El grafo reserva `events/{id}` y `venues/{id}` con argumentos numéricos para conectar los detalles cuando exista contenido.
- Explorar consulta las categorías, los eventos y los lugares públicos de `https://eventia.jmacboy.dev/api/` mediante un ViewModel con StateFlow, un repositorio y Retrofit con Gson. Hilt inyecta Retrofit y las interfaces de repositorio. Los repositorios convierten los DTO en modelos de dominio antes de que el ViewModel publique el estado de carga, contenido, vacío o error. El estado de error permite reintentar.
- Room, DataStore y Coil están declarados en el catálogo de versiones para los flujos que los usarán. Las pantallas de Mapa, Favoritos, Perfil y detalle son destinos base.

## Día 1 — Arquitectura y consumo de API

En Android, Retrofit utiliza `https://eventia.jmacboy.dev/api/` y las rutas relativas `categories`, `events`, `venues` y `auth/login`. El catálogo usa la fuente remota. El contrato de inicio de sesión recibe `email` y `password` y lee `token`. La UI de sesión y la caché local corresponden a flujos posteriores.

### Postman

1. Importa [la colección del Día 1](postman/Eventia-Dia-1.postman_collection.json). La variable `baseUrl` ya apunta a `https://eventia.jmacboy.dev/api`.
2. En **Lecturas públicas**, ejecuta **Categorías**, **Eventos (catálogo completo)** y **Lugares**, en ese orden. Los scripts guardan `eventId`, `venueId` y, si aparece un evento con cero cupos, `fullEventId`. Usa el detalle de evento **sin token** con **No Auth**.
3. Configura `courseEmail` y `coursePassword` en un entorno privado con las credenciales facilitadas por el curso. Ejecuta **Iniciar sesión**, **Usuario autenticado** y **Detalle de evento**. El script de inicio de sesión guarda `token`, que las dos lecturas envían como `Authorization: Bearer {{token}}`.
4. Para inscribirse, cancelar o consultar inscripciones, configura `attendanceToken` con el token de una cuenta de prueba dedicada y comprueba los IDs contra el catálogo. Si `fullEventId` está vacío, queda pendiente el caso 409 por falta de cupos. Mantén las credenciales y los tokens solo en valores locales y limpia los tokens antes de compartir o exportar la colección.

### Lecturas públicas repetidas el 20 de noviembre de 2026

Las consultas por HTTP a **categorías → eventos → lugares** devolvieron 200 con arreglos de 7, 21 y 10 elementos. El detalle de invitado del evento 8 devolvió 200 y omitió `isAttending`. El lugar 8 tenía ambas coordenadas nulas y el 10 tenía solo la longitud nula. Se observaron imágenes nulas en los eventos 16, 21, 24 y 32. El evento 8 tenía 60 cupos totales y 60 disponibles. Ningún evento devuelto tenía cero cupos, por lo que `fullEventId` queda sin un valor verificado.

El evento 14 confirmó `startsAt: 2026-11-22T18:00:00-04:00` y `endsAt: 2026-11-22T20:00:00-04:00`.

### Observaciones de la ejecución anterior

Los resultados autenticados de la tabla proceden de la verificación previa. La importación en Postman, el inicio de sesión y las solicitudes con Bearer quedan pendientes de repetir con un entorno privado y las cuentas de prueba autorizadas.

| Solicitud | Resultado observado |
| --- | --- |
| `GET /categories` | 200. Arreglo de 7 objetos con `id` y `name`. Ejemplo válido: categoría 7. |
| `GET /events` | 200. Arreglo de 21 eventos con categoría y lugar anidados. El evento 8 tenía 60 cupos totales y 60 disponibles. El catálogo devuelto no contenía ningún evento con cero cupos disponibles. |
| `GET /venues` | 200. Arreglo de objetos con dirección, descripción, horario, imagen, coordenadas y conteo de próximos eventos. El lugar 8 tenía ambas coordenadas nulas y el 10 solo longitud nula. |
| `GET /events/8` | 200 en ambos casos. Sin encabezado de autorización se omitió `isAttending`. Con Bearer se recibió `isAttending: false`. |
| `GET /venues/1` | 200. Devuelve un objeto con la misma forma que los elementos de la lista de lugares. |
| `POST /auth/login` y `GET /me` | 200 con la cuenta del curso y su token. `GET /me` sin token devolvió 401. |

El evento 16 devolvió `imageUrl: null`. El evento 14 devolvió `startsAt: 2026-11-22T18:00:00-04:00` con offset `-04:00`. Los DTO admiten esos nulos y el dominio conserva el offset con `OffsetDateTime`.

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
