package com.businessanalyzer.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlScriptTest {

    @Test
    void separaSentenciasEIgnoraComentarios() {
        String script = """
                -- comentario inicial
                CREATE TABLE a (id INTEGER);

                   -- comentario con sangria
                INSERT INTO a (id)
                VALUES (1);
                """;

        List<String> sentencias = SqlScript.dividir(script);

        assertEquals(2, sentencias.size());
        assertEquals("CREATE TABLE a (id INTEGER)", sentencias.get(0));
        assertTrue(sentencias.get(1).startsWith("INSERT INTO a (id)"));
    }

    @Test
    void unScriptVacioNoTieneSentencias() {
        assertTrue(SqlScript.dividir("").isEmpty());
        assertTrue(SqlScript.dividir("-- solo comentarios\n;\n").isEmpty());
    }

    @Test
    void aceptaFinesDeLineaDeWindows() {
        List<String> sentencias = SqlScript.dividir("-- x\r\nSELECT 1;\r\nSELECT 2;\r\n");
        assertEquals(List.of("SELECT 1", "SELECT 2"), sentencias);
    }
}
