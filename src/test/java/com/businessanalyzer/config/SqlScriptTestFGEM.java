package com.businessanalyzer.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlScriptTestFGEM {

    @Test
    void separaSentenciasEIgnoraComentariosFGEM() {
        String scriptFGEM = """
                -- comentario inicial
                CREATE TABLE a (id INTEGER);

                   -- comentario con sangria
                INSERT INTO a (id)
                VALUES (1);
                """;

        List<String> sentenciasFGEM = SqlScriptFGEM.dividirFGEM(scriptFGEM);

        assertEquals(2, sentenciasFGEM.size());
        assertEquals("CREATE TABLE a (id INTEGER)", sentenciasFGEM.get(0));
        assertTrue(sentenciasFGEM.get(1).startsWith("INSERT INTO a (id)"));
    }

    @Test
    void unScriptVacioNoTieneSentenciasFGEM() {
        assertTrue(SqlScriptFGEM.dividirFGEM("").isEmpty());
        assertTrue(SqlScriptFGEM.dividirFGEM("-- solo comentarios\n;\n").isEmpty());
    }

    @Test
    void aceptaFinesDeLineaDeWindowsFGEM() {
        List<String> sentenciasFGEM = SqlScriptFGEM.dividirFGEM("-- x\r\nSELECT 1;\r\nSELECT 2;\r\n");
        assertEquals(List.of("SELECT 1", "SELECT 2"), sentenciasFGEM);
    }
}
