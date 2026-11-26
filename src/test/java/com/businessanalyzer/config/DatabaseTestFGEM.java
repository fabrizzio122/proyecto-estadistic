package com.businessanalyzer.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseTestFGEM {

    @TempDir
    Path carpetaFGEM;

    private DatabaseFGEM databaseFGEM;

    @BeforeEach
    void prepararFGEM() {
        databaseFGEM = new DatabaseFGEM(carpetaFGEM.resolve("prueba.db"));
        databaseFGEM.conConexionFGEM(cFGEM -> {
            try (Statement sFGEM = cFGEM.createStatement()) {
                sFGEM.execute("CREATE TABLE dato (valor INTEGER)");
            }
            return null;
        });
    }

    @Test
    void lasTransaccionesAnidadasFormanParteDeLaExteriorFGEM() {
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            insertarFGEM(1);
            insertarFGEM(2);
            assertEquals(2, contarFGEM());
            return null;
        });
        assertEquals(2, contarFGEM());
    }

    @Test
    void siLaTransaccionExteriorFallaSeRevierteTambienLoAnidadoFGEM() {
        assertThrows(IllegalStateException.class, () -> databaseFGEM.enTransaccionFGEM(cFGEM -> {
            insertarFGEM(1);
            throw new IllegalStateException("falla después de la escritura anidada");
        }));
        assertEquals(0, contarFGEM());
        insertarFGEM(3);
        assertEquals(1, contarFGEM());
    }

    private void insertarFGEM(int valorFGEM) {
        databaseFGEM.enTransaccionFGEM(cFGEM -> {
            try (Statement sFGEM = cFGEM.createStatement()) {
                sFGEM.executeUpdate("INSERT INTO dato (valor) VALUES (" + valorFGEM + ")");
            }
            return null;
        });
    }

    private int contarFGEM() {
        return databaseFGEM.conConexionFGEM(cFGEM -> {
            try (Statement sFGEM = cFGEM.createStatement(); ResultSet rFGEM = sFGEM.executeQuery("SELECT COUNT(*) FROM dato")) {
                return rFGEM.next() ? rFGEM.getInt(1) : 0;
            }
        });
    }
}
