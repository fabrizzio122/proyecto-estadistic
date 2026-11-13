package com.businessanalyzer.ui;

import com.businessanalyzer.exception.ValidacionException;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Selectores de fecha con formato dd/mm/aaaa, independiente de la configuración regional
 * de Windows. Acepta también fechas escritas sin ceros (5/3/2026).
 */
public final class CamposFecha {

    private static final DateTimeFormatter MOSTRAR = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter LEER = DateTimeFormatter.ofPattern("d/M/uuuu");

    private CamposFecha() {
    }

    public static DatePicker nuevo(LocalDate valor) {
        DatePicker selector = new DatePicker();
        configurar(selector);
        selector.setValue(valor);
        return selector;
    }

    public static void configurar(DatePicker selector) {
        selector.setPromptText("dd/mm/aaaa");
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return fecha == null ? "" : fecha.format(MOSTRAR);
            }

            @Override
            public LocalDate fromString(String texto) {
                return texto == null || texto.isBlank() ? null : LocalDate.parse(texto.strip(), LEER);
            }
        });
    }

    /**
     * Devuelve la fecha del selector, interpretando también lo que el usuario escribió sin
     * confirmar con Enter. {@code null} si está vacío.
     *
     * @throws ValidacionException si el texto no es una fecha válida
     */
    public static LocalDate leer(DatePicker selector, String campo) {
        String texto = selector.getEditor().getText();
        if (texto == null || texto.isBlank()) {
            selector.setValue(null);
            return null;
        }
        try {
            LocalDate fecha = selector.getConverter().fromString(texto);
            selector.setValue(fecha);
            return fecha;
        } catch (DateTimeParseException e) {
            throw new ValidacionException("La fecha de " + campo + " no es válida. Usa el formato dd/mm/aaaa.");
        }
    }
}
