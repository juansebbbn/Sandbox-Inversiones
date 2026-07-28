-- Datos de prueba para poder ejercitar la UI (compra/venta, avance de tiempo)
-- antes de que exista el módulo real de ingesta de datos históricos.
-- Los precios son ilustrativos, no series históricas reales. Esta migración
-- se reemplaza o se borra cuando llegue la ingesta real (ver PROGRESO.md).

INSERT INTO activo (nombre, tipo, fecha_disponible_desde, ticker) VALUES
    ('Oro (dato de prueba)', 'MATERIA_PRIMA', '1900-01-01', 'ORO-DEMO'),
    ('Índice Bursátil Demo', 'INDICE', '1900-01-01', 'IDX-DEMO'),
    ('Acción Demo Tecnológica', 'ACCION', '1980-01-01', 'ACC-DEMO');

INSERT INTO historial_precio (activo_id, fecha, valor)
SELECT id, fecha, valor FROM activo, (VALUES
    ('1900-01-01'::date, 20.67),
    ('1934-01-01'::date, 35.00),
    ('1971-08-01'::date, 35.00),
    ('1980-01-01'::date, 615.00),
    ('1990-01-01'::date, 401.00),
    ('2000-01-01'::date, 288.00),
    ('2010-01-01'::date, 1096.00),
    ('2020-01-01'::date, 1517.00),
    ('2025-01-01'::date, 2650.00),
    ('2026-01-01'::date, 2650.00)
) AS precios(fecha, valor)
WHERE activo.ticker = 'ORO-DEMO';

INSERT INTO historial_precio (activo_id, fecha, valor)
SELECT id, fecha, valor FROM activo, (VALUES
    ('1900-01-01'::date, 66.00),
    ('1929-09-01'::date, 381.00),
    ('1932-07-01'::date, 41.00),
    ('1950-01-01'::date, 200.00),
    ('1970-01-01'::date, 809.00),
    ('1990-01-01'::date, 2810.00),
    ('2000-01-01'::date, 11497.00),
    ('2008-09-01'::date, 11543.00),
    ('2009-03-01'::date, 6547.00),
    ('2020-01-01'::date, 28538.00),
    ('2025-01-01'::date, 43000.00),
    ('2026-01-01'::date, 43000.00)
) AS precios(fecha, valor)
WHERE activo.ticker = 'IDX-DEMO';

INSERT INTO historial_precio (activo_id, fecha, valor)
SELECT id, fecha, valor FROM activo, (VALUES
    ('1980-01-01'::date, 1.00),
    ('1990-01-01'::date, 5.50),
    ('2000-01-01'::date, 45.00),
    ('2001-06-01'::date, 18.00),
    ('2010-01-01'::date, 30.00),
    ('2020-01-01'::date, 135.00),
    ('2025-01-01'::date, 230.00),
    ('2026-01-01'::date, 230.00)
) AS precios(fecha, valor)
WHERE activo.ticker = 'ACC-DEMO';
