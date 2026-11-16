package com.businessanalyzer.ui;

import com.businessanalyzer.exception.ValidacionExceptionFGEM;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Selectores de fecha con formato dd/mm/aaaa, independiente de la configuración regional
 * de Windows. Acepta también fechas escritas sin ceros (5/3/2026).
 */
public final class CamposFechaFGEM {

    private static final DateTimeFormatter MOSTRAR_FGEM = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter LEER_FGEM = DateTimeFormatter.ofPattern("d/M/uuuu");

    private CamposFechaFGEM() {
    }

    public static DatePicker nuevoFGEM(LocalDate valorFGEM) {
        DatePicker selectorFGEM = new DatePicker();
        configurarFGEM(selectorFGEM);
        selectorFGEM.setValue(valorFGEM);
        return selectorFGEM;
    }

    public static void configurarFGEM(DatePicker selectorFGEM) {
        selectorFGEM.setPromptText("dd/mm/aaaa");
        selectorFGEM.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fechaFGEM) {
                return fechaFGEM == null ? "" : fechaFGEM.format(MOSTRAR_FGEM);
            }

            @Override
            public LocalDate fromString(String textoFGEM) {
                return textoFGEM == null || textoFGEM.isBlank() ? null : LocalDate.parse(textoFGEM.strip(), LEER_FGEM);
            }
        });
    }

    /**
     * Devuelve la fecha del selector, interpretando también lo que el usuario escribió sin
     * confirmar con Enter. {@code null} si está vacío.
     *
     * @throws ValidacionException si el texto no es una fecha válida
     */
    public static LocalDate leerFGEM(DatePicker selectorFGEM, String campoFGEM) {
        String textoFGEM = selectorFGEM.getEditor().getText();
        if (textoFGEM == null || textoFGEM.isBlank()) {
            selectorFGEM.setValue(null);
            return null;
        }
        try {
            LocalDate fechaFGEM = selectorFGEM.getConverter().fromString(textoFGEM);
            selectorFGEM.setValue(fechaFGEM);
            return fechaFGEM;
        } catch (DateTimeParseException eFGEM) {
            throw new ValidacionExceptionFGEM("La fecha de " + campoFGEM + " no es válida. Usa el formato dd/mm/aaaa.");
        }
    }
}
