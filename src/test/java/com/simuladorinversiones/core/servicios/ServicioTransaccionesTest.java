package com.simuladorinversiones.core.servicios;

import com.simuladorinversiones.core.entidades.Activo;
import com.simuladorinversiones.core.entidades.CarteraUsuario;
import com.simuladorinversiones.core.entidades.EstadoSesion;
import com.simuladorinversiones.core.entidades.HistorialPrecio;
import com.simuladorinversiones.core.entidades.PosicionCartera;
import com.simuladorinversiones.core.entidades.SesionInversion;
import com.simuladorinversiones.core.entidades.TipoActivo;
import com.simuladorinversiones.core.entidades.TipoTransaccion;
import com.simuladorinversiones.core.entidades.Transaccion;
import com.simuladorinversiones.core.repositorios.RepositorioActivo;
import com.simuladorinversiones.core.repositorios.RepositorioCarteraUsuario;
import com.simuladorinversiones.core.repositorios.RepositorioHistorialPrecio;
import com.simuladorinversiones.core.repositorios.RepositorioPosicionCartera;
import com.simuladorinversiones.core.repositorios.RepositorioSesionInversion;
import com.simuladorinversiones.core.repositorios.RepositorioTransaccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioTransaccionesTest {

    @Mock
    private RepositorioSesionInversion repositorioSesion;
    @Mock
    private RepositorioActivo repositorioActivo;
    @Mock
    private RepositorioCarteraUsuario repositorioCartera;
    @Mock
    private RepositorioPosicionCartera repositorioPosicion;
    @Mock
    private RepositorioTransaccion repositorioTransaccion;
    @Mock
    private RepositorioHistorialPrecio repositorioHistorialPrecio;

    @InjectMocks
    private ServicioTransacciones servicio;

    private SesionInversion sesion;
    private Activo activo;
    private CarteraUsuario cartera;

    @BeforeEach
    void prepararEscenarioBase() {
        sesion = new SesionInversion();
        sesion.setEstado(EstadoSesion.ACTIVA);
        sesion.setFechaActual(LocalDate.of(2020, 1, 1));
        sesion.setSaldoEfectivoActual(new BigDecimal("1000.00"));

        activo = new Activo();
        activo.setNombre("Dow Jones");
        activo.setTipo(TipoActivo.INDICE);
        activo.setFechaDisponibleDesde(LocalDate.of(1900, 1, 1));

        cartera = new CarteraUsuario();

        lenient().when(repositorioTransaccion.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repositorioSesion.actualizar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void compraDescuentaSaldoYCreaPosicionNueva() {
        when(repositorioSesion.buscarPorId(1L)).thenReturn(Optional.of(sesion));
        when(repositorioActivo.buscarPorId(10L)).thenReturn(Optional.of(activo));
        when(repositorioHistorialPrecio.buscarUltimoPrecioHasta(10L, sesion.getFechaActual()))
                .thenReturn(Optional.of(precioDe("100.00")));
        when(repositorioCartera.buscarPorSesion(1L)).thenReturn(Optional.of(cartera));
        when(repositorioPosicion.buscarPorCarteraYActivo(any(), anyLong())).thenReturn(Optional.empty());

        Transaccion transaccion = servicio.comprar(1L, 10L, new BigDecimal("5"));

        assertEquals(0, new BigDecimal("500.00").compareTo(sesion.getSaldoEfectivoActual()));
        assertEquals(TipoTransaccion.COMPRA, transaccion.getTipo());

        ArgumentCaptor<PosicionCartera> captor = ArgumentCaptor.forClass(PosicionCartera.class);
        verify(repositorioPosicion).guardar(captor.capture());
        assertEquals(0, new BigDecimal("5").compareTo(captor.getValue().getCantidad()));
        assertEquals(0, new BigDecimal("100.00").compareTo(captor.getValue().getPrecioPromedioCompra()));
    }

    @Test
    void compraSumaAPosicionExistenteConPrecioPromedioPonderado() {
        PosicionCartera posicionExistente = new PosicionCartera();
        posicionExistente.setCartera(cartera);
        posicionExistente.setActivo(activo);
        posicionExistente.setCantidad(new BigDecimal("10"));
        posicionExistente.setPrecioPromedioCompra(new BigDecimal("100.00"));

        when(repositorioSesion.buscarPorId(1L)).thenReturn(Optional.of(sesion));
        when(repositorioActivo.buscarPorId(10L)).thenReturn(Optional.of(activo));
        when(repositorioHistorialPrecio.buscarUltimoPrecioHasta(10L, sesion.getFechaActual()))
                .thenReturn(Optional.of(precioDe("200.00")));
        when(repositorioCartera.buscarPorSesion(1L)).thenReturn(Optional.of(cartera));
        when(repositorioPosicion.buscarPorCarteraYActivo(any(), anyLong())).thenReturn(Optional.of(posicionExistente));

        servicio.comprar(1L, 10L, new BigDecimal("10"));

        // (10*100 + 10*200) / 20 = 150
        assertEquals(0, new BigDecimal("20").compareTo(posicionExistente.getCantidad()));
        assertEquals(0, new BigDecimal("150.0000").compareTo(posicionExistente.getPrecioPromedioCompra()));
    }

    @Test
    void compraDeActivoNoDisponibleTodaviaLanzaExcepcion() {
        activo.setFechaDisponibleDesde(LocalDate.of(2021, 1, 1));
        when(repositorioSesion.buscarPorId(1L)).thenReturn(Optional.of(sesion));
        when(repositorioActivo.buscarPorId(10L)).thenReturn(Optional.of(activo));

        assertThrows(ExcepcionOperacionInvalida.class, () -> servicio.comprar(1L, 10L, BigDecimal.ONE));
    }

    @Test
    void ventaSinPosicionPreviaLanzaExcepcion() {
        when(repositorioSesion.buscarPorId(1L)).thenReturn(Optional.of(sesion));
        when(repositorioActivo.buscarPorId(10L)).thenReturn(Optional.of(activo));
        when(repositorioHistorialPrecio.buscarUltimoPrecioHasta(10L, sesion.getFechaActual()))
                .thenReturn(Optional.of(precioDe("100.00")));
        when(repositorioCartera.buscarPorSesion(1L)).thenReturn(Optional.of(cartera));
        when(repositorioPosicion.buscarPorCarteraYActivo(any(), anyLong())).thenReturn(Optional.empty());

        assertThrows(ExcepcionOperacionInvalida.class,
                () -> servicio.vender(1L, 10L, BigDecimal.ONE));
    }

    @Test
    void ventaConCantidadMayorALaPoseidaLanzaExcepcion() {
        PosicionCartera posicionExistente = new PosicionCartera();
        posicionExistente.setCantidad(new BigDecimal("3"));
        posicionExistente.setPrecioPromedioCompra(new BigDecimal("100.00"));

        when(repositorioSesion.buscarPorId(1L)).thenReturn(Optional.of(sesion));
        when(repositorioActivo.buscarPorId(10L)).thenReturn(Optional.of(activo));
        when(repositorioHistorialPrecio.buscarUltimoPrecioHasta(10L, sesion.getFechaActual()))
                .thenReturn(Optional.of(precioDe("100.00")));
        when(repositorioCartera.buscarPorSesion(1L)).thenReturn(Optional.of(cartera));
        when(repositorioPosicion.buscarPorCarteraYActivo(any(), anyLong())).thenReturn(Optional.of(posicionExistente));

        assertThrows(ExcepcionOperacionInvalida.class,
                () -> servicio.vender(1L, 10L, new BigDecimal("5")));
        verify(repositorioPosicion, never()).actualizar(any());
    }

    @Test
    void ventaQueDejaLaPosicionEnCeroLaElimina() {
        PosicionCartera posicionExistente = new PosicionCartera();
        posicionExistente.setCantidad(new BigDecimal("5"));
        posicionExistente.setPrecioPromedioCompra(new BigDecimal("100.00"));

        when(repositorioSesion.buscarPorId(1L)).thenReturn(Optional.of(sesion));
        when(repositorioActivo.buscarPorId(10L)).thenReturn(Optional.of(activo));
        when(repositorioHistorialPrecio.buscarUltimoPrecioHasta(10L, sesion.getFechaActual()))
                .thenReturn(Optional.of(precioDe("120.00")));
        when(repositorioCartera.buscarPorSesion(1L)).thenReturn(Optional.of(cartera));
        when(repositorioPosicion.buscarPorCarteraYActivo(any(), anyLong())).thenReturn(Optional.of(posicionExistente));

        servicio.vender(1L, 10L, new BigDecimal("5"));

        assertEquals(0, new BigDecimal("1600.00").compareTo(sesion.getSaldoEfectivoActual()));
        verify(repositorioPosicion).eliminar(posicionExistente);
        verify(repositorioPosicion, never()).actualizar(any());
    }

    private HistorialPrecio precioDe(String valor) {
        HistorialPrecio precio = new HistorialPrecio();
        precio.setValor(new BigDecimal(valor));
        return precio;
    }
}
