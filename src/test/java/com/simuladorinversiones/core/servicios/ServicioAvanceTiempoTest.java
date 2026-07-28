package com.simuladorinversiones.core.servicios;

import com.simuladorinversiones.core.entidades.EstadoSesion;
import com.simuladorinversiones.core.entidades.SesionInversion;
import com.simuladorinversiones.core.repositorios.RepositorioSesionInversion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAvanceTiempoTest {

    @Mock
    private RepositorioSesionInversion repositorioSesion;

    @InjectMocks
    private ServicioAvanceTiempo servicio;

    private SesionInversion sesion;

    @BeforeEach
    void prepararSesion() {
        sesion = new SesionInversion();
        sesion.setEstado(EstadoSesion.ACTIVA);
        sesion.setFechaActual(LocalDate.of(2020, 1, 1));
        sesion.setSaldoEfectivoActual(BigDecimal.ZERO);

        when(repositorioSesion.buscarPorId(1L)).thenReturn(Optional.of(sesion));
        lenient().when(repositorioSesion.actualizar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void avanzarUnMesConSaldoNegativoAplicaInteresSimple() {
        sesion.setSaldoEfectivoActual(new BigDecimal("-1000.00"));

        servicio.avanzar(1L, UnidadTiempo.MES, 1);

        // ~30 días / 30.44 * 2% sobre -1000 ≈ -19.68, saldo queda ≈ -1019.68
        assertEquals(LocalDate.of(2020, 2, 1), sesion.getFechaActual());
        assertTrue(sesion.getSaldoEfectivoActual().compareTo(new BigDecimal("-1000.00")) < 0);
        assertTrue(sesion.getSaldoEfectivoActual().compareTo(new BigDecimal("-1030.00")) > 0);
    }

    @Test
    void avanzarConSaldoPositivoNoAplicaInteres() {
        sesion.setSaldoEfectivoActual(new BigDecimal("500.00"));

        servicio.avanzar(1L, UnidadTiempo.MES, 1);

        assertEquals(0, new BigDecimal("500.00").compareTo(sesion.getSaldoEfectivoActual()));
    }

    @Test
    void avanzarMasAlladeFechaLimiteClampeaYFinalizaLaSesion() {
        sesion.setFechaActual(LocalDate.of(2025, 12, 1));

        servicio.avanzar(1L, UnidadTiempo.MES, 3);

        assertEquals(ServicioSesionInversion.FECHA_LIMITE, sesion.getFechaActual());
        assertEquals(EstadoSesion.FINALIZADA, sesion.getEstado());
    }

    @Test
    void retrocederAntesDe1900Clampea() {
        sesion.setFechaActual(LocalDate.of(1900, 2, 1));

        servicio.retroceder(1L, UnidadTiempo.MES, 6);

        assertEquals(ServicioSesionInversion.FECHA_MINIMA, sesion.getFechaActual());
    }

    @Test
    void avanzarUnaSesionPausadaLanzaExcepcion() {
        sesion.setEstado(EstadoSesion.PAUSADA);

        assertThrows(ExcepcionOperacionInvalida.class, () -> servicio.avanzar(1L, UnidadTiempo.DIA, 1));
    }

    @Test
    void avanzarConCantidadCeroOMenorLanzaExcepcion() {
        assertThrows(ExcepcionOperacionInvalida.class, () -> servicio.avanzar(1L, UnidadTiempo.DIA, 0));
    }
}
