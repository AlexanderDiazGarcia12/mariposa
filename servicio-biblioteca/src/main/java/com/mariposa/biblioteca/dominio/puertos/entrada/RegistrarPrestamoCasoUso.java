package com.mariposa.biblioteca.dominio.puertos.entrada;

import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.PrestamoRegistrado;
import com.mariposa.biblioteca.dominio.puertos.entrada.comandos.RegistrarPrestamoComando;

public interface RegistrarPrestamoCasoUso {

    PrestamoRegistrado registrar(RegistrarPrestamoComando comando);
}
