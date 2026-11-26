package com.businessanalyzer.model;

import java.time.LocalDateTime;

public record RegistroAccesoFGEM(long idFGEM, LocalDateTime fechaFGEM, String nombreUsuarioFGEM,
                             EventoAccesoFGEM eventoFGEM, String realizadoPorFGEM) {
}
