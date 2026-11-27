# Eventia visual redesign and UX
Branch: design/general-2. Delivery: one local branch. Runner: Gradle debug and Android instrumentation.
Estimated change: approximately 380 authored lines per phase before verification.

## Specs

### S1
Implementa el rediseño visual global de Eventia en el proyecto Android existente con Kotlin, Jetpack Compose y Material 3. Usa «Tres eventos destacados en Explorar.png» como referencia visual principal. Antes de editar, revisa el tema, la tipografía, la navegación raíz, los encabezados y los componentes de tarjetas existentes.

1. Tema y tipografía globales

- Aplica Crimson Text Regular a toda la interfaz mediante la Typography de Material 3. Usa el archivo de fuente oficial como recurso local en res/font e incluye su licencia. Usa peso Regular (400) y construye la jerarquía con el tamaño y el espaciado.
- Usa estas medidas como base: 28 sp para títulos de pantalla, 22 sp para nombres de eventos, 16 sp para buscadores y selectores, 14 sp para fechas, lugares, filtros y categorías, y 13 sp para etiquetas de navegación. Respeta el escalado de texto del sistema.
- Usa blanco puro (#FFFFFF) en fondos generales, buscadores y barra inferior. Usa verde salvia oscuro para acciones y estados activos, salvia claro para etiquetas y superficies seleccionadas, y gris verdoso oscuro para texto secundario. Centraliza los tonos en el tema y revisa el contraste del texto sobre cada superficie.
- Aplica el tema también en Mapa, Favoritos, Perfil, login, registro, detalle del evento y detalle del lugar. En arranque y autenticación, conserva las ilustraciones y el rótulo gráfico de Eventia. Mantén visibles las imágenes, los degradados y los mapas donde forman parte del diseño.

2. Encabezados y avatar

- En las pantallas principales que tienen encabezado, alinea el título a la izquierda y añade a la derecha un avatar circular pequeño, con borde salvia fino y área táctil de al menos 48 dp. En Explorar debe verse alineado con «Explorar», encima del buscador, como en el mockup.
- Reutiliza la foto o el avatar de la cuenta. Usa un icono genérico cuando falte la foto o se navegue como invitado. Al pulsarlo, abre la pestaña Perfil con su flujo actual.
- En Mapa, mantén el diseño sin barra superior y coloca el avatar junto al buscador flotante, sin cubrir los filtros ni los controles del mapa. En las pantallas de detalle, conserva los controles de Volver, Favorito y Compartir donde correspondan.

3. Barra inferior compartida

- Aplica la misma barra flotante a Explorar, Mapa, Favoritos y Perfil. Usa una superficie blanca con esquinas redondeadas, margen lateral y una sombra suave que aporte profundidad. Elimina la línea divisoria superior.
- Conserva los cuatro destinos en ese orden y sus etiquetas debajo. Elimina el indicador en forma de cápsula detrás de cada icono.
- El icono de la pestaña activa se rellena en verde salvia oscuro. Los otros se muestran delineados, con el interior blanco. Al cambiar de pantalla, el icono anterior vuelve al contorno. Usa la ruta actual como fuente del estado seleccionado para que el avatar, la barra y el botón Atrás queden sincronizados.
- Respeta el área del sistema y reserva el espacio que ocupa la barra para que el contenido y los paneles del mapa sigan accesibles. Conserva las reglas existentes sobre dónde se muestra la navegación inferior.

4. Pantalla Explorar

- Reproduce el orden del mockup: título con avatar, buscador blanco, selector «Eventos / Lugares», fila de filtros y catálogo. En la vista inicial, activa «Eventos» y «Todos». Muestra los accesos rápidos «Todos», «Música» y «Gastronomía» con icono y texto, y usa «Filtros» para acceder a las demás categorías reales.
- En el listado de eventos, muestra tres tarjetas panorámicas completas, de la misma altura, apiladas en una sola columna y separadas por un espacio uniforme. Calcula la altura de las tarjetas con el espacio disponible entre los filtros y la barra inferior. Si hay más eventos, aparecen al deslizar. En pantallas muy bajas o con fuente ampliada, prioriza la legibilidad y permite el desplazamiento.
- Conserva la foto a todo el ancho y el degradado oscuro. Coloca la fecha y hora arriba a la izquierda, el corazón arriba a la derecha, el nombre y el lugar abajo a la izquierda, y la categoría abajo a la derecha. Deja que los títulos largos ocupen hasta dos líneas y evita que el lugar se solape con la etiqueta de categoría.
- Usa los datos reales y el recurso sustituto existente si falla la foto. Tocar la tarjeta abre el detalle. Tocar el corazón guarda o quita el favorito sin navegar. Reutiliza Room y los estados actuales de búsqueda, filtros, carga, error, vacío y posición de desplazamiento.

5. Verificación

- Compila la variante debug. Verifica que el tema blanco y salvia y Crimson Text Regular se apliquen en todas las pantallas de Eventia.
- En un emulador vertical con escala de texto predeterminada, comprueba que se vean tres tarjetas completas, que la cuarta aparezca al deslizar y que la barra no cubra la última tarjeta.
- Prueba las cuatro pestañas, el cambio de contorno a relleno, el acceso a Perfil desde el avatar, el modo invitado y la navegación Atrás. Revisa también una pantalla pequeña, texto ampliado y el panel «Eventos en este punto» del mapa.
- Mantén los flujos de sesión, catálogo, favoritos, inscripciones y mapa existentes. Al finalizar, indica los archivos modificados y las comprobaciones que ejecutaste.

### S2
Revisa y mejora la experiencia de usuario de toda Eventia en el proyecto Android existente, con Kotlin, Jetpack Compose y Material 3. Implementa cambios concretos en la interfaz y en sus interacciones. Usa como base el diseño aprobado: fondo blanco, acentos verde salvia, Crimson Text Regular, navegación inferior flotante e iconos activos rellenos.

Revisa primero el tema, el grafo de navegación, los componentes compartidos y los flujos reales de cada pantalla. Aprovecha los componentes y estados existentes. Mantén el alcance en el cliente Android y conserva el contrato de la API.

## 1. Aplica estos principios a decisiones concretas

1. Ley de Tesler. Deja que la app resuelva trabajo repetitivo, como restaurar filtros al volver de un detalle, formatear fechas y conservar los campos válidos después de un error de formulario.
2. Carga cognitiva. Define una acción principal por contexto. Reduce textos repetidos y deja visible la información necesaria para decidir qué hacer.
3. Ley de proximidad. Coloca juntos los elementos relacionados: fecha y hora, nombre y lugar, campo y mensaje de error, botón de asistencia y cupos.
4. Regla del pico y el final. Cuida el cierre de acciones importantes. Después de guardar un favorito o confirmar una inscripción, muestra el resultado real y deja claro qué puede hacer el usuario a continuación.
5. Principio de Pareto. Da acceso directo a las tareas más útiles para el flujo principal: buscar, filtrar, abrir un evento y guardarlo. Usa el mockup de Explorar como punto de partida para los accesos rápidos.
6. Ley de similitud. Usa iconos, etiquetas, tamaños y estados equivalentes para acciones equivalentes en toda la app. El corazón debe comunicar el mismo estado en Explorar, Mapa, Favoritos y detalle.
7. Divulgación progresiva. Muestra primero el resumen y deja el contenido adicional detrás de controles claros como «Filtros», «Ver más» o «Gestionar inscripción», según corresponda.
8. Efecto de estética y usabilidad. Cuida alineación, ritmo visual y consistencia. Comprueba también el contraste sobre fotos, el ajuste de textos largos y las áreas táctiles.
9. Efecto de posición serial. Coloca los accesos prioritarios al inicio o al final de grupos breves. Mantén «Todos» al inicio y «Filtros» al final de los accesos rápidos de Explorar.
10. Reconocer en vez de recordar. Acompaña los iconos de filtros con etiquetas visibles, identifica claramente el filtro activo y usa verbos concretos como «Cómo llegar» y «Ver detalle».
11. Ley de Parkinson. Reduce pasos y esperas evitables en tareas simples. Permite aplicar un filtro rápido con un toque, muestra el progreso de las operaciones asíncronas y deja las confirmaciones para acciones que realmente las necesitan.
12. Ley de región común. Agrupa controles relacionados dentro de una superficie compartida, como el selector «Eventos / Lugares» o «Próximas / Pasadas».
13. Efecto por defecto. En la primera entrada a Explorar, activa «Eventos» y «Todos». Respeta la selección del usuario cuando vuelva de otra pantalla. Las acciones que cambian datos deben partir de una decisión explícita.
14. Paradoja del usuario activo. Permite empezar a explorar de inmediato. Da ayuda breve en el momento en que se necesita, como al solicitar ubicación desde «Mi ubicación».
15. Conexión uniforme. Vincula visualmente cada acción con el elemento que modifica. En una lista con varios eventos, cada corazón y cada «Ver detalle» deben pertenecer claramente a su tarjeta.

## 2. Aplícalos en todas las pantallas

- Arranque. Conserva la vista ilustrada y la transición automática. Resuelve la sesión en paralelo y avanza cuando el destino esté listo.
- Inicio de sesión y registro. Mantén una acción principal clara. Coloca errores junto al campo correspondiente, conserva los datos válidos tras un fallo y deja visible «Continuar sin cuenta» en el login.
- Explorar. Conserva el título con avatar, el buscador, el selector y los filtros rápidos. Muestra el filtro activo y ofrece una forma directa de limpiarlo. Mantén tres tarjetas panorámicas completas cuando el espacio lo permita. Al volver del detalle, recupera la búsqueda, los filtros y la posición de la lista.
- Mapa. Conserva el buscador blanco y los filtros con iconos. Distingue el pin seleccionado con tamaño y aro blanco. En «Eventos en este punto», asocia «Cómo llegar» y «Ver detalle» al evento correcto. Conserva el acceso al catálogo del mapa si el usuario está lejos de los pines y permite explorar aunque deniegue la ubicación.
- Favoritos. Usa el mismo corazón que en el resto de Eventia. Cuando la lista esté vacía, explica brevemente cómo guardar un evento y ofrece un acceso directo a Explorar. Si quitar un favorito es reversible mediante el flujo existente, ofrece «Deshacer» cuando sea práctico.
- Detalle del evento. Agrupa categoría, nombre y fecha en el encabezado. Conserva la ubicación, el minimapa y «Cómo llegar» juntos. En la barra inferior, muestra «Asistiré» junto a los cupos. Actualiza el estado de asistencia tras recibir el resultado real de la operación.
- Detalle del lugar. Agrupa nombre, horario y contador de próximos eventos en el encabezado. Coloca dirección, mapa y «Cómo llegar» en la sección de ubicación. Mantén claramente separada la lista de próximos eventos.
- Perfil. Mantén compactos los datos de cuenta y diferencia «Cerrar sesión» de la gestión de inscripciones. Conserva «Próximas / Pasadas» con sus contadores y los tickets compactos. Coloca «Gestionar inscripción» dentro del ticket correspondiente.
- Estados compartidos. Usa una estructura consistente para carga, error, vacío y sin conexión. Cuando haya una acción útil, nómbrala según el contexto, por ejemplo «Reintentar», «Limpiar filtros» o «Explorar eventos».

## 3. Verifica el resultado

Compila la variante debug y prueba recorridos completos en el emulador: entrar como invitado, buscar y filtrar, abrir un evento y volver, guardar y quitar un favorito, seleccionar un pin, denegar ubicación y gestionar una inscripción. Comprueba una pantalla pequeña y una con texto ampliado. Revisa que la barra flotante no cubra acciones, que los estados se distingan sin depender solo del color y que los controles con icono tengan etiquetas accesibles.

Al finalizar, resume por pantalla qué cambiaste, qué tarea se simplificó y qué pruebas ejecutaste. Si algún principio ya estaba bien resuelto, describe el comportamiento existente que verificaste.

## Tasks

- [x] T1 — S1 — inline, shared theme/navigation/catalog — c07d30b
- [x] T2 — S2 — inline, UX improvements on the redesigned client — c07d30b

## Log

L1 — Original request

Implementa el rediseño visual global de Eventia en el proyecto Android existente con Kotlin, Jetpack Compose y Material 3. Usa «Tres eventos destacados en Explorar.png» como referencia visual principal. Antes de editar, revisa el tema, la tipografía, la navegación raíz, los encabezados y los componentes de tarjetas existentes.

1. Tema y tipografía globales

- Aplica Crimson Text Regular a toda la interfaz mediante la Typography de Material 3. Usa el archivo de fuente oficial como recurso local en res/font e incluye su licencia. Usa peso Regular (400) y construye la jerarquía con el tamaño y el espaciado.
- Usa estas medidas como base: 28 sp para títulos de pantalla, 22 sp para nombres de eventos, 16 sp para buscadores y selectores, 14 sp para fechas, lugares, filtros y categorías, y 13 sp para etiquetas de navegación. Respeta el escalado de texto del sistema.
- Usa blanco puro (#FFFFFF) en fondos generales, buscadores y barra inferior. Usa verde salvia oscuro para acciones y estados activos, salvia claro para etiquetas y superficies seleccionadas, y gris verdoso oscuro para texto secundario. Centraliza los tonos en el tema y revisa el contraste del texto sobre cada superficie.
- Aplica el tema también en Mapa, Favoritos, Perfil, login, registro, detalle del evento y detalle del lugar. En arranque y autenticación, conserva las ilustraciones y el rótulo gráfico de Eventia. Mantén visibles las imágenes, los degradados y los mapas donde forman parte del diseño.

2. Encabezados y avatar

- En las pantallas principales que tienen encabezado, alinea el título a la izquierda y añade a la derecha un avatar circular pequeño, con borde salvia fino y área táctil de al menos 48 dp. En Explorar debe verse alineado con «Explorar», encima del buscador, como en el mockup.
- Reutiliza la foto o el avatar de la cuenta. Usa un icono genérico cuando falte la foto o se navegue como invitado. Al pulsarlo, abre la pestaña Perfil con su flujo actual.
- En Mapa, mantén el diseño sin barra superior y coloca el avatar junto al buscador flotante, sin cubrir los filtros ni los controles del mapa. En las pantallas de detalle, conserva los controles de Volver, Favorito y Compartir donde correspondan.

3. Barra inferior compartida

- Aplica la misma barra flotante a Explorar, Mapa, Favoritos y Perfil. Usa una superficie blanca con esquinas redondeadas, margen lateral y una sombra suave que aporte profundidad. Elimina la línea divisoria superior.
- Conserva los cuatro destinos en ese orden y sus etiquetas debajo. Elimina el indicador en forma de cápsula detrás de cada icono.
- El icono de la pestaña activa se rellena en verde salvia oscuro. Los otros se muestran delineados, con el interior blanco. Al cambiar de pantalla, el icono anterior vuelve al contorno. Usa la ruta actual como fuente del estado seleccionado para que el avatar, la barra y el botón Atrás queden sincronizados.
- Respeta el área del sistema y reserva el espacio que ocupa la barra para que el contenido y los paneles del mapa sigan accesibles. Conserva las reglas existentes sobre dónde se muestra la navegación inferior.

4. Pantalla Explorar

- Reproduce el orden del mockup: título con avatar, buscador blanco, selector «Eventos / Lugares», fila de filtros y catálogo. En la vista inicial, activa «Eventos» y «Todos». Muestra los accesos rápidos «Todos», «Música» y «Gastronomía» con icono y texto, y usa «Filtros» para acceder a las demás categorías reales.
- En el listado de eventos, muestra tres tarjetas panorámicas completas, de la misma altura, apiladas en una sola columna y separadas por un espacio uniforme. Calcula la altura de las tarjetas con el espacio disponible entre los filtros y la barra inferior. Si hay más eventos, aparecen al deslizar. En pantallas muy bajas o con fuente ampliada, prioriza la legibilidad y permite el desplazamiento.
- Conserva la foto a todo el ancho y el degradado oscuro. Coloca la fecha y hora arriba a la izquierda, el corazón arriba a la derecha, el nombre y el lugar abajo a la izquierda, y la categoría abajo a la derecha. Deja que los títulos largos ocupen hasta dos líneas y evita que el lugar se solape con la etiqueta de categoría.
- Usa los datos reales y el recurso sustituto existente si falla la foto. Tocar la tarjeta abre el detalle. Tocar el corazón guarda o quita el favorito sin navegar. Reutiliza Room y los estados actuales de búsqueda, filtros, carga, error, vacío y posición de desplazamiento.

5. Verificación

- Compila la variante debug. Verifica que el tema blanco y salvia y Crimson Text Regular se apliquen en todas las pantallas de Eventia.
- En un emulador vertical con escala de texto predeterminada, comprueba que se vean tres tarjetas completas, que la cuarta aparezca al deslizar y que la barra no cubra la última tarjeta.
- Prueba las cuatro pestañas, el cambio de contorno a relleno, el acceso a Perfil desde el avatar, el modo invitado y la navegación Atrás. Revisa también una pantalla pequeña, texto ampliado y el panel «Eventos en este punto» del mapa.
- Mantén los flujos de sesión, catálogo, favoritos, inscripciones y mapa existentes. Al finalizar, indica los archivos modificados y las comprobaciones que ejecutaste.



Al acabar eso sigue con lo siguiente:
Revisa y mejora la experiencia de usuario de toda Eventia en el proyecto Android existente, con Kotlin, Jetpack Compose y Material 3. Implementa cambios concretos en la interfaz y en sus interacciones. Usa como base el diseño aprobado: fondo blanco, acentos verde salvia, Crimson Text Regular, navegación inferior flotante e iconos activos rellenos.

Revisa primero el tema, el grafo de navegación, los componentes compartidos y los flujos reales de cada pantalla. Aprovecha los componentes y estados existentes. Mantén el alcance en el cliente Android y conserva el contrato de la API.

## 1. Aplica estos principios a decisiones concretas

1. Ley de Tesler. Deja que la app resuelva trabajo repetitivo, como restaurar filtros al volver de un detalle, formatear fechas y conservar los campos válidos después de un error de formulario.
2. Carga cognitiva. Define una acción principal por contexto. Reduce textos repetidos y deja visible la información necesaria para decidir qué hacer.
3. Ley de proximidad. Coloca juntos los elementos relacionados: fecha y hora, nombre y lugar, campo y mensaje de error, botón de asistencia y cupos.
4. Regla del pico y el final. Cuida el cierre de acciones importantes. Después de guardar un favorito o confirmar una inscripción, muestra el resultado real y deja claro qué puede hacer el usuario a continuación.
5. Principio de Pareto. Da acceso directo a las tareas más útiles para el flujo principal: buscar, filtrar, abrir un evento y guardarlo. Usa el mockup de Explorar como punto de partida para los accesos rápidos.
6. Ley de similitud. Usa iconos, etiquetas, tamaños y estados equivalentes para acciones equivalentes en toda la app. El corazón debe comunicar el mismo estado en Explorar, Mapa, Favoritos y detalle.
7. Divulgación progresiva. Muestra primero el resumen y deja el contenido adicional detrás de controles claros como «Filtros», «Ver más» o «Gestionar inscripción», según corresponda.
8. Efecto de estética y usabilidad. Cuida alineación, ritmo visual y consistencia. Comprueba también el contraste sobre fotos, el ajuste de textos largos y las áreas táctiles.
9. Efecto de posición serial. Coloca los accesos prioritarios al inicio o al final de grupos breves. Mantén «Todos» al inicio y «Filtros» al final de los accesos rápidos de Explorar.
10. Reconocer en vez de recordar. Acompaña los iconos de filtros con etiquetas visibles, identifica claramente el filtro activo y usa verbos concretos como «Cómo llegar» y «Ver detalle».
11. Ley de Parkinson. Reduce pasos y esperas evitables en tareas simples. Permite aplicar un filtro rápido con un toque, muestra el progreso de las operaciones asíncronas y deja las confirmaciones para acciones que realmente las necesitan.
12. Ley de región común. Agrupa controles relacionados dentro de una superficie compartida, como el selector «Eventos / Lugares» o «Próximas / Pasadas».
13. Efecto por defecto. En la primera entrada a Explorar, activa «Eventos» y «Todos». Respeta la selección del usuario cuando vuelva de otra pantalla. Las acciones que cambian datos deben partir de una decisión explícita.
14. Paradoja del usuario activo. Permite empezar a explorar de inmediato. Da ayuda breve en el momento en que se necesita, como al solicitar ubicación desde «Mi ubicación».
15. Conexión uniforme. Vincula visualmente cada acción con el elemento que modifica. En una lista con varios eventos, cada corazón y cada «Ver detalle» deben pertenecer claramente a su tarjeta.

## 2. Aplícalos en todas las pantallas

- Arranque. Conserva la vista ilustrada y la transición automática. Resuelve la sesión en paralelo y avanza cuando el destino esté listo.
- Inicio de sesión y registro. Mantén una acción principal clara. Coloca errores junto al campo correspondiente, conserva los datos válidos tras un fallo y deja visible «Continuar sin cuenta» en el login.
- Explorar. Conserva el título con avatar, el buscador, el selector y los filtros rápidos. Muestra el filtro activo y ofrece una forma directa de limpiarlo. Mantén tres tarjetas panorámicas completas cuando el espacio lo permita. Al volver del detalle, recupera la búsqueda, los filtros y la posición de la lista.
- Mapa. Conserva el buscador blanco y los filtros con iconos. Distingue el pin seleccionado con tamaño y aro blanco. En «Eventos en este punto», asocia «Cómo llegar» y «Ver detalle» al evento correcto. Conserva el acceso al catálogo del mapa si el usuario está lejos de los pines y permite explorar aunque deniegue la ubicación.
- Favoritos. Usa el mismo corazón que en el resto de Eventia. Cuando la lista esté vacía, explica brevemente cómo guardar un evento y ofrece un acceso directo a Explorar. Si quitar un favorito es reversible mediante el flujo existente, ofrece «Deshacer» cuando sea práctico.
- Detalle del evento. Agrupa categoría, nombre y fecha en el encabezado. Conserva la ubicación, el minimapa y «Cómo llegar» juntos. En la barra inferior, muestra «Asistiré» junto a los cupos. Actualiza el estado de asistencia tras recibir el resultado real de la operación.
- Detalle del lugar. Agrupa nombre, horario y contador de próximos eventos en el encabezado. Coloca dirección, mapa y «Cómo llegar» en la sección de ubicación. Mantén claramente separada la lista de próximos eventos.
- Perfil. Mantén compactos los datos de cuenta y diferencia «Cerrar sesión» de la gestión de inscripciones. Conserva «Próximas / Pasadas» con sus contadores y los tickets compactos. Coloca «Gestionar inscripción» dentro del ticket correspondiente.
- Estados compartidos. Usa una estructura consistente para carga, error, vacío y sin conexión. Cuando haya una acción útil, nómbrala según el contexto, por ejemplo «Reintentar», «Limpiar filtros» o «Explorar eventos».

## 3. Verifica el resultado

Compila la variante debug y prueba recorridos completos en el emulador: entrar como invitado, buscar y filtrar, abrir un evento y volver, guardar y quitar un favorito, seleccionar un pin, denegar ubicación y gestionar una inscripción. Comprueba una pantalla pequeña y una con texto ampliado. Revisa que la barra flotante no cubra acciones, que los estados se distingan sin depender solo del color y que los controles con icono tengan etiquetas accesibles.

Al finalizar, resume por pantalla qué cambiaste, qué tarea se simplificó y qué pruebas ejecutaste. Si algún principio ya estaba bien resuelto, describe el comportamiento existente que verificaste.

L2 — Located reference in Downloads. CodeGraph mapping completed. Engram mirror pending because no memory tool is exposed.

L3 — Baseline debug assembled with the existing Android Studio JBR and local Gradle cache. The reference stays external. Production catalog, Room, session and navigation provide the app data.
L4 — Pixel 6a manual run: guest entry, all four tabs, Profile via the avatar, back to Explore, music filter restoration after event details, favorite saved and favorite removed with Undo. The real catalog cards used the existing image fallback during this run.
L5 — Explore geometry, category interactions, and 320 dp/1.5x-font scrolling have dedicated instrumentation assertions. The first run exposed test matcher ambiguity, exact float equality, and a visible fourth-card sliver at the bottom. Scoped matchers, an epsilon, and rounding up the fitted height address these observations.

L6 — Final Gradle run succeeded: assembleDebug, testDebugUnitTest (22 passed), and connectedDebugAndroidTest on Pixel 6a (28 passed). The smaller catalog fixture uses a 320 dp width and a 1.5x font scale. The attendance test uses an isolated repository fixture. The theme's named text pairs range from 5.26:1 to 9.14:1.

L7 — Final on-device smoke run: three full real-photo cards, local event date at 27 nov · 20:00, query narrowed the catalog, the map stayed usable after location denial, and the two-event point opened its panel with per-event actions. The panel opened the requested event and the event opened Teatro Municipal with hours, directions and two upcoming events. Android 17 ran the debug build in 16 KB page-size compatibility mode for existing dependency libraries.
L8 — Authenticated attendance changes and refresh were exercised with a test repository instead of a live account.
L9 — S1 and S2 share the verified feature commit because navigation, theme, and component changes are integrated. The Engram mirror is pending.

### Changed files
- README.md
- app/src/androidTest/java/com/example/eventia/ExploreCatalogLayoutTest.kt
- app/src/androidTest/java/com/example/eventia/SessionFlowTest.kt
- app/src/main/assets/licenses/crimson_text_ofl.txt
- app/src/main/java/com/example/eventia/ui/components/EventCard.kt
- app/src/main/java/com/example/eventia/ui/components/FavoriteButton.kt
- app/src/main/java/com/example/eventia/ui/components/TabHeader.kt
- app/src/main/java/com/example/eventia/ui/navigation/EventiaNavigation.kt
- app/src/main/java/com/example/eventia/ui/screens/eventdetail/EventDetailScreen.kt
- app/src/main/java/com/example/eventia/ui/screens/eventdetail/EventDetailViewModel.kt
- app/src/main/java/com/example/eventia/ui/screens/explore/ExplorePresentation.kt
- app/src/main/java/com/example/eventia/ui/screens/explore/ExploreScreen.kt
- app/src/main/java/com/example/eventia/ui/screens/favorites/FavoritesScreen.kt
- app/src/main/java/com/example/eventia/ui/screens/favorites/FavoritesViewModel.kt
- app/src/main/java/com/example/eventia/ui/screens/map/MapEventSheet.kt
- app/src/main/java/com/example/eventia/ui/screens/map/MapScreen.kt
- app/src/main/java/com/example/eventia/ui/screens/profile/ProfileScreen.kt
- app/src/main/java/com/example/eventia/ui/screens/session/FestivalSessionForm.kt
- app/src/main/java/com/example/eventia/ui/screens/venue/VenueDetailScreen.kt
- app/src/main/java/com/example/eventia/ui/screens/venue/VenueListScreen.kt
- app/src/main/java/com/example/eventia/ui/theme/Theme.kt
- app/src/main/java/com/example/eventia/ui/theme/Type.kt
- app/src/main/res/drawable/ic_catalog_filters.xml
- app/src/main/res/drawable/ic_explore_filled.xml
- app/src/main/res/drawable/ic_map_filled.xml
- app/src/main/res/drawable/ic_profile_outline.xml
- app/src/main/res/font/crimson_text_regular.ttf
- app/src/main/res/values/strings.xml
