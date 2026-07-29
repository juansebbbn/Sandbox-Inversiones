package com.simuladorinversiones.core.datos;

import com.simuladorinversiones.config.ConfiguracionBaseDatos;
import com.simuladorinversiones.config.EjecutorTransaccional;
import com.simuladorinversiones.core.enums.TipoActivo;

import java.util.Locale;

/**
 * Punto de entrada de línea de comandos para la ingesta de datos históricos.
 * No es parte del flujo de la UI: se ejecuta como un script aparte, por ejemplo:
 * <pre>
 *   mvn compile exec:java -Dexec.mainClass="com.simuladorinversiones.core.datos.ImportadorPrincipal" \
 *       -Dexec.args="yahoo AAPL 'Apple Inc.' ACCION"
 *
 *   mvn compile exec:java -Dexec.mainClass="com.simuladorinversiones.core.datos.ImportadorPrincipal" \
 *       -Dexec.args="shiller"
 * </pre>
 */
public final class ImportadorPrincipal {

    private ImportadorPrincipal() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            imprimirAyuda();
            return;
        }
        try {
            ActivoDescargado datos = descargar(args);
            ResultadoImportacion resultado = EjecutorTransaccional.ejecutar(
                    fabrica -> fabrica.servicioImportacion().importar(datos));
            System.out.printf(
                    "Importado %s (%s): %d precios nuevos, %d ya existentes.%n",
                    resultado.activo().getNombre(), resultado.activo().getTicker(),
                    resultado.nuevos(), resultado.yaExistentes());
        } catch (RuntimeException e) {
            System.err.println("Error en la importación: " + e.getMessage());
            System.exit(1);
        } finally {
            ConfiguracionBaseDatos.cerrar();
        }
    }

    private static ActivoDescargado descargar(String[] args) {
        String fuente = args[0].toLowerCase(Locale.ROOT);
        return switch (fuente) {
            case "yahoo" -> descargarDesdeYahoo(args);
            case "shiller" -> new FuenteDatosShiller().descargarSp500();
            default -> throw new IllegalArgumentException(
                    "Fuente desconocida '" + fuente + "': usar 'yahoo' o 'shiller'");
        };
    }

    private static ActivoDescargado descargarDesdeYahoo(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Uso: yahoo <ticker> <nombre> [tipo=ACCION]");
        }
        String ticker = args[1];
        String nombre = args[2];
        TipoActivo tipo = args.length >= 4
                ? TipoActivo.valueOf(args[3].toUpperCase(Locale.ROOT))
                : TipoActivo.ACCION;
        return new FuenteDatosYahooFinance().descargar(ticker, nombre, tipo);
    }

    private static void imprimirAyuda() {
        System.out.println("""
                Uso:
                  yahoo <ticker> <nombre> [tipo=ACCION]   Importa una acción desde Yahoo Finance
                  shiller                                  Importa el S&P 500 desde el dataset de Shiller
                """);
    }
}
