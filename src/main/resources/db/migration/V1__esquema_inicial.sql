-- Esquema inicial: usuario, sesiones de inversión, activos, precios históricos,
-- cartera, transacciones y noticias. Ver PROGRESO.md para el contexto de este módulo.

CREATE TABLE usuario (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    capital_inicial_por_defecto NUMERIC(18, 2)
);

CREATE TABLE sesion_inversion (
    id                            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id                    BIGINT         NOT NULL REFERENCES usuario (id),
    fecha_inicio                  DATE           NOT NULL,
    fecha_actual                  DATE           NOT NULL,
    fecha_limite                  DATE           NOT NULL,
    capital_inicial               NUMERIC(18, 2) NOT NULL,
    saldo_efectivo_actual         NUMERIC(18, 2) NOT NULL,
    estado                        VARCHAR(20)    NOT NULL,
    velocidad_avance_continuo     INTEGER,
    fecha_creacion                TIMESTAMP      NOT NULL DEFAULT now(),
    fecha_ultima_modificacion     TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT sesion_inversion_fecha_valida CHECK (fecha_inicio >= DATE '1900-01-01')
);

CREATE INDEX idx_sesion_inversion_usuario ON sesion_inversion (usuario_id);

CREATE TABLE activo (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre                 VARCHAR(200) NOT NULL,
    tipo                   VARCHAR(30)  NOT NULL,
    fecha_disponible_desde DATE         NOT NULL,
    ticker                 VARCHAR(20)  NOT NULL UNIQUE
);

CREATE TABLE historial_precio (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    activo_id BIGINT         NOT NULL REFERENCES activo (id),
    fecha     DATE           NOT NULL,
    valor     NUMERIC(18, 4) NOT NULL,
    CONSTRAINT uq_historial_precio_activo_fecha UNIQUE (activo_id, fecha)
);

CREATE INDEX idx_historial_precio_fecha ON historial_precio (fecha);

CREATE TABLE cartera_usuario (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sesion_id BIGINT NOT NULL UNIQUE REFERENCES sesion_inversion (id)
);

CREATE TABLE posicion_cartera (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cartera_id               BIGINT         NOT NULL REFERENCES cartera_usuario (id),
    activo_id                BIGINT         NOT NULL REFERENCES activo (id),
    cantidad                 NUMERIC(18, 6) NOT NULL,
    precio_promedio_compra   NUMERIC(18, 4) NOT NULL,
    CONSTRAINT uq_posicion_cartera_activo UNIQUE (cartera_id, activo_id)
);

CREATE TABLE transaccion (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sesion_id       BIGINT         NOT NULL REFERENCES sesion_inversion (id),
    activo_id       BIGINT         NOT NULL REFERENCES activo (id),
    tipo            VARCHAR(10)    NOT NULL,
    cantidad        NUMERIC(18, 6) NOT NULL,
    precio_unitario NUMERIC(18, 4) NOT NULL,
    fecha           DATE           NOT NULL
);

CREATE INDEX idx_transaccion_sesion ON transaccion (sesion_id);

CREATE TABLE noticia (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha                 DATE         NOT NULL,
    titulo                VARCHAR(500) NOT NULL,
    resumen               TEXT,
    activo_relacionado_id BIGINT REFERENCES activo (id),
    fuente                VARCHAR(200)
);

CREATE INDEX idx_noticia_fecha ON noticia (fecha);
CREATE INDEX idx_noticia_activo_relacionado ON noticia (activo_relacionado_id);
