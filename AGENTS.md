# Reglas básicas para Eventia

## Estructura del proyecto

- Eventia es una app Android de un solo módulo (`app`) con Kotlin, Jetpack Compose y Material 3. El código principal vive en `app/src/main/java/com/example/eventia/` y las dependencias se declaran en `gradle/libs.versions.toml`.
- Conserva `MainActivity.kt` como punto de entrada y `ui/theme/` para el tema de Compose.
- Incorpora los siguientes paquetes al implementar sus flujos. La estructura sigue la referencia de PracticaAPILibros e incorpora las capas que exige Eventia.

| Paquete | Responsabilidad |
| --- | --- |
| `ui/navigation/` | Grafo, destinos y barra inferior. |
| `ui/screens/` | Pantallas, ViewModels y estados de presentación. |
| `api/` y `api/models/` | Interfaces Retrofit y DTO de red. |
| `domain/model/` | Modelos de dominio que consumen los ViewModels. |
| `repositories/` | Interfaces de repositorio, implementaciones y selección de la fuente de datos. |
| `data/local/` | Entidades y DAO de Room, base de datos y fuentes locales de DataStore. |
| `di/` | Módulos y bindings de Hilt. |

## Arquitectura y estado

- Mantén el flujo `Compose → ViewModel → interfaz de repositorio → fuente de datos`. Los composables muestran el estado y envían acciones. Los ViewModels coordinan el flujo de pantalla. Los repositorios encapsulan red y persistencia.
- Usa `@HiltViewModel` e inyección por constructor. Proporciona el servicio Retrofit, Room y DataStore mediante Hilt y reutiliza sus instancias según su alcance.
- Expón un `StateFlow` de solo lectura por pantalla con estados de carga, contenido, vacío y error. Consúmelo con `collectAsStateWithLifecycle()`. Durante una actualización, conserva los datos visibles si falla la solicitud y comunica el error.
- Usa `viewModelScope` para las acciones asíncronas. Usa `suspend` para operaciones puntuales y `Flow` para datos que cambian en el tiempo. Las operaciones suspendidas de Retrofit y Room no necesitan un `Dispatchers.IO` adicional. Reserva ese dispatcher para trabajo bloqueante y propaga `CancellationException`.
- Presenta mensajes comprensibles en español y acciones de reintento cuando correspondan. Mantén los detalles técnicos, tokens y credenciales fuera de los mensajes de UI.

## Navegación

- Mantén un solo `NavHost` raíz con `rememberNavController()`. `MainActivity` aplica el tema y monta la raíz de Compose.
- Centraliza las rutas de **Explorar**, **Mapa**, **Favoritos** y **Perfil**. Pasa únicamente el ID en los detalles de evento y lugar, preferiblemente mediante argumentos tipados.
- Envía las acciones de navegación desde las pantallas mediante callbacks. Conserva el estado de las pestañas y, al volver desde un detalle, los filtros y la posición del catálogo.

## Red, persistencia y mapas

- Usa Retrofit con Gson y la base `https://eventia.jmacboy.dev/api/`. Declara rutas relativas como `events` y `auth/login`, sin barra inicial. Consulta la sección 8 del enunciado y la colección Postman al modificar un contrato de red.
- Transforma los DTO y las entidades locales a modelos de dominio antes de entregarlos al ViewModel. Conserva la distinción entre `isAttending` ausente y `false`, admite imágenes y coordenadas nulas y conserva la zona horaria al interpretar las fechas ISO-8601.
- Usa Room para favoritos y caché del catálogo, DataStore para sesión y preferencias, y Coil para imágenes. El repositorio decide cuándo consultar red o persistencia local. Incluye Bearer cuando el endpoint requiere sesión o cuando corresponde enriquecer una consulta pública con la sesión activa.
- Integra el mapa con Maps Compose. Valida ambas coordenadas antes de crear un marcador. Lee `MAPS_API_KEY` desde `secrets.properties`, excluye ese archivo de Git y documenta el nombre de la variable y la configuración de la restricción por `applicationId` y SHA-1 en el README.

## Previews y verificación

- Crea previews para destinos principales, elementos de lista y paneles compuestos. Usa datos de ejemplo y callbacks simples para que puedan renderizarse sin ViewModels, repositorios ni servicios externos.
- Mantén las pruebas unitarias en `app/src/test/` y las instrumentadas en `app/src/androidTest/`. Prioriza los estados de pantalla y el comportamiento de los repositorios cuando cambies esos flujos.
- Actualiza el README cuando cambien la configuración del entorno, las dependencias relevantes o las instrucciones para ejecutar la app.
