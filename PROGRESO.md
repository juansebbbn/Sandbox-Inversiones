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
- Lógica de negocio: compra/venta de activos, avance/retroceso de tiempo, cálculo de interés simple sobre saldo negativo.
- Ingesta real de datos históricos (Dow Jones/S&P 500/Shiller, oro vía measuringworth.com, acciones individuales vía Stooq) en `core.datos`.
- Ingesta de noticias históricas (NYT Archive API) — se decidió dejarla para más adelante, todavía no hay API key.
- UI real tipo dashboard (referencia: Bullmarket), gráficos de evolución de cartera, ControlsFX/Ikonli para el look & feel.
- Empaquetado con `jpackage` para distribución nativa.
