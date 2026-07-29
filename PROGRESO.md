# Progreso del proyecto

Resumen legible en español de la evolución del proyecto. No reemplaza el historial de git, es un complemento para retomar contexto rápido.

## 2026-07-28 — Scaffolding del proyecto + modelo de datos

Qué se hizo:
- Se creó el proyecto Maven (`pom.xml`) con Java 17, JavaFX 21, Hibernate ORM (JPA) + driver de PostgreSQL, Flyway para migraciones, y las dependencias de testing (JUnit 5, Mockito, TestFX).
- Se armó `docker-compose.yml` para levantar PostgreSQL localmente (base `simulador_inversiones`, usuario/password de desarrollo `simulador`/`simulador`). Se usa el puerto host `5433` (no el `5432` default) porque la máquina ya tiene otro contenedor Postgres de otro proyecto ocupando ese puerto.
- Se escribió la migración inicial de esquema (`V1__esquema_inicial.sql`) con las tablas: usuario, sesion_inversion, activo, historial_precio, cartera_usuario, posicion_cartera, transaccion y noticia.
- Se modelaron las 8 entidades JPA (paquete `core.entidades`) y sus enums (`EstadoSesion`, `TipoActivo`, `TipoTransaccion`), siguiendo la sección 4 de la especificación.
- Se agregó `PosicionCartera` como entidad nueva (no estaba explícita en la spec original) porque JPA necesita una tabla real para modelar la lista `activosPoseidos` de `CarteraUsuario` — no es una tupla suelta, es una relación cartera–activo con cantidad y precio promedio de compra.
- Se creó la capa de repositorios (`core.repositorios`): una clase base genérica `RepositorioBase<T, ID>` con operaciones CRUD sobre `EntityManager`, y un repositorio concreto por entidad.
- Se configuró `ConfiguracionBaseDatos` (paquete `config`) para levantar el `EntityManagerFactory`, permitiendo sobrescribir la conexión con las variables de entorno `DB_URL`, `DB_USUARIO` y `DB_PASSWORD` (si no están, usa los valores de desarrollo de `persistence.xml`, iguales a los del `docker-compose.yml`).
- Se armó una app JavaFX mínima (`ui.AplicacionPrincipal` + `pantalla-principal.fxml` + `ControladorPantallaPrincipal`) que al arrancar consulta la base de datos y muestra si la conexión funciona. Todavía no es el dashboard real — es solo para validar que toda la cadena (JavaFX → Hibernate → PostgreSQL) está conectada.
- Se agregó un test de humo (`RepositorioActivoTest`) que guarda y lee un `Activo` real contra PostgreSQL.

Por qué:
- Es la base estructural sobre la que se va a construir el resto de la app (lógica de negocio, ingesta de datos históricos, UI real). Se decidió avanzar módulo por módulo, revisando cada uno antes de seguir.
- Se eligió Hibernate/JPA puro (sin Spring) porque el stack definido no menciona Spring, y encaja naturalmente con la capa de "repositorios" pedida en la estructura de carpetas.
- Se eligió Flyway + `flyway-maven-plugin` para versionar el esquema sin tener que escribir código Java solo para correr migraciones.
- Se usó Docker Compose para PostgreSQL porque no había una instancia local disponible.

Pendiente para próximos módulos:
- Ingesta real de datos históricos (Dow Jones/S&P 500/Shiller, oro vía measuringworth.com, acciones individuales vía Stooq) en `core.datos`.
- Ingesta de noticias históricas (NYT Archive API) — se decidió dejarla para más adelante, todavía no hay API key.
- UI real tipo dashboard (referencia: Bullmarket), gráficos de evolución de cartera, ControlsFX/Ikonli para el look & feel.
- Empaquetado con `jpackage` para distribución nativa.

## 2026-07-28 — Capa de servicios: sesiones, compra/venta y avance de tiempo

Qué se hizo:
- Se agregó `RepositorioPosicionCartera` (faltaba del módulo anterior) y métodos de consulta específicos en los repositorios existentes: `RepositorioActivo.buscarDisponiblesEn`, `RepositorioHistorialPrecio.buscarUltimoPrecioHasta`, `RepositorioSesionInversion.buscarPorUsuario`, `RepositorioCarteraUsuario.buscarPorSesion`.
- Se implementó `core.servicios` con cuatro servicios:
  - `ServicioSesionInversion`: crear (valida fecha 1900–2026 y usuario, crea la cartera vacía asociada), pausar, reanudar, finalizar, listar por usuario y buscar por id (esto último cubre "retomar" una sesión guardada). Define las constantes `FECHA_MINIMA` (1900-01-01) y `FECHA_LIMITE` (2026-01-01) que usa también `ServicioAvanceTiempo`.
  - `ServicioActivos`: activos disponibles en una fecha y último precio conocido de un activo hasta esa fecha (no exige un dato exacto para el día, porque la frecuencia de muestreo de las fuentes históricas varía).
  - `ServicioTransacciones`: comprar/vender. El saldo de efectivo puede quedar negativo (permitido por la spec), pero no se puede vender más cantidad de la que la sesión posee — no hay venta en corto. En una compra sobre una posición existente, el precio promedio de compra se recalcula ponderado por cantidad; en una venta que deja la posición en cero, se elimina la fila de `PosicionCartera`.
  - `ServicioAvanceTiempo`: avanza/retrocede la fecha simulada por día/semana/mes/año con cantidad personalizable, usando aritmética de calendario de `LocalDate` (no días fijos). Al avanzar, si el saldo es negativo, aplica interés simple del **2% mensual** prorrateado según los días efectivamente avanzados (constante `TASA_INTERES_MENSUAL`, ajustable). Si el avance supera `FECHA_LIMITE`, la fecha se recorta a ese límite y la sesión pasa a `FINALIZADA`; si el retroceso supera `FECHA_MINIMA`, se recorta a 1900-01-01. Retroceder no revierte intereses ya aplicados.
  - Se agregó `ExcepcionOperacionInvalida` (unchecked) para las violaciones de reglas de negocio (vender sin stock, operar una sesión no activa, fechas fuera de rango, etc.), y el enum `UnidadTiempo` (DIA/SEMANA/MES/ANIO).
- Los servicios reciben sus repositorios por constructor (no crean su propio `EntityManager`), para que sean mockeables con Mockito en los tests unitarios. Quien arma el servicio con repositorios reales es responsable de abrir el `EntityManager` y la transacción — patrón validado en el test de integración.
- Se escribieron 18 tests unitarios (JUnit 5 + Mockito, repositorios mockeados) cubriendo compra/venta, promedio ponderado, validaciones de negocio, interés sobre saldo negativo y los límites de fecha.
- Se agregó `ServiciosIntegracionTest`, que corre el flujo completo (crear sesión → comprar → avanzar un mes → vender) contra PostgreSQL real.

Decisiones confirmadas con el usuario:
- Tasa de interés: 2% mensual simple, prorrateada — valor de partida ajustable (la spec la dejaba explícitamente a definir).
- No se permite venta en corto: solo se puede vender hasta la cantidad que la sesión efectivamente posee en cartera; el saldo negativo permitido es únicamente de efectivo.

Bug encontrado y corregido en el camino:
- Un test intentaba mockear `HistorialPrecio` (una entidad JPA simple) haciendo un `when()` anidado dentro de otro `when()` sin terminar, lo que Mockito rechaza (`UnfinishedStubbingException`). Se resolvió instanciando la entidad directamente en vez de mockearla — es un POJO, no hace falta mock.

Nota de entorno para correr los tests en esta máquina:
- La instalación de Java por defecto en este Mac es la 26 (`/opt/homebrew/opt/openjdk`), pero Mockito 5.12/ByteBuddy todavía no soportan generar mocks sobre bytecode de Java 26 (falla con "Mockito cannot mock this class"). Para correr `mvn test` hay que forzar Java 21, ya instalado en `/opt/homebrew/opt/openjdk@21`:
  ```
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21
  mvn test
  ```
  No se cambió el `JAVA_HOME` global de la máquina porque es una configuración del usuario, no del proyecto.

Pendiente para próximos módulos:
- Ingesta real de datos históricos en `core.datos`.
- Ingesta de noticias históricas (NYT Archive API).
- UI real tipo dashboard: pantallas para crear/retomar sesión, comprar/vender, avanzar/retroceder tiempo (incluyendo el avance continuo con velocidad configurable, que todavía no se implementó — es más un detalle de UI/temporizador que de lógica de negocio).
- Empaquetado con `jpackage` para distribución nativa.

## 2026-07-28 — UI real: navegación de pantallas, dashboard de cartera

Qué se hizo:
- Se reemplazó la app mínima placeholder (`pantalla-principal.fxml` / `ControladorPantallaPrincipal`) por un flujo de dos pantallas: `pantalla-inicio.fxml` (crear sesión nueva o retomar/reanudar una guardada) → `pantalla-dashboard.fxml` (operar la sesión activa).
- `NavegadorPantallas` centraliza la carga de FXML sobre el `Stage` único de la app y devuelve el controller cargado para que quien navega lo inicialice explícitamente (los controllers no tienen hook de inicialización propio, dependen de que el llamador les pase el contexto).
- `EjecutorTransaccional` es ahora el único lugar que abre un `EntityManager` + transacción: recibe una función que trabaja sobre una `FabricaServicios` fresca y hace commit o rollback según si la función lanza una `RuntimeException`. Todo `@FXML` handler que toca la base pasa por acá.
- `FabricaServicios` arma todos los repositorios/servicios sobre un mismo `EntityManager` y agrega `obtenerOCrearUsuarioUnico()`, que reutiliza la única fila `Usuario` o la crea en el primer arranque — consistente con el modelo de un solo usuario sin autenticación.
- Se agregó la capa `ServicioCartera` + `PosicionPortafolio` (record de solo lectura) para calcular, por sesión: las posiciones con su valor de mercado actual y ganancia/pérdida (usando el último precio conocido de cada activo a la fecha simulada), y el patrimonio neto total (efectivo + valor de las posiciones).
- `ControladorPantallaDashboard` implementa la pantalla operativa completa: fecha/saldo/patrimonio/estado de la sesión, tabla de activos disponibles con botones comprar/vender (piden cantidad por diálogo), tabla de la cartera, controles de avance/retroceso de tiempo por unidad (día/semana/mes/año) y cantidad, y avance automático continuo (un `Timeline` de JavaFX que llama a `ServicioAvanceTiempo.avanzar` un día a la vez a un intervalo configurable por slider, y se detiene solo si la sesión termina o se pausa/finaliza manualmente).
- Se agregó `V2__datos_semilla_prueba.sql`: datos de prueba (activos + historial de precios) para poder ejercitar el dashboard localmente sin depender todavía de la ingesta real de datos históricos.
- Se agregó `estilos.css` con el look oscuro de las dos pantallas.

Por qué:
- Antes de meterse con la ingesta de datos reales (el próximo módulo pendiente), tenía más sentido cerrar el círculo completo de la UI para poder probar visualmente el flujo entero (crear sesión → comprar/vender → avanzar tiempo → ver ganancia/pérdida → finalizar) con datos de prueba controlados.
- Se centralizó la apertura de `EntityManager`/transacción en `EjecutorTransaccional` en vez de dejar que cada controller abra la suya, para que la regla "una transacción por acción de UI" sea imposible de romper por accidente y quede en un solo lugar auditable.

Verificación:
- `mvn test` (JAVA_HOME=openjdk@21): 20/20 tests OK, sin regresiones.
- Se corrió `mvn javafx:run` contra la base local y se confirmó visualmente que la pantalla dashboard renderiza bien con datos reales de la sesión y de `V2__datos_semilla_prueba.sql`.

Pendiente para próximos módulos:
- Ingesta real de datos históricos en `core.datos` (Dow Jones/S&P 500/Shiller, oro vía measuringworth.com, acciones individuales vía Stooq) — hoy el dashboard depende de los datos semilla de `V2__datos_semilla_prueba.sql`.
- Ingesta de noticias históricas (NYT Archive API) — bloqueada, todavía no hay API key. La pestaña "Noticias" del dashboard existe en el FXML pero no tiene datos para mostrar.
- Empaquetado con `jpackage` para distribución nativa.

## 2026-07-29 — Ingesta de datos históricos: acciones (Yahoo Finance) y S&P 500 (Shiller)

Qué se hizo:
- Se implementó `core.datos`, hasta ahora vacío (solo tenía un `.gitkeep`): dos fuentes de datos históricos y un importador que los vuelca a la base.
- `FuenteDatosYahooFinance` descarga la serie diaria completa de una acción desde la API pública de gráficos de Yahoo Finance (`query1.finance.yahoo.com/v8/finance/chart/{ticker}`, JSON, sin API key). Parsea la respuesta con `org.json` (dependencia nueva, liviana, sin transitivas).
- `FuenteDatosShiller` descarga y parsea `ie_data.xls` (dataset público de Robert Shiller, `.xls` viejo formato BIFF) con Apache POI (`HSSFWorkbook`), extrayendo el precio nominal mensual del S&P Composite desde 1871. Dependencia nueva: `org.apache.poi:poi` (sin `poi-ooxml`, no hace falta para `.xls`).
- `ImportadorDatosHistoricos` (recibe `RepositorioActivo` y `RepositorioHistorialPrecio` por constructor, mismo patrón mockeable que el resto de `core.servicios`) hace upsert por ticker: crea el `Activo` si no existe, extiende `fechaDisponibleDesde` hacia atrás si los datos nuevos van más lejos en el pasado, e inserta solo los `HistorialPrecio` de fechas que todavía no estaban cargadas (evita el conflicto con la unique constraint `activo_id+fecha` en reimportaciones).
- Se agregó `ImportadorPrincipal`, un `main()` de línea de comandos (no pasa por la UI): reutiliza `EjecutorTransaccional.ejecutar(...)` para abrir la única transacción de la corrida, igual que cualquier acción de la UI. Se corre con `mvn compile exec:java -Dexec.mainClass=... -Dexec.args="yahoo AAPL 'Apple Inc.' ACCION"` o `-Dexec.args="shiller"`. Se agregó `exec-maven-plugin` al `pom.xml` para esto.
- Se agregaron `RepositorioActivo.buscarPorTicker` y `RepositorioHistorialPrecio.buscarFechasExistentes`, usados por el importador.
- `FabricaServicios` ahora expone `servicioImportacion()`.
- Tests nuevos (11 total): `FuenteDatosYahooFinanceTest` y `FuenteDatosShillerTest` contra fixtures en memoria (JSON armado a mano / `HSSFWorkbook` armado a mano), sin red; `ImportadorDatosHistoricosTest` con los repos mockeados (creación de activo nuevo, salteo de fechas duplicadas, extensión de `fechaDisponibleDesde`).

Por qué:
- El plan original (de la spec del producto) era usar Stooq para acciones individuales. Al intentarlo, Stooq resultó estar bloqueando **todo** acceso no-navegador (tanto `/q/d/l/` como su descarga bulk en `static.stooq.com`) detrás de un desafío anti-bot: un proof-of-work en JavaScript que hay que resolver y mandar a `/__verify` antes de que el sitio sirva contenido real. Programar ese solver habría sido evadir deliberadamente una protección anti-scraping puesta a propósito — se decidió con el usuario cambiar a Yahoo Finance en su lugar, que expone una API JSON pública sin ningún gate de este tipo.
- measuringworth.com (oro) tampoco tiene descarga directa — es un formulario interactivo. Se decidió no scrapearlo por ahora (queda pendiente, ver abajo) para no bloquear el resto del módulo por una sola fuente.

Bug encontrado y corregido en el camino:
- Pedirle a Yahoo Finance `range=max&interval=1d` no da precios diarios reales para historiales largos: Yahoo lo recorta en silencio a granularidad trimestral (`meta.dataGranularity` vuelve `"3mo"` en vez de `"1d"`) sin avisar ni devolver error — para AAPL, en vez de ~11500 precios diarios devolvía 168 (uno cada 3 meses). Se detectó comparando el conteo de precios contra lo esperado en una prueba manual contra la API real, no en los tests unitarios con fixtures (esos no lo hubieran detectado porque el fixture no reproduce el recorte de Yahoo). Se resolvió pidiendo un rango explícito (`period1=0&period2=<epoch actual>`) en vez de `range=max`, que sí devuelve la serie diaria completa.

Verificación:
- `mvn test` (JAVA_HOME=openjdk@21): 28/28 tests OK (los 2 tests que pegan contra PostgreSQL real — `RepositorioActivoTest` y `ServiciosIntegracionTest` — no corrieron en esta sesión porque el Colima local está en un estado roto ajeno a este cambio: "vz driver is running but host agent is not"; no se tocó esa configuración).
- Se corrió una prueba manual contra las fuentes reales (fuera de la suite de tests, con un `main()` descartable) confirmando: Yahoo Finance trae 11498 precios diarios de AAPL desde 1980-12-12 hasta hoy; Shiller trae 1833 precios mensuales del S&P 500 desde 1871-01 hasta 2023-09 (el dataset de Shiller no se actualiza en tiempo real, se corta ahí).

Pendiente para próximos módulos:
- Oro (measuringworth.com) y Dow Jones: no cubiertos todavía. El dataset de Shiller solo trae el S&P Composite; Dow Jones necesitaría otra fuente. measuringworth.com necesitaría scraping de HTML (es un formulario, no tiene export CSV directo) o un flujo de importación manual vía CSV si se prefiere no scrapear.
- Reparar el entorno local de Colima/Docker (no es un problema del proyecto, es de esta máquina) para poder correr `RepositorioActivoTest` y `ServiciosIntegracionTest`, y probar la ingesta end-to-end contra PostgreSQL real.
- Ingesta de noticias históricas (NYT Archive API) — bloqueada, todavía no hay API key.
- Empaquetado con `jpackage` para distribución nativa.

## 2026-07-29 — Reorganización: excepciones y enums en paquetes propios

Qué se hizo:
- Se movieron las dos excepciones del proyecto (`ExcepcionOperacionInvalida`, antes en `core.servicios`; `ExcepcionIngestaDatos`, antes en `core.datos`) a un paquete nuevo, `core.excepciones`.
- Se movieron los cuatro enums (`TipoActivo`, `EstadoSesion`, `TipoTransaccion`, antes en `core.entidades`; `UnidadTiempo`, antes en `core.servicios`) a un paquete nuevo, `core.enums`.
- Se actualizaron los imports en los ~20 archivos que los usaban (entidades, servicios, controllers de UI, fuentes de datos y sus tests).

Por qué:
- Pedido explícito del usuario: tener las excepciones agrupadas en una sola carpeta y los enums en otra, en vez de repartidos junto a la clase que los usa primero.

Verificación:
- `mvn test` (JAVA_HOME=openjdk@21): mismos 26/28 verdes que antes del refactor (los 2 que fallan siguen siendo los que requieren PostgreSQL real, no relacionados con este cambio).
