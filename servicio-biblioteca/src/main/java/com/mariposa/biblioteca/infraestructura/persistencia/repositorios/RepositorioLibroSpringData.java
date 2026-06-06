package com.mariposa.biblioteca.infraestructura.persistencia.repositorios;

import com.mariposa.biblioteca.infraestructura.persistencia.entidades.EntidadLibro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioLibroSpringData
        extends JpaRepository<EntidadLibro, UUID>, JpaSpecificationExecutor<EntidadLibro> {

    Optional<EntidadLibro> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);
}
