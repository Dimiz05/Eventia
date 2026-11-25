const fs = require('node:fs');
const path = require('node:path');
const main = 'app/src/main/java/com/example/eventia/';
const drawable = 'app/src/main/res/drawable/';
const test = 'app/src/androidTest/java/com/example/eventia/';
const ids = new Map();
const scan = read('.ua/intermediate/scan-result.json');
for (const file of scan.files) {
  const kind = file.fileCategory === 'config' ? 'config' : file.fileCategory === 'docs' ? 'document' : 'file';
  ids.set(file.path, kind + ':' + file.path);
}

const briefs = {
  'ExploreFlowTest.kt': ['Prueba en Android la búsqueda con espera, la restauración de filtros y la recarga del catálogo al recuperar la conexión.', 'pruebas exploración corrutinas'],
  'CatalogConnectivity.kt': ['Convierte las notificaciones de conectividad de Android en un flujo de estados y emite un evento al recuperar una red validada.', 'conectividad corrutinas Android', 'callbackFlow adapta NetworkCallback a Flow y awaitClose retira el callback al dejar de observarlo.'],
  'NetworkModule.kt': ['Enlaza los repositorios y la conectividad mediante Hilt. Proporciona una instancia de Retrofit con Gson para EventiaApi.', 'inyección Hilt Retrofit', '@Binds vincula interfaces con implementaciones. @Provides construye el cliente externo que Hilt no puede crear de forma automática.'],
  'Category.kt': ['Representa una categoría del catálogo por su identificador y nombre.', 'dominio categorías Kotlin'],
  'CatalogRepository.kt': ['Define el acceso a eventos y lugares y distingue los resultados remotos de la copia local con fecha de descarga.', 'catálogo contrato Kotlin', 'La interfaz sellada CatalogEvents permite distinguir de forma exhaustiva entre resultados remotos y guardados.'],
  'CategoryRepository.kt': ['Define la consulta de categorías y convierte las categorías devueltas por la API en modelos de dominio.', 'catálogo categorías Retrofit'],
  'ExploreViewModel.kt': ['Coordina filtros, carga y reintentos de Explorar. Actualiza la caché al reconectar y mantiene el contenido anterior cuando una actualización falla.', 'exploración catálogo caché', 'SavedStateHandle conserva los filtros, StateFlow publica el estado y una corrutina cancelable aplica la espera de búsqueda.'],
  'MapViewModel.kt': ['Comprueba la configuración del mapa y agrupa los eventos por coordenadas válidas. Mantiene el filtro de categoría y la selección de marcador.', 'mapa marcadores estado'],
  'SessionFlowTest.kt': ['Prueba los formularios, el modo invitado, la restauración y cierre de sesión y las acciones de asistencia desde el detalle.', 'pruebas sesión inscripciones'],
  'Account.kt': ['Representa la cuenta de dominio mediante el correo electrónico y un nombre opcional.', 'dominio cuenta Kotlin'],
  'SessionRepository.kt': ['Coordina el inicio de sesión, el registro, la consulta de cuenta y la asistencia a eventos entre la API y el almacén de sesión.', 'cuenta autenticación inscripciones', 'Las funciones suspendidas de orden superior reutilizan el manejo de autenticación entre operaciones de red.'],
  'EventDetailViewModel.kt': ['Carga el detalle de un evento y coordina sus inscripciones y cancelaciones. Distingue sesión vencida, ausencia de evento y conflictos de asistencia.', 'eventos inscripciones estado'],
  'SessionViewModel.kt': ['Controla la restauración de sesión, el formulario, el acceso como invitado y el retorno al evento pendiente después de autenticarse.', 'cuenta sesión estado', 'La interfaz sellada SessionUiState representa estados excluyentes y SavedStateHandle conserva el ID de retorno a un evento.'],
  'AGENTS.md': ['Describe la arquitectura esperada de Eventia y las convenciones de navegación, persistencia, mapas, vistas previas y pruebas.', 'documentación arquitectura convenciones'],
  'README.md': ['Explica los requisitos, la configuración del entorno y la API, los flujos de cuenta y catálogo y los comandos de compilación y pruebas.', 'documentación compilación configuración'],
  'gradle.properties': ['Configura los argumentos de la JVM de Gradle, la caché de configuración y el estilo de código de Kotlin.', 'configuración Gradle Kotlin'],
  'gradlew.bat': ['Inicia el Gradle Wrapper desde Windows, localiza Java y devuelve el código de salida del proceso.', 'Windows Gradle compilación'],
  'ic_back.xml': ['Dibuja la flecha para regresar en la navegación y en el detalle de un evento.', 'drawable navegación icono'],
  'ic_event_calendar.xml': ['Dibuja el calendario que identifica la fecha y hora en el detalle de un evento.', 'drawable eventos calendario'],
  'ic_event_placeholder.xml': ['Proporciona la ilustración de reemplazo cuando una imagen de evento o lugar falta o no se puede cargar.', 'drawable imágenes eventos'],
  'ic_event_share.xml': ['Dibuja el icono de la acción de compartir un evento desde su detalle.', 'drawable eventos compartir'],
  'ic_explore.xml': ['Dibuja el icono de exploración que la app reutiliza en las pestañas, los filtros y los mensajes de pantalla.', 'drawable exploración búsqueda'],
  'ic_favorite_outline.xml': ['Dibuja el marcador de contorno para un evento que aún no está en favoritos.', 'drawable favoritos icono'],
  'ic_favorites.xml': ['Dibuja el marcador activo de favoritos en la navegación y en el control de un evento guardado.', 'drawable favoritos navegación'],
  'ic_launcher_background.xml': ['Define la capa de fondo de los iconos adaptativos de la aplicación.', 'drawable lanzador Android'],
  'ic_launcher_foreground.xml': ['Define la capa de primer plano de los iconos adaptativos de la aplicación.', 'drawable lanzador Android'],
  'ic_map.xml': ['Dibuja el icono de mapa usado por la navegación y por la ubicación en el detalle del evento.', 'drawable mapas navegación'],
  'ic_profile.xml': ['Dibuja el icono de perfil para el destino de cuenta de la navegación principal.', 'drawable perfil navegación'],
  'ic_status_error.xml': ['Dibuja el icono que ScreenMessage usa para señalar un estado de error.', 'drawable errores estado'],
};

const classBriefs = {
  ExploreFlowTest: 'Ejecuta escenarios de exploración en el hilo principal con solicitudes de catálogo controladas por la prueba.',
  CatalogConnectivity: 'Expone un flujo de eventos de reconexión para consumidores del catálogo.',
  AndroidCatalogConnectivity: 'Observa la red activa con ConnectivityManager y traduce la conectividad validada a flujos reactivos.',
  NetworkModule: 'Declara los enlaces de Hilt y el proveedor de Retrofit que usan los repositorios.',
  Category: 'Modelo de dominio de una categoría con identificador y nombre.',
  CatalogRepository: 'Contrato suspendido para consultar eventos, lugares y resultados del catálogo.',
  CatalogEvents: 'Resultado que distingue datos remotos y datos guardados con categorías y momento de descarga.',
  CategoryRepository: 'Contrato para consultar categorías como modelos de dominio.',
  RemoteCategoryRepository: 'Implementación que consulta EventiaApi y proyecta cada categoría al dominio.',
  ExploreFilters: 'Agrupa el texto de búsqueda y la categoría seleccionada.',
  ExploreUiState: 'Agrupa filtros, contenido y señales de carga o error de Explorar.',
  ExploreViewModel: 'Coordina las solicitudes cancelables y mantiene los filtros de Explorar en SavedStateHandle.',
  MapUiState: 'Distingue un mapa disponible, uno sin configuración y uno sin servicios de Google disponibles.',
  MapPoint: 'Representa coordenadas y ofrece una fábrica que valida su rango y normaliza sus equivalencias.',
  MapViewModel: 'Carga eventos para el mapa y actualiza categorías, marcadores y selección.',
  SessionFlowTest: 'Comprueba los estados y efectos observables de la cuenta y la asistencia mediante dobles de los repositorios.',
  Account: 'Modelo de dominio de una cuenta con correo y nombre opcional.',
  SessionRepository: 'Contrato para autenticarse, recuperar la cuenta, cerrar sesión y cambiar la asistencia.',
  AccountField: 'Enumera los campos de cuenta que pueden presentar errores de validación.',
  AuthenticationRejected: 'Representa un rechazo de autenticación y conserva los campos indicados por el servidor.',
  EventSessionExpired: 'Señala que una acción vinculada a un evento necesita renovar la sesión.',
  RemoteSessionRepository: 'Integra la API con el almacén de sesión y traduce los rechazos de autenticación a errores del dominio.',
  EventDetailUiState: 'Modela la carga, el contenido, la ausencia y el error del detalle de evento.',
  AttendanceConflict: 'Enumera los posibles resultados al verificar una inscripción en conflicto.',
  EventDetailViewModel: 'Carga un evento y mantiene el estado de asistencia sincronizado tras una acción del usuario.',
  SessionUiState: 'Modela los estados de inicio, invitado, formulario y sesión activa.',
  SessionViewModel: 'Gestiona solicitudes de cuenta y las transiciones de sesión y retorno a eventos.',
};

const methodBriefs = {
  'ExploreFlowTest.kt': {
    filtersCoalesceCancelAndRestore: 'Verifica que las búsquedas sucesivas se agrupen, cancelen solicitudes obsoletas y recuperen los filtros guardados.',
    refreshKeepsPreviousResultOnFailureAndCanRetry: 'Confirma que un error de actualización conserve el resultado anterior y permita reintentar.',
    reconnectDownloadsFullCatalogAndKeepsTheLatestFilters: 'Comprueba que al reconectar se actualice el catálogo sin filtros y se respete la selección vigente.',
  },
  'CatalogConnectivity.kt': { 'AndroidCatalogConnectivity.reconnects': 'Emite una notificación al volver a estar en línea después del estado inicial.', },
  'NetworkModule.kt': {
    bindSessionRepository: 'Enlaza SessionRepository con RemoteSessionRepository mediante Hilt.',
    bindAttendanceRepository: 'Enlaza AttendanceRepository con RemoteAttendanceRepository mediante Hilt.',
    bindCategoryRepository: 'Enlaza CategoryRepository con RemoteCategoryRepository mediante Hilt.',
    bindCatalogRepository: 'Enlaza CatalogRepository con RemoteCatalogRepository mediante Hilt.',
    bindCatalogConnectivity: 'Proporciona AndroidCatalogConnectivity a consumidores de la interfaz CatalogConnectivity.',
    provideEventiaApi: [42, 48, 'Construye y comparte el cliente Retrofit con el convertidor Gson para EventiaApi.'],
  },
  'CatalogRepository.kt': {
    getEvents: 'Declara la consulta de eventos con búsqueda y categoría opcionales.',
    loadEvents: 'Declara la carga del catálogo y comunica si proviene de la red o de una copia local.',
    getEvent: 'Declara la carga de un evento por ID y permite indicar si se requiere sesión o se acepta una copia guardada.',
    getVenues: 'Declara la consulta del listado de lugares.',
    getVenue: 'Declara la consulta de un lugar por ID.',
    getVenueEvents: 'Declara la consulta de eventos asociados a un lugar.',
  },
  'CategoryRepository.kt': { 'RemoteCategoryRepository.getCategories': 'Consulta EventiaApi y convierte las categorías a identificadores y nombres del dominio.' },
  'ExploreViewModel.kt': {
    changeQuery: 'Recibe cambios del texto de búsqueda y aplica una espera antes de recargar.',
    selectCategory: 'Activa o desactiva el filtro de categoría seleccionado.',
    clearFilters: 'Restablece ambos filtros y recarga de inmediato.',
    refresh: 'Solicita una actualización del catálogo cuando no hay una carga activa.',
    updateFilters: 'Persiste cambios en los filtros y prepara el estado para cargar los resultados.',
    load: 'Cancela la solicitud anterior y conserva el contenido existente cuando la nueva carga falla.',
    fetchContent: 'Carga los resultados filtrados y las categorías y excluye eventos cuyo inicio ya pasó.',
    refreshCatalogCache: 'Solicita el catálogo completo para actualizar la copia local después de recuperar la conexión.',
  },
  'MapViewModel.kt': {
    'MapPoint.from': [40, 48, 'Valida ambas coordenadas y normaliza el antimeridiano y el cero con signo.'],
    retryMap: 'Revalida la disponibilidad de Maps e inicia otro intento de presentación.',
    retryEvents: 'Reintenta cargar eventos si el mapa está listo y no hay una carga activa.',
    selectCategory: 'Persiste la categoría escogida y vuelve a calcular los marcadores correspondientes.',
    selectPoint: 'Mantiene la selección solo cuando sus coordenadas corresponden a un marcador.',
    loadEvents: 'Consulta el catálogo y actualiza las categorías, puntos y estado de carga del mapa.',
    markersFor: 'Agrupa los eventos con coordenadas válidas y respeta la categoría activa.',
    checkAvailability: 'Comprueba la configuración de Maps y la disponibilidad de Google Play Services.',
  },
  'SessionFlowTest.kt': {
    invalidEmailAndShortPasswordShowSupportingTextInTheirFields: 'Comprueba que el formulario muestre los errores de correo y contraseña junto al campo correspondiente.',
    figureTwoFieldsRejectBlankEmailAndShortPasswordLocally: 'Verifica que un correo vacío y una contraseña corta se rechacen antes de consultar la red.',
    cleanStartAllowsGuestEntryAndReturnToSignIn: 'Comprueba que sin sesión se pueda entrar como invitado y volver al formulario.',
    cancellingEventSignInReturnsAsGuestAndKeepsTheEventUntilNavigationConsumesIt: 'Verifica que cancelar el ingreso devuelva al modo invitado y conserve el evento pendiente hasta que se consuma.',
    expiredAttendanceClearsTheTokenAndOpensSignInWithTheReturnEvent: 'Comprueba que una sesión vencida al inscribirse limpie la sesión y conserve el evento de retorno.',
    expiredProfileSessionClearsTheTokenWithoutAnEventToReturnTo: 'Comprueba que expirar la sesión desde el perfil vuelva al ingreso sin asociar un evento.',
    attendanceUpdatesDetailAndRequestsProfileRefreshAfterEachChange: 'Verifica que la inscripción y su cancelación actualicen el detalle y soliciten refrescar el perfil.',
    conflictUsesUpdatedAttendanceBeforeCapacityAndDoesNotRetryThePost: 'Verifica que un conflicto se resuelva con datos recientes de asistencia y sin repetir la escritura.',
    offlineValidationKeepsSessionUntilLogout: 'Verifica que un fallo de red al consultar la cuenta conserve la sesión hasta el cierre.',
    rejectedRestoredSessionReturnsToLoginWithGuestAvailable: 'Comprueba que una sesión guardada rechazada devuelva al formulario y permita el modo invitado.',
  },
  'SessionRepository.kt': {
    'SessionRepository.loadAccount': 'Declara la carga de la cuenta asociada a la sesión local.',
    'RemoteSessionRepository.signIn': 'Envía el formulario de acceso y guarda el token devuelto por la API.',
    'RemoteSessionRepository.register': 'Envía los datos de registro y guarda el token devuelto por la API.',
    'RemoteSessionRepository.loadAccount': 'Carga el perfil autenticado y quita el token cuando la API rechaza la sesión.',
    updateAttendance: 'Aplica una mutación de asistencia con el token local y distingue el vencimiento de sesión.',
    authenticate: 'Valida y guarda el token devuelto y convierte rechazos de autenticación en errores de campo.',
    validationFields: 'Extrae del cuerpo de error los nombres de campos reconocidos por el formulario.',
  },
  'EventDetailViewModel.kt': {
    load: 'Cancela una solicitud previa y abre una carga del evento en el estado de sesión actual.',
    cancelRequest: 'Cancela el trabajo activo del detalle.',
    refresh: 'Revalida los datos de asistencia después de una respuesta que no pudo confirmarlos.',
    dismissConflict: 'Quita del estado actual el mensaje de conflicto.',
    attend: 'Solicita inscribir o cancelar y vuelve a consultar el evento para confirmar el resultado.',
    loadEvent: 'Carga el detalle y decide si mostrar contenido, ausencia, error o una solicitud de autenticación.',
  },
  'SessionViewModel.kt': {
    retryStartup: 'Reintenta restaurar la sesión desde un estado de error inicial.',
    restore: 'Busca una sesión persistida y carga la cuenta o abre el formulario de acceso.',
    continueAsGuest: 'Cambia al estado invitado cuando el formulario está disponible.',
    openSignIn: 'Ofrece el formulario de acceso desde el modo invitado.',
    requestEventSignIn: 'Conserva el evento pendiente antes de abrir el formulario.',
    expireEventSession: 'Expira la sesión manteniendo el ID del evento para el retorno.',
    expireSession: 'Cancela las solicitudes pendientes, limpia la sesión y presenta el formulario con el aviso correspondiente.',
    eventReturnHandled: 'Limpia el evento pendiente tras consumir el retorno correcto.',
    signOut: 'Cancela operaciones anteriores y procesa el cierre de sesión y sus posibles errores.',
    switchForm: 'Cambia entre acceso y registro mientras el formulario está disponible.',
    clearError: 'Retira el error del campo editado y del formulario.',
    submit: 'Valida el formulario y envía la autenticación, mostrando errores del servidor en el campo correspondiente.',
    retryAccount: 'Solicita la recarga de cuenta si no hay una operación activa.',
    loadAccount: 'Carga el perfil y conserva el estado adecuado durante el retorno a un evento o un fallo de red.',
  },
  'gradlew.bat': {
    findJavaFromJavaHome: [56, 68, 'Comprueba el ejecutable Java de JAVA_HOME y deriva el fallo al tratamiento de errores.'],
    execute: [70, 77, 'Invoca el JAR del Gradle Wrapper y le pasa las opciones de la JVM y los argumentos del usuario.'],
    fail: [83, 89, 'Traduce el resultado de Gradle a un código de salida utilizable desde Windows.'],
  },
};

const unexported = new Set(['updateFilters','fetchContent','refreshCatalogCache','loadEvents','markersFor','checkAvailability','updateAttendance','authenticate','validationFields','loadEvent','restore','loadAccount','findJavaFromJavaHome','execute','fail']);

function read(file) { return JSON.parse(fs.readFileSync(file, 'utf8')); }
function edge(source, target, type, weight) { return { source, target, type, direction: 'forward', weight }; }
function node(type, id, name, filePath, summary, tags, complexity, lineRange, languageNotes) {
  return { id, type, name, filePath, summary, tags, complexity, ...(lineRange && {lineRange}), ...(languageNotes && {languageNotes}) };
}
function complexity(lines) { return lines >= 140 ? 'complex' : lines >= 70 ? 'moderate' : 'simple'; }

const batchInput = read('.ua/intermediate/batches.json').batches;
for (const i of [4,5,6,7]) {
  const batch = batchInput.find(item => item.batchIndex === i);
  const data = read('.ua/tmp/ua-file-extract-results-' + i + '.json');
  if (!data.scriptCompleted || data.filesUnreadable?.length) throw Error('Extraction incomplete: ' + i);
  const results = new Map(data.results.map(item => [item.path,item]));
  const nodes = [], edges = [], used = new Set();
  const add = (source,target,type,weight) => {
    if (!source || !target) throw Error('Missing endpoint for ' + type);
    const key = [source,target,type].join('|');
    if (!used.has(key)) {
      edges.push(edge(source,target,type,weight));
      used.add(key);
    }
  };
  for (const file of batch.files) {
    const f = file.path, basename = path.posix.basename(f), fileId = ids.get(f);
    const base = briefs[basename], result = results.get(f);
    if (!base || (!result && !data.filesSkipped.includes(f))) throw Error('Missing file ' + f);
    if (!result && !fs.existsSync(f)) throw Error('Unreadable skipped file ' + f);
    const tags = base[1].split(' ');
    const tag = f.startsWith(test) ? 'prueba' : 'Kotlin';
    nodes.push(node(fileId.split(':')[0],fileId,basename,f,base[0],tags,complexity(file.sizeLines),undefined,base[2]));
    if (file.fileCategory === 'code') for (const dep of batch.batchImportData[f] || []) add(fileId,'file:'+dep,'imports',0.7);

    const classes = result?.classes || [];
    const classesByName = new Map();
    for (const c of classes) {
      if (!classBriefs[c.name]) continue;
      const id = 'class:' + f + ':' + c.name;
      classesByName.set(c.name,id);
      nodes.push(node('class',id,c.name,f,classBriefs[c.name],tags,complexity(c.endLine-c.startLine+1),[c.startLine,c.endLine]));
      add(fileId,id,'contains',1);
      add(fileId,id,'exports',0.8);
    }

    const emitted = new Map();
    for (const [name,value] of Object.entries(methodBriefs[basename] || {})) {
      const symbols = name.split('.');
      const simpleName = symbols.at(-1);
      const owner = symbols.length === 2 ? classes.find(c => c.name === symbols[0]) : undefined;
      const definition = result?.functions?.find(method => method.name === simpleName && (!owner || (method.startLine >= owner.startLine && method.endLine <= owner.endLine)));
      if (!Array.isArray(value) && !definition) throw Error('Missing function evidence: ' + f + ' ' + name);
      const [start,end] = Array.isArray(value) ? value : [definition.startLine,definition.endLine];
      const summary = Array.isArray(value) ? value[2] : value;
      const id = 'function:' + f + ':' + name;
      const declaring = owner || classes.find(c => start >= c.startLine && end <= c.endLine);
      emitted.set(simpleName,id);
      nodes.push(node('function',id,simpleName,f,summary,[tag,tags[0]],complexity(end-start+1),[start,end]));
      add(fileId,id,'contains',1);
      if (declaring && classesByName.has(declaring.name)) add(classesByName.get(declaring.name),id,'contains',1);
      if (!unexported.has(name) && (name !== 'load' || basename === 'EventDetailViewModel.kt')) add(fileId,id,'exports',0.8);
    }
    for (const call of result?.callGraph || []) {
      if (call.caller !== call.callee && emitted.has(call.caller) && emitted.has(call.callee)) {
        add(emitted.get(call.caller),emitted.get(call.callee),'calls',0.8);
      }
    }
  }

  const implemented = (file,name,base) => add('class:'+main+file+':'+name,'class:'+main+file+':'+base,'implements',0.9);
  const fileEdge = (from,to,type,weight) => add(ids.get(from),ids.get(to),type,weight);
  if (i===4) {
    implemented('data/connectivity/CatalogConnectivity.kt','AndroidCatalogConnectivity','CatalogConnectivity');
    implemented('repositories/CategoryRepository.kt','RemoteCategoryRepository','CategoryRepository');
    fileEdge(main+'ui/screens/explore/ExploreViewModel.kt',test+'ExploreFlowTest.kt','tested_by',0.5);
    fileEdge(main+'di/NetworkModule.kt',main+'api/EventiaApi.kt','configures',0.6);
    add('function:'+main+'ui/screens/map/MapViewModel.kt:loadEvents','function:'+main+'repositories/CatalogRepository.kt:getEvents','calls',0.8);
    for (const name of ['fetchContent','refreshCatalogCache']) add('function:'+main+'ui/screens/explore/ExploreViewModel.kt:'+name,'function:'+main+'repositories/CatalogRepository.kt:loadEvents','calls',0.8);
  }
  if (i===5) {
    implemented('repositories/SessionRepository.kt','RemoteSessionRepository','SessionRepository');
    for (const target of ['ui/screens/eventdetail/EventDetailViewModel.kt','ui/screens/session/SessionViewModel.kt']) fileEdge(main+target,test+'SessionFlowTest.kt','tested_by',0.5);
    fileEdge(main+'repositories/SessionRepository.kt',main+'data/local/SessionStore.kt','reads_from',0.6);
    fileEdge(main+'repositories/SessionRepository.kt',main+'data/local/SessionStore.kt','writes_to',0.6);
    add('function:'+main+'ui/screens/eventdetail/EventDetailViewModel.kt:loadEvent','function:'+main+'repositories/CatalogRepository.kt:getEvent','calls',0.8);
    add('function:'+main+'ui/screens/session/SessionViewModel.kt:loadAccount','function:'+main+'repositories/SessionRepository.kt:SessionRepository.loadAccount','calls',0.8);
  }
  if (i===6) {
    fileEdge('AGENTS.md', main+'MainActivity.kt','documents',0.5);
    fileEdge('README.md', main+'ui/navigation/EventiaNavigation.kt','documents',0.5);
    fileEdge('README.md', main+'repositories/RemoteCatalogRepository.kt','documents',0.5);
    fileEdge('README.md', 'app/build.gradle.kts','documents',0.5);
    fileEdge('gradle.properties','app/build.gradle.kts','configures',0.6);
    fileEdge('gradlew.bat','gradle/wrapper/gradle-wrapper.properties','depends_on',0.6);
  }
  if (i===7) {
    for (const [resource,consumers] of Object.entries({
      ic_back: ['ui/navigation/EventiaNavigation.kt','ui/screens/eventdetail/EventDetailScreen.kt'],
      ic_event_calendar: ['ui/screens/eventdetail/EventDetailScreen.kt'],
      ic_event_placeholder: ['ui/components/EventCard.kt'],
      ic_event_share: ['ui/screens/eventdetail/EventDetailScreen.kt'],
      ic_explore: ['ui/navigation/EventiaNavigation.kt','ui/components/ScreenMessage.kt','ui/screens/explore/ExploreScreen.kt','ui/screens/venue/VenueListScreen.kt'],
      ic_favorite_outline: ['ui/components/FavoriteButton.kt'],
      ic_favorites: ['ui/navigation/EventiaNavigation.kt','ui/components/FavoriteButton.kt'],
      ic_map: ['ui/navigation/EventiaNavigation.kt','ui/screens/eventdetail/EventDetailScreen.kt'],
      ic_profile: ['ui/navigation/EventiaNavigation.kt'],
      ic_status_error: ['ui/components/ScreenMessage.kt'],
    })) for (const consumer of consumers) fileEdge(main+consumer,drawable+resource+'.xml','depends_on',0.6);
    for (const launcher of ['ic_launcher','ic_launcher_round']) for (const layer of ['background','foreground']) {
      fileEdge('app/src/main/res/mipmap-anydpi/'+launcher+'.xml',drawable+'ic_launcher_'+layer+'.xml','depends_on',0.6);
    }
  }
  const inventory = nodes.filter(n => ids.get(n.filePath) === n.id);
  if (inventory.length!==batch.files.length || !batch.files.every(f => inventory.some(n=>n.filePath===f.path))) throw Error('Inventory mismatch for batch '+i);
  if (new Set(nodes.map(n=>n.id)).size!==nodes.length) throw Error('Duplicate IDs for batch '+i);
  const expectedImports = batch.files.filter(f=>f.fileCategory==='code').reduce((acc,f)=>acc+(batch.batchImportData[f.path]?.length || 0),0);
  const actualImports = edges.filter(e=>e.type==='imports').length;
  if (expectedImports!==actualImports) throw Error('Import count mismatch for batch '+i);
  fs.writeFileSync('.ua/intermediate/batch-'+i+'.json',JSON.stringify({nodes,edges},null,2)+'\n');
  console.log('Batch '+i+': '+inventory.length+' files; '+nodes.length+' nodes; '+edges.length+' edges; '+actualImports+' imports');
}
