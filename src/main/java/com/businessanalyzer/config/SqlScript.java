package com.businessanalyzer.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Divide un script SQL en sentencias individuales para ejecutarlas con JDBC.
 * <p>
 * Reglas: las lineas que empiezan con {@code --} son comentarios y se ignoran; cada
 * sentencia termina en {@code ;}. No usar {@code ;} dentro de textos ni triggers con
 * bloques BEGIN...END en los scripts de migracion.
 */
final class SqlScript {

    private SqlScript() {
    }

    static List<String> dividir(String script) {
        StringBuilder sinComentarios = new StringBuilder();
        for (String linea : script.split("\\R")) {
            if (!linea.strip().startsWith("--")) {
                sinComentarios.append(linea).append('\n');
            }
        }

        List<String> sentencias = new ArrayList<>();
        for (String parte : sinComentarios.toString().split(";")) {
            String sentencia = parte.strip();
            if (!sentencia.isEmpty()) {
                sentencias.add(sentencia);
            }
        }
        return sentencias;
    }
}
