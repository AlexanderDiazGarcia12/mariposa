package com.mariposa.biblioteca.dominio.puertos.salida;

import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.PrestamoRegistrado;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarPrestamoComando;

public interface ClientePrestamos {

    PrestamoRegistrado registrar(RegistrarPrestamoComando comando);
}
