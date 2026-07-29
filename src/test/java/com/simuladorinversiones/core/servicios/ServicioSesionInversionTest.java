package com.simuladorinversiones.core.servicios;

import com.simuladorinversiones.core.entidades.SesionInversion;
import com.simuladorinversiones.core.entidades.Usuario;
import com.simuladorinversiones.core.enums.EstadoSesion;
import com.simuladorinversiones.core.excepciones.ExcepcionOperacionInvalida;
import com.simuladorinversiones.core.repositorios.RepositorioCarteraUsuario;
import com.simuladorinversiones.core.repositorios.RepositorioSesionInversion;
import com.simuladorinversiones.core.repositorios.RepositorioUsuario;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioSesionInversionTest {

    @Mock
    private RepositorioSesionInversion repositorioSesion;
    @Mock
    private RepositorioUsuario repositorioUsuario;
    @Mock
    private RepositorioCarteraUsuario repositorioCartera;

    @InjectMocks
    private ServicioSesionInversion servicio;

    @Test
    void crearSesionPersisteSesionYCarteraVacia() {
        when(repositorioUsuario.buscarPorId(1L)).thenReturn(Optional.of(new Usuario()));

        SesionInversion sesion = servicio.crearSesion(1L, LocalDate.of(1950, 6, 1), new BigDecimal("10000"));

        assertEquals(LocalDate.of(1950, 6, 1), sesion.getFechaActual());
        assertEquals(0, new BigDecimal("10000").compareTo(sesion.getSaldoEfectivoActual()));
        assertEquals(EstadoSesion.ACTIVA, sesion.getEstado());
        assertEquals(ServicioSesionInversion.FECHA_LIMITE, sesion.getFechaLimite());
        verify(repositorioCartera).guardar(argThat(cartera -> cartera.getSesion() == sesion));
    }

    @Test
    void crearSesionConFechaAnteriorA1900LanzaExcepcion() {
        assertThrows(ExcepcionOperacionInvalida.class,
                () -> servicio.crearSesion(1L, LocalDate.of(1899, 12, 31), BigDecimal.TEN));
        verify(repositorioUsuario, never()).buscarPorId(any());
    }

    @Test
    void crearSesionConFechaEnOLuegoDeFechaLimiteLanzaExcepcion() {
        assertThrows(ExcepcionOperacionInvalida.class,
                () -> servicio.crearSesion(1L, ServicioSesionInversion.FECHA_LIMITE, BigDecimal.TEN));
    }

    @Test
    void crearSesionConUsuarioInexistenteLanzaExcepcion() {
        when(repositorioUsuario.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(ExcepcionOperacionInvalida.class,
                () -> servicio.crearSesion(99L, LocalDate.of(1950, 1, 1), BigDecimal.TEN));
    }

    @Test
    void pausarSesionActivaCambiaEstado() {
        SesionInversion sesion = new SesionInversion();
        sesion.setEstado(EstadoSesion.ACTIVA);
        when(repositorioSesion.buscarPorId(5L)).thenReturn(Optional.of(sesion));
        when(repositorioSesion.actualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        SesionInversion resultado = servicio.pausar(5L);

        assertEquals(EstadoSesion.PAUSADA, resultado.getEstado());
    }

    @Test
    void reanudarSesionQueNoEstaPausadaLanzaExcepcion() {
        SesionInversion sesion = new SesionInversion();
        sesion.setEstado(EstadoSesion.ACTIVA);
        when(repositorioSesion.buscarPorId(5L)).thenReturn(Optional.of(sesion));

        assertThrows(ExcepcionOperacionInvalida.class, () -> servicio.reanudar(5L));
    }
}
