# RecordatoriosApp — Versión 3 (Persistencia Local, Preferencias y Sincronización)

Aplicación móvil desarrollada en **Kotlin** y **Jetpack Compose** bajo una arquitectura **MVVM + Repositorio Local-First**, que integra almacenamiento estructurado con **Room**, preferencias reactivas con **Preferences DataStore** y sincronización controlada con una API REST mediante **Retrofit**.

---

## 1. Evolución de la Aplicación

* **Versión 1 (Interfaz y estado efímero):** Primera iteración enfocada en la construcción de pantallas declarativas con Jetpack Compose y Material 3. Los recordatorios vivían únicamente en memoria dentro de listas mutables, perdiéndose al cerrar la aplicación.
* **Versión 2 (Arquitectura limpia y navegación):** Incorporación de `ViewModel` con `StateFlow`, separación entre pantallas (listado, detalle y creación) y flujo unidireccional de datos (UDF), manteniendo aún un origen de datos simulado en memoria.
* **Versión 3 (Persistencia local, preferencias y sincronización *offline-first*):** Evolución hacia una aplicación instalable y resiliente frente a cortes de red. Se reemplazó el almacenamiento volátil por una capa `data` desacoplada que combina **Room** (base de datos SQLite local), **Preferences DataStore** (configuración de usuario) y **Retrofit + OkHttp** (comunicación remota controlada), permitiendo operar sin conexión y sincronizar cambios pendientes de forma segura.

---

## 2. Diferencia entre Preferencias (DataStore) y Registros (Room)

La aplicación separa estrictamente la configuración ligera de la interfaz respecto de los datos de negocio estructurados:

| Criterio | Preferences DataStore (`PreferencesManager`) | Base de Datos Room (`ReminderDatabase`) |
| :--- | :--- | :--- |
| **Propósito** | Guardar ajustes personales y estado de visualización de la UI. | Almacenar el catálogo completo de recordatorios y su estado de sincronización. |
| **Modelo de datos** | Pares clave-valor tipados (`booleanPreferencesKey`, `stringPreferencesKey`) mapeados a `UserPreferences`. | Tablas relacionales SQLite representadas por `ReminderEntity` y consultadas vía `ReminderDao`. |
| **Datos almacenados** | `isDarkMode` (tema oscuro), `showCompleted` (filtro de completados) y `sortOrder` (criterio de orden). | `id`, `title`, `description`, `category`, `priority`, `isCompleted`, `createdAt`, `isPendingSync` e `isDeleted`. |
| **Reactividad** | Expone un `Flow<UserPreferences>` con manejo de `IOException` que actualiza el tema y los filtros en tiempo real. | Expone consultas reactivas `Flow<List<ReminderEntity>>` combinadas con las preferencias en el `ViewModel`. |

---

## 3. Arquitectura de la Capa de Datos

La capa `data` se organiza en paquetes independientes conectados mediante inyección manual centralizada en `RecordatoriosApplication`:

* **Almacenamiento Local (`data/local`):**
    * `ReminderEntity`: Entidad de Room que define la tabla de recordatorios y sus banderas de control (`isPendingSync`, `isDeleted`).
    * `ReminderDao`: Interfaz de acceso a datos con operaciones `suspend` (inserción, actualización, borrado lógico/físico) y consultas reactivas `Flow` que filtran automáticamente los registros marcados como eliminados (`isDeleted = 0`).
    * `ReminderDatabase`: Base de datos abstracta implementada como *Singleton* (`@Volatile` + `synchronized`) para evitar múltiples instancias concurrentes.
    * `PreferencesManager` y `UserPreferences`: Gestor de `Preferences DataStore` encargado de leer y persistir de forma asíncrona las preferencias del usuario.
* **Capa Remota (`data/remote`):**
    * `ReminderDto`: Objeto de transferencia de datos con anotaciones `@SerializedName` para serializar y deserializar JSON sin acoplar la API a la base de datos.
    * `ReminderApiService`: Interfaz Retrofit con operaciones CRUD (`GET`, `POST`, `PUT`, `DELETE`) que devuelven `Response<T>` para inspeccionar códigos de estado HTTP.
    * `RetrofitClient`: Instancia única configurada con `BuildConfig.BASE_URL`, `GsonConverterFactory` y un `OkHttpClient` con `HttpLoggingInterceptor` y *timeouts* de 15 segundos.
* **Mapeo y Coordinación (`data/mapper` y `data/repository`):**
    * `Reminder` (Dominio) y `ReminderMapper`: Funciones de extensión explícitas que convierten entre `ReminderEntity`, `ReminderDto` y el modelo limpio `Reminder` consumido por la UI.
    * `ReminderRepository`: Coordinador central que abstrae el origen de los datos al `RecordatorioViewModel`.

---

## 4. Enfoque Local-First

El sistema adopta una arquitectura **Local-First** donde **Room actúa como la única fuente de verdad (*Single Source of Truth*)**:

1. **Lectura reactiva:** La interfaz nunca renderiza directamente respuestas de Retrofit. El `RecordatorioViewModel` utiliza el operador `combine` para unir el `Flow` de recordatorios provenientes de Room con el `Flow` de `UserPreferences` de DataStore, aplicando filtros y ordenamientos localmente de manera instantánea.
2. **Escritura inmediata:** Toda creación, edición, cambio de estado o eliminación se persiste primero en SQLite mediante `ReminderDao`. El usuario percibe una respuesta inmediata sin latencia de red ni bloqueos en pantalla.

---

## 5. Tratamiento de Operaciones Pendientes y Manejo de Errores

Para garantizar el funcionamiento *offline* sin pérdida de datos ni cierres inesperados:

* **Marcado de cambios locales:** Al crear o modificar un recordatorio, se guarda en Room con `isPendingSync = true` (visualizado en la UI mediante el indicador `⚠️ Pendiente de sincronización`).
* **Borrado lógico (*Soft Delete*):** Al eliminar un recordatorio, si aún no fue confirmado por el servidor, se marca con `isDeleted = true` e `isPendingSync = true`. El `DAO` lo oculta inmediatamente de la lista visible (`WHERE isDeleted = 0`), pero conserva la intención de borrado para notificar al backend cuando vuelva la conectividad.
* **Aislamiento de fallos de red:** Todas las llamadas a `ReminderApiService` dentro de `ReminderRepository` están encapsuladas en bloques `try/catch` que absorben excepciones de conectividad (`IOException`) y respuestas HTTP no exitosas (`4xx`/`5xx`), evitando que errores de red escalen hacia el `ViewModel` o cierren la aplicación.
* **Sincronización controlada (`syncWithRemote()`):** Se ejecuta automáticamente al iniciar el `ViewModel` y de forma manual desde el botón **🔄** de la barra superior siguiendo una estrategia **Push-before-Pull**:
    1. **Push:** Consulta en Room los registros con `isPendingSync = true` o `isDeleted = true` y envía cada operación (`POST`/`PUT`/`DELETE`) al servidor; si la respuesta es exitosa (`response.isSuccessful`), limpia la bandera `isPendingSync = false` o elimina físicamente los registros borrados.
    2. **Pull:** Descarga el listado remoto actualizado (`GET`) y actualiza Room únicamente con los registros confirmados.

---

## 6. Estrategia para Evitar Duplicaciones

Para impedir registros duplicados tras múltiples sincronizaciones o reintentos sin conexión:

1. **Identificador único estable (`id`):** Cada recordatorio se genera con un identificador único estable (`UUID` / `String` primario) desde su creación local, manteniendo exactamente el mismo `id` en `ReminderEntity`, `Reminder` y `ReminderDto`.
2. **Inserción idempotente en Room:** Las operaciones de escritura en `ReminderDao` utilizan `@Insert(onConflict = OnConflictStrategy.REPLACE)`, garantizando que actualizar o re-sincronizar un mismo `id` sobrescriba la fila existente en lugar de insertar una copia.
3. **Protección durante el Pull:** Al recibir datos desde el servidor en `syncWithRemote()`, el repositorio verifica los identificadores locales que todavía tienen `isPendingSync = true` o `isDeleted = true` para no pisar cambios locales pendientes ni resucitar registros eliminados localmente antes de completar su subida.

---

## 7. Resultados de las Pruebas Realizadas

Se ejecutaron pruebas funcionales sobre emulador Android (API 37) verificando los escenarios críticos de la rúbrica:

* **Persistencia de preferencias (DataStore):** Se activó el modo oscuro, se modificó el criterio de ordenamiento y se alternó el filtro de completados desde el menú `⚙️`. Tras cerrar la aplicación por completo desde recientes y volver a abrirla, el tema oscuro y los filtros configurados se mantuvieron intactos.
* **Operación *Offline-First* (Modo Avión):** Con conectividad desactivada en el emulador, se crearon cuatro recordatorios, se editaron detalles y se marcó un registro para eliminar. Todas las tarjetas se reflejaron al instante con el indicador `⚠️` sin mensajes de error ni bloqueos.
* **Persistencia local tras reinicio sin red (Room):** Manteniendo el Modo Avión activo, se forzó el cierre de la app y se volvió a iniciar. Los registros creados y modificados permanecieron almacenados localmente con su marca `⚠️`, mientras que el recordatorio eliminado continuó oculto.
* **Sincronización y resiliencia HTTP (Retrofit + OkHttp):** Al reactivar la conexión y presionar el botón de sincronización `🔄`, `HttpLoggingInterceptor` registró en Logcat las peticiones salientes hacia `BASE_URL` y el repositorio absorbió de manera controlada las respuestas `404` del endpoint de prueba sin lanzar excepciones ni interrumpir la sesión del usuario.

---

## 8. Instrucciones de Configuración, Compilación y Ejecución

### Requisitos previos
* **Android Studio** (Ladybug / Meerkat o superior con soporte para AGP 9.4.1 y Kotlin 2.0.21).
* **JDK 17** configurado en Gradle.
* Un emulador Android o dispositivo físico con **Android 8.0 (API 26)** o superior.

### Pasos para configurar y ejecutar
1. **Descomprimir / Clonar el proyecto:** Abrí la carpeta raíz `RecordatoriosApp` desde Android Studio (*File > Open*).
2. **Configurar variables de entorno locales (sin secretos):**
    * En la raíz del proyecto se incluye el archivo plantilla **`local.properties.example`**.
    * Creá o editá el archivo **`local.properties`** en la raíz del proyecto (junto a `sdk.dir`) y agregá la URL base de la API terminada en `/`:
      ```properties
      BASE_URL=[https://jsonplaceholder.typicode.com/](https://jsonplaceholder.typicode.com/)
      ```
3. **Sincronizar Gradle:** Hacé clic en **Sync Now** (o *File > Sync Project with Gradle Files*) para descargar dependencias y generar `BuildConfig.BASE_URL` junto con el código de Room mediante KSP.
4. **Compilar y ejecutar:**
    * Presioná **Shift + F10** (o el botón **Run 'app'**) seleccionando un emulador activo para instalar y abrir la aplicación directamente.
    * *(Opcional)* Para generar el archivo instalable `.apk` de depuración, ejecutá **Build > Build Bundle(s) / APK(s) > Build APK(s)**; el instalador quedará disponible en `app/build/outputs/apk/debug/app-debug.apk`.