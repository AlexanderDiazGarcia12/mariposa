package com.mariposa.biblioteca.infraestructura.web.mapeadores;

import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.PrestamoRegistrado;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarPrestamoComando;
import com.mariposa.biblioteca.infraestructura.web.dto.prestamo.PrestamoRespuesta;
import com.mariposa.biblioteca.infraestructura.web.dto.prestamo.RegistrarPrestamoSolicitud;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MapeadorWebPrestamo {

    public RegistrarPrestamoComando aComando(UUID idUsuarioAutenticado, RegistrarPrestamoSolicitud solicitud) {
        return new RegistrarPrestamoComando(
                idUsuarioAutenticado,
                solicitud.idLibro(),
                solicitud.fechaPrestamo(),
                solicitud.fechaDevolucionEstimada()
        );
    }

    public PrestamoRespuesta aRespuesta(PrestamoRegistrado dominio) {
        return new PrestamoRespuesta(
                dominio.idPrestamo(),
                dominio.idUsuario(),
                dominio.idLibro(),
                dominio.fechaPrestamo(),
                dominio.fechaDevolucionEstimada(),
                dominio.estado(),
                dominio.registradoEn()
        );
    }
}
