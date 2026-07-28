package com.simuladorinversiones.core.servicios;

import com.simuladorinversiones.core.entidades.Activo;
import com.simuladorinversiones.core.entidades.HistorialPrecio;
import com.simuladorinversiones.core.repositorios.RepositorioActivo;
import com.simuladorinversiones.core.repositorios.RepositorioHistorialPrecio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Consulta de activos y precios en el contexto de una fecha simulada. */
public class ServicioActivos {

    private final RepositorioActivo repositorioActivo;
    private final RepositorioHistorialPrecio repositorioHistorialPrecio;

    public ServicioActivos(RepositorioActivo repositorioActivo, RepositorioHistorialPrecio repositorioHistorialPrecio) {
        this.repositorioActivo = repositorioActivo;
        this.repositorioHistorialPrecio = repositorioHistorialPrecio;
    }

    /** Activos que ya existían en la fecha simulada dada (ej. no mostrar acciones de empresas que aún no cotizaban). */
    public List<Activo> listarDisponiblesEn(LocalDate fecha) {
        return repositorioActivo.buscarDisponiblesEn(fecha);
    }

    /**
     * Último precio conocido de un activo en o antes de la fecha dada. Si la fuente de datos
     * tiene muestreo mensual y la fecha simulada avanzó solo un día, puede devolver el mismo
     * precio del último dato disponible: es el comportamiento esperado, no un error.
     */
    public Optional<HistorialPrecio> obtenerUltimoPrecio(Long activoId, LocalDate fecha) {
        return repositorioHistorialPrecio.buscarUltimoPrecioHasta(activoId, fecha);
    }
}
