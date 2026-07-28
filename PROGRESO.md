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
