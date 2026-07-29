package com.simuladorinversiones.core.datos;

import com.simuladorinversiones.core.enums.TipoActivo;
import com.simuladorinversiones.core.excepciones.ExcepcionIngestaDatos;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FuenteDatosShillerTest {

    @Test
    void parseaFilasDeDatosHastaElPieDeNota() {
        try (Workbook libro = new HSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Data");

            // Filas de título antes del encabezado real, como en el archivo real de Shiller.
            hoja.createRow(1).createCell(0).setCellValue("Stock Market Data Used in \"Irrational Exuberance\"");

            Row encabezado = hoja.createRow(7);
            encabezado.createCell(0).setCellValue("Date");
            encabezado.createCell(1).setCellValue("P");

            Row fila1 = hoja.createRow(8);
            fila1.createCell(0).setCellValue(1871.01);
            fila1.createCell(1).setCellValue(4.44);

            Row fila2 = hoja.createRow(9);
            fila2.createCell(0).setCellValue(2021.1); // octubre (el 0 final de "10" se pierde como double)
            fila2.createCell(1).setCellValue(4460.71);

            // Pie de nota textual: debe cortar el parseo, no reventar.
            Row pie = hoja.createRow(10);
            pie.createCell(0).setCellValue("");
            pie.createCell(1).setCellValue("Sept price is Sept 1st close");

            ActivoDescargado datos = FuenteDatosShiller.parsearHoja(hoja);

            assertEquals("SP500-SHILLER", datos.ticker());
            assertEquals(TipoActivo.INDICE, datos.tipo());
            assertEquals(2, datos.precios().size());
            assertEquals(LocalDate.of(1871, 1, 1), datos.precios().get(0).fecha());
            assertEquals(0, new BigDecimal("4.44").compareTo(datos.precios().get(0).valor()));
            assertEquals(LocalDate.of(2021, 10, 1), datos.precios().get(1).fecha());
            assertEquals(datos.precios().get(0).fecha(), datos.fechaDisponibleDesde());
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void lanzaExcepcionSiNoEncuentraEncabezado() {
        try (Workbook libro = new HSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Data");
            hoja.createRow(0).createCell(0).setCellValue("Sin encabezado acá");

            assertThrows(ExcepcionIngestaDatos.class, () -> FuenteDatosShiller.parsearHoja(hoja));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
