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
final class SqlScriptFGEM {

    private SqlScriptFGEM() {
    }

    static List<String> dividirFGEM(String scriptFGEM) {
        StringBuilder sinComentariosFGEM = new StringBuilder();
        for (String lineaFGEM : scriptFGEM.split("\\R")) {
            if (!lineaFGEM.strip().startsWith("--")) {
                sinComentariosFGEM.append(lineaFGEM).append('\n');
            }
        }

        List<String> sentenciasFGEM = new ArrayList<>();
        for (String parteFGEM : sinComentariosFGEM.toString().split(";")) {
            String sentenciaFGEM = parteFGEM.strip();
            if (!sentenciaFGEM.isEmpty()) {
                sentenciasFGEM.add(sentenciaFGEM);
            }
        }
        return sentenciasFGEM;
    }
}
