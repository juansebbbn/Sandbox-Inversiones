package com.simuladorinversiones.core.datos;

import com.simuladorinversiones.core.enums.TipoActivo;
import com.simuladorinversiones.core.excepciones.ExcepcionIngestaDatos;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FuenteDatosYahooFinanceTest {

    @Test
    void parseaPreciosDeCierreIgnorandoNulos() {
        String json = """
                {"chart":{"result":[{
                    "timestamp":[1577836800,1577923200,1578009600],
                    "indicators":{"quote":[{"close":[100.0,null,102.5]}]}
                }],"error":null}}
                """;

        ActivoDescargado datos = FuenteDatosYahooFinance.parsearRespuesta(json, "ACC", "Acción de prueba", TipoActivo.ACCION);

        assertEquals("ACC", datos.ticker());
        assertEquals("Acción de prueba", datos.nombre());
        assertEquals(TipoActivo.ACCION, datos.tipo());
        assertEquals(2, datos.precios().size());
        assertEquals(LocalDate.of(2020, 1, 1), datos.precios().get(0).fecha());
        assertEquals(0, new BigDecimal("100.0").compareTo(datos.precios().get(0).valor()));
        assertEquals(LocalDate.of(2020, 1, 3), datos.precios().get(1).fecha());
        assertEquals(datos.precios().get(0).fecha(), datos.fechaDisponibleDesde());
    }

    @Test
    void lanzaExcepcionCuandoYahooDevuelveError() {
        String json = """
                {"chart":{"result":null,"error":{"code":"Not Found","description":"No data found, symbol may be delisted"}}}
                """;

        ExcepcionIngestaDatos excepcion = assertThrows(ExcepcionIngestaDatos.class,
                () -> FuenteDatosYahooFinance.parsearRespuesta(json, "NOEXISTE", "Ticker inválido", TipoActivo.ACCION));
        assertEquals(true, excepcion.getMessage().contains("NOEXISTE"));
    }

    @Test
    void lanzaExcepcionSiNoHayPreciosUtilizables() {
        String json = """
                {"chart":{"result":[{
                    "timestamp":[1577836800],
                    "indicators":{"quote":[{"close":[null]}]}
                }],"error":null}}
                """;

        assertThrows(ExcepcionIngestaDatos.class,
                () -> FuenteDatosYahooFinance.parsearRespuesta(json, "ACC", "Acción de prueba", TipoActivo.ACCION));
    }
}
