package com.businessanalyzer.model;

import java.time.LocalDateTime;

/**
 * Una fila del registro de accesos.
 *
 * @param nombreUsuarioFGEM usuario afectado (o el texto escrito, si el usuario no existe)
 * @param realizadoPorFGEM  usuario que hizo la accion, cuando fue un administrador; si no, {@code null}
 */
public record RegistroAccesoFGEM(long idFGEM, LocalDateTime fechaFGEM, String nombreUsuarioFGEM,
                             EventoAccesoFGEM eventoFGEM, String realizadoPorFGEM) {
}
