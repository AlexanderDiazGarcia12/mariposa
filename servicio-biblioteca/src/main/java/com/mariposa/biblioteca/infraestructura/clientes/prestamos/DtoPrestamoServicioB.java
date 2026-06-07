package com.mariposa.biblioteca.infraestructura.clientes.prestamos;

public final class DtoPrestamoServicioB {

    private DtoPrestamoServicioB() {
    }

    public record RegistrarPrestamoSolicitudB(
            String idUsuario,
            String idLibro,
            String fechaPrestamo,
            String fechaDevolucionEstimada
    ) {
    }

    public record PrestamoRespuestaB(
            String id,
            String idUsuario,
            String idLibro,
            String fechaPrestamo,
            String fechaDevolucionEstimada,
            String fechaDevolucionReal,
            String estado,
            boolean estaAtrasado,
            String fechaCreacion,
            String fechaActualizacion
    ) {
    }

    public record ProblemDetailB(
            String type,
            String title,
            int status,
            String detail,
            String instance
    ) {
    }
}
