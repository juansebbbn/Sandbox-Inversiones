package com.simuladorinversiones.core.datos;

import com.simuladorinversiones.core.entidades.Activo;
import com.simuladorinversiones.core.enums.TipoActivo;
import com.simuladorinversiones.core.repositorios.RepositorioActivo;
import com.simuladorinversiones.core.repositorios.RepositorioHistorialPrecio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportadorDatosHistoricosTest {

    @Mock
    private RepositorioActivo repositorioActivo;
    @Mock
    private RepositorioHistorialPrecio repositorioHistorialPrecio;

    @InjectMocks
    private ImportadorDatosHistoricos importador;

    @Test
    void creaElActivoCuandoNoExisteYCargaTodosLosPrecios() {
        ActivoDescargado datos = new ActivoDescargado("ACC", "Acción de prueba", TipoActivo.ACCION,
                LocalDate.of(2020, 1, 1), List.of(
                        new PrecioDiario(LocalDate.of(2020, 1, 1), new BigDecimal("10.0")),
                        new PrecioDiario(LocalDate.of(2020, 1, 2), new BigDecimal("11.0"))));

        when(repositorioActivo.buscarPorTicker("ACC")).thenReturn(Optional.empty());
        Activo activoCreado = new Activo();
        activoCreado.setTicker("ACC");
        activoCreado.setNombre("Acción de prueba");
        activoCreado.setFechaDisponibleDesde(LocalDate.of(2020, 1, 1));
        when(repositorioActivo.guardar(any(Activo.class))).thenReturn(activoCreado);
        when(repositorioHistorialPrecio.buscarFechasExistentes(any())).thenReturn(List.of());

        ResultadoImportacion resultado = importador.importar(datos);

        assertEquals(2, resultado.nuevos());
        assertEquals(0, resultado.yaExistentes());
        verify(repositorioActivo, never()).actualizar(any());
        verify(repositorioHistorialPrecio, org.mockito.Mockito.times(2)).guardar(any());
    }

    @Test
    void saltaLasFechasQueYaEstabanCargadas() {
        Activo activoExistente = new Activo();
        activoExistente.setTicker("ACC");
        activoExistente.setFechaDisponibleDesde(LocalDate.of(2019, 1, 1));

        ActivoDescargado datos = new ActivoDescargado("ACC", "Acción de prueba", TipoActivo.ACCION,
                LocalDate.of(2020, 1, 1), List.of(
                        new PrecioDiario(LocalDate.of(2020, 1, 1), new BigDecimal("10.0")),
                        new PrecioDiario(LocalDate.of(2020, 1, 2), new BigDecimal("11.0"))));

        when(repositorioActivo.buscarPorTicker("ACC")).thenReturn(Optional.of(activoExistente));
        when(repositorioHistorialPrecio.buscarFechasExistentes(any()))
                .thenReturn(List.of(LocalDate.of(2020, 1, 1)));

        ResultadoImportacion resultado = importador.importar(datos);

        assertEquals(1, resultado.nuevos());
        assertEquals(1, resultado.yaExistentes());
        ArgumentCaptor<com.simuladorinversiones.core.entidades.HistorialPrecio> captor =
                ArgumentCaptor.forClass(com.simuladorinversiones.core.entidades.HistorialPrecio.class);
        verify(repositorioHistorialPrecio).guardar(captor.capture());
        assertEquals(LocalDate.of(2020, 1, 2), captor.getValue().getFecha());
    }

    @Test
    void extiendeFechaDisponibleDesdeSiLosDatosNuevosVanMasAtras() {
        Activo activoExistente = new Activo();
        activoExistente.setTicker("ACC");
        activoExistente.setFechaDisponibleDesde(LocalDate.of(2020, 1, 1));

        ActivoDescargado datos = new ActivoDescargado("ACC", "Acción de prueba", TipoActivo.ACCION,
                LocalDate.of(2015, 1, 1), List.of(
                        new PrecioDiario(LocalDate.of(2015, 1, 1), new BigDecimal("5.0"))));

        when(repositorioActivo.buscarPorTicker("ACC")).thenReturn(Optional.of(activoExistente));
        when(repositorioHistorialPrecio.buscarFechasExistentes(any())).thenReturn(List.of());

        importador.importar(datos);

        assertEquals(LocalDate.of(2015, 1, 1), activoExistente.getFechaDisponibleDesde());
        verify(repositorioActivo).actualizar(activoExistente);
    }
}
