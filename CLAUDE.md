# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A single-user JavaFX desktop app (no auth) that lets a user simulate investing across historical market data:
create a "session" starting at some historical date, buy/sell assets at their historical prices, and advance
simulated time forward (or back), with interest accruing on negative cash balances. Stack: Java 17, JavaFX 21,
Hibernate/JPA (no Spring) over PostgreSQL, Flyway migrations, JUnit 5 + Mockito + TestFX for testing.

All code, comments, and commit messages are in Spanish. Match that convention.

## Commands

Requires Java 21 specifically — see "Java version" below.

```bash
# Start local PostgreSQL (db `simulador_inversiones`, user/pass `simulador`/`simulador`, host port 5433)
docker compose up -d

# Apply schema migrations
mvn flyway:migrate

# Run the app
mvn javafx:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ServicioTransaccionesTest

# Run a single test method
mvn test -Dtest=ServicioTransaccionesTest#nombreDelMetodo

# Compile only
mvn compile

# Import historical data (core/datos) — a standalone CLI, not a UI action
mvn compile exec:java -Dexec.mainClass="com.simuladorinversiones.core.datos.ImportadorPrincipal" \
    -Dexec.args="yahoo AAPL 'Apple Inc.' ACCION"
mvn compile exec:java -Dexec.mainClass="com.simuladorinversiones.core.datos.ImportadorPrincipal" \
    -Dexec.args="shiller"
```

### Java version

The system default JDK on this machine is Java 26, but Mockito 5.12/ByteBuddy cannot mock classes on
Java 26 bytecode (`mvn test` fails with "Mockito cannot mock this class"). Force Java 21 for any Maven
command that compiles or runs tests:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
mvn test
```

Don't change the machine's global `JAVA_HOME` — this is a per-session workaround, not a project setting.

### Database connection override

`ConfiguracionBaseDatos` reads `DB_URL`, `DB_USUARIO`, `DB_PASSWORD` env vars if present, otherwise falls
back to the dev values baked into `persistence.xml` (same credentials as `docker-compose.yml`, port 5433 —
chosen to avoid colliding with another local Postgres container on 5432).

## Architecture

### Layering

```
ui/            JavaFX controllers + FXML — one controller per screen
   ↓ calls
config/EjecutorTransaccional   — the ONLY place that opens an EntityManager + transaction
   ↓ builds
core/servicios/FabricaServicios — wires repositories → services sharing one EntityManager
   ↓ uses
core/servicios/                — business logic (Servicio*), receives repos via constructor
   ↓ uses
core/repositorios/             — one repo per entity, extends RepositorioBase<T, ID> (generic CRUD)
   ↓ uses
core/entidades/                — JPA entities (plain Hibernate, no Spring Data)

core/enums/                    — every enum used by entities/services (TipoActivo, EstadoSesion,
                                  TipoTransaccion, UnidadTiempo), in one place rather than colocated with
                                  whichever entity/service happens to use them
core/excepciones/               — every unchecked exception (ExcepcionOperacionInvalida,
                                   ExcepcionIngestaDatos), same reasoning
```

Every UI action that touches the database goes through `EjecutorTransaccional.ejecutar(fabrica -> ...)`.
It opens an `EntityManager`, begins a transaction, hands the caller a fresh `FabricaServicios` built on
that `EntityManager`, then commits or rolls back on `RuntimeException`. Services and repositories never
open their own `EntityManager` — they receive one via constructor, which is what makes them mockable with
Mockito in unit tests (see `ServicioTransaccionesTest` etc. — repos mocked; `ServiciosIntegracionTest` runs
the full stack against real PostgreSQL instead).

Screen navigation goes through `NavegadorPantallas.mostrar(fxmlPath)`, which loads the FXML into the
single `Stage`'s `Scene` and returns the controller so the caller can call an `inicializar(...)` method on
it (controllers have no zero-arg init hook of their own — the caller wires them up explicitly after load).

### Domain model

Core entities (`core/entidades`): `Usuario` → `SesionInversion` (a simulation run, `EstadoSesion`:
ACTIVA/PAUSADA/FINALIZADA) → `CarteraUsuario` (1:1 with a session) → `PosicionCartera`
(cartera–activo holdings with quantity + weighted average buy price) and `Transaccion` (buy/sell log).
Market data: `Activo` (`TipoActivo`) with a `HistorialPrecio` time series. `Noticia` for historical news
(ingestion not yet implemented). The enums (`EstadoSesion`, `TipoActivo`, `TipoTransaccion`, `UnidadTiempo`)
live in `core/enums`, not alongside the entity/service that uses them.

Business rules of note (in `core/servicios`):
- Sessions are bounded to dates between `FECHA_MINIMA` (1900-01-01) and `FECHA_LIMITE` (2026-01-01),
  defined as constants on `ServicioSesionInversion` and reused by `ServicioAvanceTiempo`.
- No short selling: you can only sell up to the quantity a session actually holds. Cash balance *can* go
  negative.
- Buying into an existing position recomputes the weighted average buy price; selling a position to zero
  deletes its `PosicionCartera` row.
- `ServicioAvanceTiempo` advances/rewinds simulated time by day/week/month/year using calendar-aware
  `LocalDate` arithmetic (not fixed day counts). Negative cash balances accrue simple interest at
  `TASA_INTERES_MENSUAL` (2%/month, prorated by days advanced) — advancing past `FECHA_LIMITE` clamps the
  date and finalizes the session; rewinding does not reverse interest already applied.
- Business-rule violations (overselling, operating on a non-active session, out-of-range dates) raise the
  unchecked `ExcepcionOperacionInvalida`.

### Persistence

- `persistence.xml` uses `hibernate.hbm2ddl.auto=validate` — schema changes must go through a new Flyway
  migration in `src/main/resources/db/migration/` (`V{n}__descripcion.sql`), never through Hibernate
  auto-DDL.
- `RepositorioBase<T, ID>` gives every repo `guardar`/`actualizar`/`buscarPorId`/`buscarTodos`/`eliminar`;
  concrete repos add JPQL query methods specific to that entity (e.g.
  `RepositorioActivo.buscarDisponiblesEn`, `RepositorioHistorialPrecio.buscarUltimoPrecioHasta`).

### Single-user model

There's no login. `FabricaServicios.obtenerOCrearUsuarioUnico()` reuses the one existing `Usuario` row or
creates it on first launch — don't build multi-user assumptions (auth, per-user scoping beyond the single
row) into new code.

## Progress log

`PROGRESO.md` is a running Spanish-language narrative of what was built, why, and what's pending — written
per work session. It complements git history rather than replacing it (git log won't tell you *why* a
design decision like the interest rate or the no-short-selling rule was made). Check it for context before
starting new work, and add an entry when you complete a module, following its existing format (Qué se hizo
/ Por qué / Pendiente).

## Historical data ingestion (`core/datos`)

Standalone, not part of the UI flow — run via `ImportadorPrincipal` (see Commands above), which wraps the
whole import in one `EjecutorTransaccional.ejecutar(...)` call. Two sources implemented so far:

- `FuenteDatosYahooFinance`: individual stocks, daily granularity, via Yahoo Finance's public chart JSON
  API (`query1.finance.yahoo.com/v8/finance/chart/{ticker}`, no API key). Originally planned to use Stooq,
  but Stooq now gates every endpoint (including bulk CSV) behind a JavaScript proof-of-work anti-bot
  challenge — bypassing that would mean deliberately evading anti-bot protection, so Yahoo Finance was used
  instead. **Must** request an explicit `period1=0&period2=<now>` range rather than `range=max`: confirmed
  empirically that `range=max&interval=1d` gets silently downsampled by Yahoo to quarterly granularity
  (`meta.dataGranularity` comes back `"3mo"`) for long histories, while an explicit period range returns the
  true daily series.
- `FuenteDatosShiller`: the S&P 500 (ticker `SP500-SHILLER`), monthly, from Robert Shiller's public dataset
  (`econ.yale.edu/~shiller/data/ie_data.xls`, an old-format `.xls` parsed with Apache POI's `HSSFWorkbook`).
  Dates are encoded as `year.month` floats (e.g. `1871.01` = Jan 1871, `2021.1` = Oct 2021 — the trailing
  zero of "10" is lost as a float), so the header row and stop condition are detected dynamically rather
  than hardcoded, since the sheet grows every time Shiller updates the file.
- `ImportadorDatosHistoricos` upserts by ticker: finds-or-creates the `Activo`, extends
  `fechaDisponibleDesde` backwards if the new data goes further back, and only inserts `HistorialPrecio`
  rows for dates not already present (skip-on-duplicate, not update-on-duplicate).

## Known pending work (from PROGRESO.md)

- Historical data ingestion: Dow Jones and gold (measuringworth.com) still not implemented — Shiller's
  dataset only covers the S&P Composite, and measuringworth.com is a query form with no direct CSV export,
  so it needs either HTML scraping or a manual-CSV-import path if pursued.
- Historical news ingestion (NYT Archive API) — blocked on an API key.
- Real dashboard UI (portfolio evolution charts, ControlsFX/Ikonli styling).
- `jpackage` native packaging for distribution.
