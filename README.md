# Simulador de Inversiones

> ⚠️ **Proyecto en construcción.** Todavía está en desarrollo activo, hay funcionalidades pendientes y la interfaz aún no está terminada. No usar en producción.

Aplicación de escritorio (JavaFX, un solo usuario, sin login) que permite simular inversiones sobre datos
históricos de mercado: crear una "sesión" a partir de una fecha histórica, comprar y vender activos a sus
precios de ese momento, y avanzar (o retroceder) el tiempo simulado, con interés acumulándose sobre saldos
de caja negativos.

## Stack

- Java 17 / JavaFX 21
- Hibernate (JPA) sobre PostgreSQL — sin Spring
- Flyway para migraciones de esquema
- JUnit 5 + Mockito + TestFX para testing

## Requisitos

- **Java 21** (ver nota abajo — el JDK del sistema puede ser otra versión)
- Docker (para levantar PostgreSQL local)
- Maven

### Sobre la versión de Java

Si el JDK por defecto del sistema no es la 21, hay que forzarla para cualquier comando de Maven que
compile o corra tests (Mockito/ByteBuddy no soportan mockear clases en versiones más nuevas):

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
```

## Cómo correrlo

```bash
# 1. Levantar PostgreSQL local (db `simulador_inversiones`, host port 5433)
docker compose up -d

# 2. Aplicar migraciones de esquema
mvn flyway:migrate

# 3. Correr la app
mvn javafx:run
```

## Tests

```bash
mvn test

# Una sola clase
mvn test -Dtest=ServicioTransaccionesTest

# Un solo método
mvn test -Dtest=ServicioTransaccionesTest#nombreDelMetodo
```

## Ingesta de datos históricos

Es un CLI independiente, no forma parte del flujo de la UI:

```bash
mvn compile exec:java -Dexec.mainClass="com.simuladorinversiones.core.datos.ImportadorPrincipal" \
    -Dexec.args="yahoo AAPL 'Apple Inc.' ACCION"

mvn compile exec:java -Dexec.mainClass="com.simuladorinversiones.core.datos.ImportadorPrincipal" \
    -Dexec.args="shiller"
```

Fuentes implementadas hasta ahora: Yahoo Finance (acciones individuales, diario) y el dataset de Robert
Shiller (S&P 500, mensual). Dow Jones, oro y noticias históricas todavía no están implementados.

## Arquitectura

```
ui/            Controladores JavaFX + FXML — un controlador por pantalla
config/        EjecutorTransaccional — único punto donde se abre EntityManager + transacción
core/servicios/     Lógica de negocio
core/repositorios/  Un repositorio por entidad (CRUD genérico + queries JPQL)
core/entidades/      Entidades JPA
core/enums/          Enums compartidos (TipoActivo, EstadoSesion, TipoTransaccion, UnidadTiempo)
core/excepciones/    Excepciones unchecked compartidas
core/datos/          Ingesta de datos históricos (CLI standalone)
```

Para el detalle completo de la arquitectura, reglas de negocio y decisiones de diseño, ver
[`CLAUDE.md`](./CLAUDE.md). El avance del desarrollo, sesión a sesión, está documentado en
[`PROGRESO.md`](./PROGRESO.md).

## Estado actual / pendiente

- [ ] Dashboard real de la cartera (gráficos de evolución, estilos con ControlsFX/Ikonli)
- [ ] Ingesta de Dow Jones y oro
- [ ] Ingesta de noticias históricas (NYT Archive API, bloqueado por falta de API key)
- [ ] Empaquetado nativo con `jpackage` para distribución

Todo el código, comentarios y mensajes de commit del proyecto están en español.
