package com.simuladorinversiones.core.repositorios;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones CRUD comunes a todas las entidades, implementadas sobre un
 * {@link EntityManager}. El ciclo de vida del EntityManager (apertura,
 * transacción, cierre) lo maneja quien construye el repositorio — pensado
 * para ser gestionado por la capa de servicios en el próximo módulo.
 */
public abstract class RepositorioBase<T, ID> {

    protected final EntityManager entityManager;
    private final Class<T> claseEntidad;

    protected RepositorioBase(EntityManager entityManager, Class<T> claseEntidad) {
        this.entityManager = entityManager;
        this.claseEntidad = claseEntidad;
    }

    public T guardar(T entidad) {
        entityManager.persist(entidad);
        return entidad;
    }

    public T actualizar(T entidad) {
        return entityManager.merge(entidad);
    }

    public Optional<T> buscarPorId(ID id) {
        return Optional.ofNullable(entityManager.find(claseEntidad, id));
    }

    public List<T> buscarTodos() {
        String jpql = "SELECT e FROM " + claseEntidad.getSimpleName() + " e";
        return entityManager.createQuery(jpql, claseEntidad).getResultList();
    }

    public void eliminar(T entidad) {
        entityManager.remove(entityManager.contains(entidad) ? entidad : entityManager.merge(entidad));
    }
}
