package com.simuladorinversiones.ui;

import com.simuladorinversiones.config.EjecutorTransaccional;
import com.simuladorinversiones.core.entidades.EstadoSesion;
import com.simuladorinversiones.core.entidades.SesionInversion;
import com.simuladorinversiones.core.servicios.ExcepcionOperacionInvalida;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Pantalla inicial: arrancar una sesión nueva o retomar una guardada. */
public class ControladorPantallaInicio {

    @FXML
    private DatePicker selectorFechaInicio;
    @FXML
    private TextField campoCapitalInicial;
    @FXML
    private Label etiquetaErrorNuevaSesion;
    @FXML
    private TableView<SesionInversion> tablaSesionesGuardadas;
    @FXML
    private TableColumn<SesionInversion, LocalDate> columnaFechaInicio;
    @FXML
    private TableColumn<SesionInversion, LocalDate> columnaFechaActual;
    @FXML
    private TableColumn<SesionInversion, BigDecimal> columnaSaldo;
    @FXML
    private TableColumn<SesionInversion, EstadoSesion> columnaEstado;
    @FXML
    private Button botonRetomar;

    private NavegadorPantallas navegador;
    private Long usuarioId;

    public void inicializar(NavegadorPantallas navegador, Long usuarioId) {
        this.navegador = navegador;
        this.usuarioId = usuarioId;
        cargarSesionesGuardadas();
    }

    @FXML
    private void initialize() {
        columnaFechaInicio.setCellValueFactory(new PropertyValueFactory<>("fechaInicio"));
        columnaFechaActual.setCellValueFactory(new PropertyValueFactory<>("fechaActual"));
        columnaSaldo.setCellValueFactory(new PropertyValueFactory<>("saldoEfectivoActual"));
        columnaEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        botonRetomar.disableProperty().bind(
                tablaSesionesGuardadas.getSelectionModel().selectedItemProperty().isNull());
    }

    private void cargarSesionesGuardadas() {
        List<SesionInversion> sesiones = EjecutorTransaccional.ejecutar(
                fabrica -> fabrica.servicioSesion().listarPorUsuario(usuarioId));
        tablaSesionesGuardadas.setItems(FXCollections.observableArrayList(sesiones));
    }

    @FXML
    private void alComenzarNuevaSesion() {
        etiquetaErrorNuevaSesion.setText("");
        LocalDate fechaInicio = selectorFechaInicio.getValue();
        if (fechaInicio == null) {
            etiquetaErrorNuevaSesion.setText("Elegí una fecha de inicio");
            return;
        }

        BigDecimal capitalInicial;
        try {
            capitalInicial = new BigDecimal(campoCapitalInicial.getText().trim());
        } catch (NumberFormatException | NullPointerException excepcion) {
            etiquetaErrorNuevaSesion.setText("El capital inicial tiene que ser un número");
            return;
        }

        try {
            Long sesionId = EjecutorTransaccional.ejecutar(fabrica ->
                    fabrica.servicioSesion().crearSesion(usuarioId, fechaInicio, capitalInicial).getId());
            abrirDashboard(sesionId);
        } catch (ExcepcionOperacionInvalida excepcion) {
            etiquetaErrorNuevaSesion.setText(excepcion.getMessage());
        }
    }

    @FXML
    private void alRetomarSesion() {
        SesionInversion seleccionada = tablaSesionesGuardadas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            return;
        }
        if (seleccionada.getEstado() == EstadoSesion.PAUSADA) {
            EjecutorTransaccional.ejecutar(fabrica -> fabrica.servicioSesion().reanudar(seleccionada.getId()));
        }
        abrirDashboard(seleccionada.getId());
    }

    private void abrirDashboard(Long sesionId) {
        ControladorPantallaDashboard controlador = navegador.mostrar("/ui/pantalla-dashboard.fxml");
        controlador.inicializar(navegador, sesionId, usuarioId);
    }
}
