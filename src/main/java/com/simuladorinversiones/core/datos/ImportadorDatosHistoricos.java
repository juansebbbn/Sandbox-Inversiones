package com.simuladorinversiones.core.datos;

import com.simuladorinversiones.core.entidades.Activo;
import com.simuladorinversiones.core.entidades.HistorialPrecio;
import com.simuladorinversiones.core.repositorios.RepositorioActivo;
import com.simuladorinversiones.core.repositorios.RepositorioHistorialPrecio;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Vuelca a la base de datos el resultado de una fuente de ingesta (Yahoo Finance,
 * Shiller, etc.): crea el {@link Activo} si no existe (por ticker) y agrega solo
 * los puntos de {@link HistorialPrecio} para fechas que todavía no estaban cargadas.
 */
public class ImportadorDatosHistoricos {

    private final RepositorioActivo repositorioActivo;
    private final RepositorioHistorialPrecio repositorioHistorialPrecio;

    public ImportadorDatosHistoricos(RepositorioActivo repositorioActivo,
                                      RepositorioHistorialPrecio repositorioHistorialPrecio) {
        this.repositorioActivo = repositorioActivo;
        this.repositorioHistorialPrecio = repositorioHistorialPrecio;
    }

    public ResultadoImportacion importar(ActivoDescargado datos) {
        Activo activo = repositorioActivo.buscarPorTicker(datos.ticker())
                .orElseGet(() -> crearActivo(datos));

        if (datos.fechaDisponibleDesde().isBefore(activo.getFechaDisponibleDesde())) {
            activo.setFechaDisponibleDesde(datos.fechaDisponibleDesde());
            repositorioActivo.actualizar(activo);
        }

        Set<LocalDate> fechasExistentes = new HashSet<>(
                repositorioHistorialPrecio.buscarFechasExistentes(activo.getId()));

        int nuevos = 0;
        for (PrecioDiario precio : datos.precios()) {
            if (!fechasExistentes.add(precio.fecha())) {
                continue;
            }
            HistorialPrecio registro = new HistorialPrecio();
            registro.setActivo(activo);
            registro.setFecha(precio.fecha());
            registro.setValor(precio.valor());
            repositorioHistorialPrecio.guardar(registro);
            nuevos++;
        }
        return new ResultadoImportacion(activo, nuevos, datos.precios().size() - nuevos);
    }

    private Activo crearActivo(ActivoDescargado datos) {
        Activo nuevo = new Activo();
        nuevo.setNombre(datos.nombre());
        nuevo.setTipo(datos.tipo());
        nuevo.setTicker(datos.ticker());
        nuevo.setFechaDisponibleDesde(datos.fechaDisponibleDesde());
        return repositorioActivo.guardar(nuevo);
    }
}
