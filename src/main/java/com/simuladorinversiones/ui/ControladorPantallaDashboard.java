package com.simuladorinversiones.ui;

import com.simuladorinversiones.config.EjecutorTransaccional;
import com.simuladorinversiones.core.entidades.Activo;
import com.simuladorinversiones.core.entidades.EstadoSesion;
import com.simuladorinversiones.core.entidades.HistorialPrecio;
import com.simuladorinversiones.core.entidades.SesionInversion;
import com.simuladorinversiones.core.servicios.ExcepcionOperacionInvalida;
import com.simuladorinversiones.core.servicios.FabricaServicios;
import com.simuladorinversiones.core.servicios.PosicionPortafolio;
import com.simuladorinversiones.core.servicios.UnidadTiempo;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Pantalla principal: fecha/saldo/patrimonio de la sesión, activos disponibles, cartera, tiempo y noticias. */
public class ControladorPantallaDashboard {

    @FXML
    private Label etiquetaFecha;
    @FXML
    private Label etiquetaSaldo;
    @FXML
    private Label etiquetaPatrimonio;
    @FXML
    private Label etiquetaEstado;
    @FXML
    private Label etiquetaVelocidad;
    @FXML
    private Button botonPausar;
    @FXML
    private Button botonFinalizar;
    @FXML
    private Button botonPlayPausaContinuo;
    @FXML
    private ComboBox<UnidadTiempo> comboUnidadTiempo;
    @FXML
    private Spinner<Integer> spinnerCantidadTiempo;
    @FXML
    private Slider sliderVelocidad;

    @FXML
    private TableView<FilaActivoDisponible> tablaActivos;
    @FXML
    private TableColumn<FilaActivoDisponible, String> columnaNombreActivo;
    @FXML
    private TableColumn<FilaActivoDisponible, String> columnaTickerActivo;
    @FXML
    private TableColumn<FilaActivoDisponible, String> columnaTipoActivo;
    @FXML
    private TableColumn<FilaActivoDisponible, String> columnaPrecioActivo;
    @FXML
    private TableColumn<FilaActivoDisponible, Void> columnaAccionesActivo;

    @FXML
    private TableView<PosicionPortafolio> tablaCartera;
    @FXML
    private TableColumn<PosicionPortafolio, String> columnaNombrePosicion;
    @FXML
    private TableColumn<PosicionPortafolio, BigDecimal> columnaCantidadPosicion;
    @FXML
    private TableColumn<PosicionPortafolio, String> columnaPrecioPromedioPosicion;
    @FXML
    private TableColumn<PosicionPortafolio, String> columnaPrecioActualPosicion;
    @FXML
    private TableColumn<PosicionPortafolio, String> columnaValorPosicion;
    @FXML
    private TableColumn<PosicionPortafolio, BigDecimal> columnaGananciaPosicion;

    private NavegadorPantallas navegador;
    private Long sesionId;
    private Long usuarioId;
    private Timeline avanceContinuo;

    public void inicializar(NavegadorPantallas navegador, Long sesionId, Long usuarioId) {
        this.navegador = navegador;
        this.sesionId = sesionId;
        this.usuarioId = usuarioId;
        refrescar();
    }

    @FXML
    private void initialize() {
        comboUnidadTiempo.setItems(FXCollections.observableArrayList(UnidadTiempo.values()));
        comboUnidadTiempo.setConverter(convertidorUnidadTiempo());
        comboUnidadTiempo.getSelectionModel().select(UnidadTiempo.MES);

        spinnerCantidadTiempo.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1));

        actualizarEtiquetaVelocidad();
        sliderVelocidad.valueProperty().addListener((observable, valorViejo, valorNuevo) -> {
            actualizarEtiquetaVelocidad();
            if (avanceContinuo != null && avanceContinuo.getStatus() == Animation.Status.RUNNING) {
                iniciarAvanceContinuo();
            }
        });

        configurarTablaActivos();
        configurarTablaCartera();
    }

    private void configurarTablaActivos() {
        columnaNombreActivo.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue().nombre()));
        columnaTickerActivo.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue().ticker()));
        columnaTipoActivo.setCellValueFactory(datos -> new ReadOnlyObjectWrapper<>(datos.getValue().tipo()));
        columnaPrecioActivo.setCellValueFactory(
                datos -> new ReadOnlyObjectWrapper<>(formatearMoneda(datos.getValue().precioActual())));

        columnaAccionesActivo.setCellFactory(columna -> new TableCell<>() {
            private final Button botonComprar = new Button("Comprar");
            private final Button botonVender = new Button("Vender");
            private final HBox contenedor = new HBox(6, botonComprar, botonVender);

            {
                botonComprar.getStyleClass().add("boton-comprar");
                botonVender.getStyleClass().add("boton-vender");
                botonComprar.setOnAction(evento -> comprarActivo(getTableView().getItems().get(getIndex())));
                botonVender.setOnAction(evento -> venderActivo(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean vacio) {
                super.updateItem(item, vacio);
                setGraphic(vacio ? null : contenedor);
            }
        });
    }

    private void configurarTablaCartera() {
        columnaNombrePosicion.setCellValueFactory(
                datos -> new ReadOnlyObjectWrapper<>(datos.getValue().nombreActivo()));
        columnaCantidadPosicion.setCellValueFactory(
                datos -> new ReadOnlyObjectWrapper<>(datos.getValue().cantidad()));
        columnaPrecioPromedioPosicion.setCellValueFactory(
                datos -> new ReadOnlyObjectWrapper<>(formatearMoneda(datos.getValue().precioPromedioCompra())));
        columnaPrecioActualPosicion.setCellValueFactory(
                datos -> new ReadOnlyObjectWrapper<>(formatearMoneda(datos.getValue().precioActual())));
        columnaValorPosicion.setCellValueFactory(
                datos -> new ReadOnlyObjectWrapper<>(formatearMoneda(datos.getValue().valorActual())));
        columnaGananciaPosicion.setCellValueFactory(
                datos -> new ReadOnlyObjectWrapper<>(datos.getValue().gananciaPerdida()));
        columnaGananciaPosicion.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal valor, boolean vacio) {
                super.updateItem(valor, vacio);
                getStyleClass().removeAll("positivo", "negativo");
                if (vacio || valor == null) {
                    setText(null);
                } else {
                    setText(formatearMoneda(valor));
                    getStyleClass().add(valor.signum() >= 0 ? "positivo" : "negativo");
                }
            }
        });
    }

    private void refrescar() {
        EstadoDashboard estado = EjecutorTransaccional.ejecutar(fabrica -> {
            SesionInversion sesion = fabrica.servicioSesion().buscarPorId(sesionId)
                    .orElseThrow(() -> new IllegalStateException("No existe la sesión " + sesionId));

            List<FilaActivoDisponible> filasActivos = fabrica.servicioActivos()
                    .listarDisponiblesEn(sesion.getFechaActual()).stream()
                    .map(activo -> filaDe(activo, sesion, fabrica))
                    .toList();

            List<PosicionPortafolio> posiciones = fabrica.servicioCartera().listarPosiciones(sesionId);
            BigDecimal patrimonioNeto = fabrica.servicioCartera().calcularPatrimonioNeto(sesionId);

            return new EstadoDashboard(sesion, filasActivos, posiciones, patrimonioNeto);
        });

        aplicarEstado(estado);
    }

    private FilaActivoDisponible filaDe(Activo activo, SesionInversion sesion, FabricaServicios fabrica) {
        BigDecimal precioActual = fabrica.servicioActivos()
                .obtenerUltimoPrecio(activo.getId(), sesion.getFechaActual())
                .map(HistorialPrecio::getValor)
                .orElse(null);
        return new FilaActivoDisponible(activo.getId(), activo.getNombre(), activo.getTicker(),
                activo.getTipo().toString(), precioActual);
    }

    private void aplicarEstado(EstadoDashboard estado) {
        etiquetaFecha.setText(estado.sesion().getFechaActual().toString());
        etiquetaSaldo.setText(formatearMoneda(estado.sesion().getSaldoEfectivoActual()));
        etiquetaPatrimonio.setText(formatearMoneda(estado.patrimonioNeto()));
        etiquetaEstado.setText(estado.sesion().getEstado().toString());

        tablaActivos.setItems(FXCollections.observableArrayList(estado.activos()));
        tablaCartera.setItems(FXCollections.observableArrayList(estado.posiciones()));
    }

    @FXML
    private void alAvanzar() {
        UnidadTiempo unidad = comboUnidadTiempo.getValue();
        int cantidad = spinnerCantidadTiempo.getValue();
        ejecutarOperacion(fabrica -> fabrica.servicioAvanceTiempo().avanzar(sesionId, unidad, cantidad));
    }

    @FXML
    private void alRetroceder() {
        UnidadTiempo unidad = comboUnidadTiempo.getValue();
        int cantidad = spinnerCantidadTiempo.getValue();
        ejecutarOperacion(fabrica -> fabrica.servicioAvanceTiempo().retroceder(sesionId, unidad, cantidad));
    }

    @FXML
    private void alPresionarPlayPausaContinuo() {
        if (avanceContinuo != null && avanceContinuo.getStatus() == Animation.Status.RUNNING) {
            pausarAvanceContinuo();
        } else {
            iniciarAvanceContinuo();
        }
    }

    private void iniciarAvanceContinuo() {
        detenerAvanceContinuoSiExiste();
        Duration intervalo = Duration.seconds(sliderVelocidad.getValue());
        avanceContinuo = new Timeline(new KeyFrame(intervalo, evento -> tickAvanceContinuo()));
        avanceContinuo.setCycleCount(Animation.INDEFINITE);
        avanceContinuo.play();
        botonPlayPausaContinuo.setText("Pausar avance automático");
    }

    private void tickAvanceContinuo() {
        boolean sigueActiva;
        try {
            sigueActiva = EjecutorTransaccional.ejecutar(fabrica -> {
                SesionInversion sesion = fabrica.servicioAvanceTiempo().avanzar(sesionId, UnidadTiempo.DIA, 1);
                return sesion.getEstado() == EstadoSesion.ACTIVA;
            });
        } catch (ExcepcionOperacionInvalida excepcion) {
            sigueActiva = false;
        }
        refrescar();
        if (!sigueActiva) {
            pausarAvanceContinuo();
        }
    }

    private void pausarAvanceContinuo() {
        if (avanceContinuo != null) {
            avanceContinuo.stop();
        }
        botonPlayPausaContinuo.setText("Iniciar avance automático");
    }

    private void detenerAvanceContinuoSiExiste() {
        if (avanceContinuo != null) {
            avanceContinuo.stop();
        }
    }

    @FXML
    private void alPausarSesion() {
        detenerAvanceContinuoSiExiste();
        EjecutorTransaccional.ejecutar(fabrica -> fabrica.servicioSesion().pausar(sesionId));
        volverAlInicio();
    }

    @FXML
    private void alFinalizarSesion() {
        detenerAvanceContinuoSiExiste();
        BigDecimal patrimonioFinal = EjecutorTransaccional.ejecutar(fabrica -> {
            fabrica.servicioSesion().finalizar(sesionId);
            return fabrica.servicioCartera().calcularPatrimonioNeto(sesionId);
        });

        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Sesión finalizada");
        alerta.setHeaderText("Resumen de la sesión");
        alerta.setContentText("Patrimonio neto final: " + formatearMoneda(patrimonioFinal));
        alerta.showAndWait();

        volverAlInicio();
    }

    @FXML
    private void alVolverInicio() {
        detenerAvanceContinuoSiExiste();
        volverAlInicio();
    }

    private void volverAlInicio() {
        ControladorPantallaInicio controlador = navegador.mostrar("/ui/pantalla-inicio.fxml");
        controlador.inicializar(navegador, usuarioId);
    }

    private void comprarActivo(FilaActivoDisponible fila) {
        if (fila.precioActual() == null) {
            mostrarError("No hay precio disponible para " + fila.nombre() + " en esta fecha");
            return;
        }
        pedirCantidad("Comprar " + fila.nombre()).ifPresent(cantidad ->
                ejecutarOperacion(fabrica -> fabrica.servicioTransacciones().comprar(sesionId, fila.activoId(), cantidad)));
    }

    private void venderActivo(FilaActivoDisponible fila) {
        pedirCantidad("Vender " + fila.nombre()).ifPresent(cantidad ->
                ejecutarOperacion(fabrica -> fabrica.servicioTransacciones().vender(sesionId, fila.activoId(), cantidad)));
    }

    private Optional<BigDecimal> pedirCantidad(String titulo) {
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.setContentText("Cantidad:");
        Optional<String> texto = dialogo.showAndWait();
        if (texto.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(texto.get().trim()));
        } catch (NumberFormatException excepcion) {
            mostrarError("La cantidad tiene que ser un número");
            return Optional.empty();
        }
    }

    private void ejecutarOperacion(Function<FabricaServicios, ?> operacion) {
        try {
            EjecutorTransaccional.ejecutar(operacion);
            refrescar();
        } catch (ExcepcionOperacionInvalida excepcion) {
            mostrarError(excepcion.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    private void actualizarEtiquetaVelocidad() {
        etiquetaVelocidad.setText(String.format(Locale.US, "%.1f s/día", sliderVelocidad.getValue()));
    }

    private String formatearMoneda(BigDecimal valor) {
        return valor == null ? "N/D" : "$ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private StringConverter<UnidadTiempo> convertidorUnidadTiempo() {
        return new StringConverter<>() {
            @Override
            public String toString(UnidadTiempo unidad) {
                if (unidad == null) {
                    return "";
                }
                return switch (unidad) {
                    case DIA -> "Día";
                    case SEMANA -> "Semana";
                    case MES -> "Mes";
                    case ANIO -> "Año";
                };
            }

            @Override
            public UnidadTiempo fromString(String texto) {
                throw new UnsupportedOperationException("El combo no es editable");
            }
        };
    }

    private record FilaActivoDisponible(Long activoId, String nombre, String ticker, String tipo,
                                         BigDecimal precioActual) {
    }

    private record EstadoDashboard(SesionInversion sesion, List<FilaActivoDisponible> activos,
                                    List<PosicionPortafolio> posiciones, BigDecimal patrimonioNeto) {
    }
}
