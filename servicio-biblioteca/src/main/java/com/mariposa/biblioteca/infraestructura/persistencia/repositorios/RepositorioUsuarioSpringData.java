package com.mariposa.biblioteca.infraestructura.persistencia.repositorios;

import com.mariposa.biblioteca.infraestructura.persistencia.entidades.EntidadUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioUsuarioSpringData extends JpaRepository<EntidadUsuario, UUID> {

    Optional<EntidadUsuario> findByNombreUsuario(String nombreUsuario);

    Optional<EntidadUsuario> findByCorreoElectronico(String correoElectronico);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreoElectronico(String correoElectronico);
}
