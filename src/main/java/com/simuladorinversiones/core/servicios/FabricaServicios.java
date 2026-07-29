package com.simuladorinversiones.core.servicios;

import com.simuladorinversiones.core.datos.ImportadorDatosHistoricos;
import com.simuladorinversiones.core.entidades.Usuario;
import com.simuladorinversiones.core.repositorios.RepositorioActivo;
import com.simuladorinversiones.core.repositorios.RepositorioCarteraUsuario;
import com.simuladorinversiones.core.repositorios.RepositorioHistorialPrecio;
import com.simuladorinversiones.core.repositorios.RepositorioPosicionCartera;
import com.simuladorinversiones.core.repositorios.RepositorioSesionInversion;
import com.simuladorinversiones.core.repositorios.RepositorioTransaccion;
import com.simuladorinversiones.core.repositorios.RepositorioUsuario;
import jakarta.persistence.EntityManager;

import java.util.List;

/** Arma los servicios de negocio compartiendo un mismo EntityManager (y por lo tanto una misma transacción). */
public class FabricaServicios {

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioSesionInversion repositorioSesion;
    private final RepositorioActivo repositorioActivo;
    private final RepositorioHistorialPrecio repositorioHistorialPrecio;
    private final RepositorioCarteraUsuario repositorioCartera;
    private final RepositorioPosicionCartera repositorioPosicion;
    private final RepositorioTransaccion repositorioTransaccion;

    public FabricaServicios(EntityManager entityManager) {
        this.repositorioUsuario = new RepositorioUsuario(entityManager);
        this.repositorioSesion = new RepositorioSesionInversion(entityManager);
        this.repositorioActivo = new RepositorioActivo(entityManager);
        this.repositorioHistorialPrecio = new RepositorioHistorialPrecio(entityManager);
        this.repositorioCartera = new RepositorioCarteraUsuario(entityManager);
        this.repositorioPosicion = new RepositorioPosicionCartera(entityManager);
        this.repositorioTransaccion = new RepositorioTransaccion(entityManager);
    }

    public ServicioSesionInversion servicioSesion() {
        return new ServicioSesionInversion(repositorioSesion, repositorioUsuario, repositorioCartera);
    }

    public ServicioActivos servicioActivos() {
        return new ServicioActivos(repositorioActivo, repositorioHistorialPrecio);
    }

    public ServicioTransacciones servicioTransacciones() {
        return new ServicioTransacciones(repositorioSesion, repositorioActivo, repositorioCartera,
                repositorioPosicion, repositorioTransaccion, repositorioHistorialPrecio);
    }

    public ServicioAvanceTiempo servicioAvanceTiempo() {
        return new ServicioAvanceTiempo(repositorioSesion);
    }

    public ServicioCartera servicioCartera() {
        return new ServicioCartera(repositorioSesion, repositorioCartera, repositorioPosicion, repositorioHistorialPrecio);
    }

    public ImportadorDatosHistoricos servicioImportacion() {
        return new ImportadorDatosHistoricos(repositorioActivo, repositorioHistorialPrecio);
    }

    /** La app es de un solo usuario sin autenticación: se reutiliza el único Usuario, o se crea al primer arranque. */
    public Usuario obtenerOCrearUsuarioUnico() {
        List<Usuario> usuarios = repositorioUsuario.buscarTodos();
        if (!usuarios.isEmpty()) {
            return usuarios.get(0);
        }
        return repositorioUsuario.guardar(new Usuario());
    }
}
