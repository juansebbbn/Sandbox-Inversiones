package com.simuladorinversiones.core.datos;

import com.simuladorinversiones.core.enums.TipoActivo;
import com.simuladorinversiones.core.excepciones.ExcepcionIngestaDatos;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Descarga y parsea el dataset histórico público de Robert Shiller
 * ({@code ie_data.xls}, econ.yale.edu), que trae el precio nominal mensual del
 * S&amp;P Composite desde 1871. No requiere API key.
 */
public class FuenteDatosShiller {

    private static final String URL_DATASET = "http://www.econ.yale.edu/~shiller/data/ie_data.xls";
    private static final String NOMBRE_HOJA = "Data";
    private static final String TICKER_SP500 = "SP500-SHILLER";

    private final HttpClient clienteHttp;

    public FuenteDatosShiller() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
    }

    FuenteDatosShiller(HttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;
    }

    public ActivoDescargado descargarSp500() {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(URL_DATASET))
                .header("User-Agent", "Mozilla/5.0 (compatible; SimuladorInversiones/1.0)")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<byte[]> respuesta;
        try {
            respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ExcepcionIngestaDatos("No se pudo descargar el dataset de Shiller", e);
        }
        if (respuesta.statusCode() != 200) {
            throw new ExcepcionIngestaDatos("El dataset de Shiller devolvió HTTP " + respuesta.statusCode());
        }

        try (HSSFWorkbook libro = new HSSFWorkbook(new ByteArrayInputStream(respuesta.body()))) {
            return parsearHoja(libro.getSheet(NOMBRE_HOJA));
        } catch (IOException e) {
            throw new ExcepcionIngestaDatos("No se pudo leer el archivo .xls de Shiller", e);
        }
    }

    static ActivoDescargado parsearHoja(Sheet hoja) {
        if (hoja == null) {
            throw new ExcepcionIngestaDatos("El dataset de Shiller no tiene una hoja '" + NOMBRE_HOJA + "'");
        }

        int filaEncabezado = -1;
        for (Row fila : hoja) {
            Cell primeraCelda = fila.getCell(0);
            if (primeraCelda != null && primeraCelda.getCellType() == CellType.STRING
                    && "Date".equals(primeraCelda.getStringCellValue().trim())) {
                filaEncabezado = fila.getRowNum();
                break;
            }
        }
        if (filaEncabezado < 0) {
            throw new ExcepcionIngestaDatos("No se encontró la fila de encabezado ('Date') en la hoja de Shiller");
        }

        // El dataset codifica la fecha como año.mes (ej. 1871.01 = enero de 1871,
        // 2021.1 = octubre de 2021, porque el cero final de "10" se pierde como float).
        List<PrecioDiario> precios = new ArrayList<>();
        for (int numeroFila = filaEncabezado + 1; numeroFila <= hoja.getLastRowNum(); numeroFila++) {
            Row fila = hoja.getRow(numeroFila);
            if (fila == null) {
                break;
            }
            Cell celdaFecha = fila.getCell(0);
            Cell celdaPrecio = fila.getCell(1);
            if (celdaFecha == null || celdaPrecio == null
                    || celdaFecha.getCellType() != CellType.NUMERIC
                    || celdaPrecio.getCellType() != CellType.NUMERIC) {
                break;
            }
            double fechaNumerica = celdaFecha.getNumericCellValue();
            int anio = (int) Math.floor(fechaNumerica + 1e-9);
            int mes = (int) Math.round((fechaNumerica - anio) * 100);
            if (mes < 1 || mes > 12 || anio < 1800 || anio > 2200) {
                break;
            }
            precios.add(new PrecioDiario(LocalDate.of(anio, mes, 1), BigDecimal.valueOf(celdaPrecio.getNumericCellValue())));
        }
        if (precios.isEmpty()) {
            throw new ExcepcionIngestaDatos("No se encontraron filas de datos en la hoja de Shiller");
        }
        return new ActivoDescargado(TICKER_SP500, "S&P 500 (dataset Shiller)", TipoActivo.INDICE,
                precios.get(0).fecha(), precios);
    }
}
