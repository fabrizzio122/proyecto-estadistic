package com.businessanalyzer.config;

import java.util.ArrayList;
import java.util.List;

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
