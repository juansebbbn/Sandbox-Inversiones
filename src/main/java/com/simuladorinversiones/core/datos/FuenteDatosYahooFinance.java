package com.simuladorinversiones.core.datos;

import com.simuladorinversiones.core.enums.TipoActivo;
import com.simuladorinversiones.core.excepciones.ExcepcionIngestaDatos;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Descarga series históricas diarias de acciones desde la API pública de gráficos
 * de Yahoo Finance ({@code query1.finance.yahoo.com/v8/finance/chart}). No requiere
 * API key. Se eligió por sobre Stooq porque Stooq exige resolver un desafío
 * anti-bot (proof-of-work en JavaScript) que un cliente HTTP simple no puede pasar
 * sin implementar una evasión deliberada de esa protección.
 *
 * <p>Se pide el rango con {@code period1=0} (época Unix) en vez de {@code range=max}:
 * con {@code range=max}, Yahoo recorta silenciosamente la granularidad a trimestral
 * para historiales largos (confirmado empíricamente: {@code meta.dataGranularity}
 * vuelve "3mo" en vez de "1d"), mientras que con un rango explícito devuelve la
 * serie diaria completa desde el primer día de cotización.
 */
public class FuenteDatosYahooFinance {

    private static final String URL_BASE =
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?period1=0&period2=%d&interval=1d";

    private final HttpClient clienteHttp;

    public FuenteDatosYahooFinance() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
    }

    FuenteDatosYahooFinance(HttpClient clienteHttp) {
        this.clienteHttp = clienteHttp;
    }

    public ActivoDescargado descargar(String ticker, String nombre, TipoActivo tipo) {
        String tickerNormalizado = ticker.trim().toUpperCase(Locale.ROOT);
        String url = String.format(URL_BASE, tickerNormalizado, Instant.now().getEpochSecond());
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (compatible; SimuladorInversiones/1.0)")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> respuesta;
        try {
            respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ExcepcionIngestaDatos("No se pudo contactar a Yahoo Finance para " + tickerNormalizado, e);
        }
        if (respuesta.statusCode() != 200) {
            throw new ExcepcionIngestaDatos(
                    "Yahoo Finance devolvió HTTP " + respuesta.statusCode() + " para " + tickerNormalizado);
        }
        return parsearRespuesta(respuesta.body(), tickerNormalizado, nombre, tipo);
    }

    static ActivoDescargado parsearRespuesta(String cuerpoJson, String ticker, String nombre, TipoActivo tipo) {
        JSONObject grafico = new JSONObject(cuerpoJson).getJSONObject("chart");
        if (!grafico.isNull("error")) {
            JSONObject error = grafico.getJSONObject("error");
            throw new ExcepcionIngestaDatos(
                    "Yahoo Finance no tiene datos para " + ticker + ": " + error.optString("description", error.toString()));
        }

        JSONArray resultados = grafico.optJSONArray("result");
        if (resultados == null || resultados.isEmpty()) {
            throw new ExcepcionIngestaDatos("Yahoo Finance no devolvió resultados para " + ticker);
        }
        JSONObject resultado = resultados.getJSONObject(0);
        JSONArray marcasTiempo = resultado.getJSONArray("timestamp");
        JSONArray cierres = resultado.getJSONObject("indicators")
                .getJSONArray("quote").getJSONObject(0).getJSONArray("close");

        List<PrecioDiario> precios = new ArrayList<>();
        for (int i = 0; i < marcasTiempo.length(); i++) {
            if (cierres.isNull(i)) {
                continue;
            }
            LocalDate fecha = Instant.ofEpochSecond(marcasTiempo.getLong(i)).atZone(ZoneOffset.UTC).toLocalDate();
            precios.add(new PrecioDiario(fecha, BigDecimal.valueOf(cierres.getDouble(i))));
        }
        if (precios.isEmpty()) {
            throw new ExcepcionIngestaDatos("Yahoo Finance no tiene precios de cierre utilizables para " + ticker);
        }
        return new ActivoDescargado(ticker, nombre, tipo, precios.get(0).fecha(), precios);
    }
}
