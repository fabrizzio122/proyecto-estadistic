package com.businessanalyzer.model;

import java.time.LocalDateTime;

/**
 * Una fila del registro de accesos.
 *
 * @param nombreUsuario usuario afectado (o el texto escrito, si el usuario no existe)
 * @param realizadoPor  usuario que hizo la accion, cuando fue un administrador; si no, {@code null}
 */
public record RegistroAcceso(long id, LocalDateTime fecha, String nombreUsuario,
                             EventoAcceso evento, String realizadoPor) {
}
