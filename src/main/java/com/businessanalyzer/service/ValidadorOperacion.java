package com.businessanalyzer.service;

import com.businessanalyzer.exception.ValidacionException;
import com.businessanalyzer.model.DatosOperacion;
import com.businessanalyzer.model.DatosOperacion.DatosLinea;
import com.businessanalyzer.util.Validaciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Valida y normaliza una venta o compra antes de registrarla. */
final class ValidadorOperacion {

    static final int MAXIMO_LINEAS = 500;
    private static final BigDecimal CANTIDAD_MAXIMA = new BigDecimal("10000000");
    private static final BigDecimal TASA_MAXIMA = new BigDecimal("100");
    private static final LocalDate FECHA_MINIMA = LocalDate.of(2000, 1, 1);

    private ValidadorOperacion() {
    }

    static DatosOperacion normalizar(DatosOperacion d, LocalDate hoy) {
        if (d == null || d.tipo() == null) {
            throw new ValidacionException("Faltan los datos de la operación.");
        }
        if (d.fecha() == null) {
            throw new ValidacionException("Indica la fecha de la " + d.tipo().descripcion().toLowerCase() + ".");
        }
        if (d.fecha().isAfter(hoy)) {
            throw new ValidacionException("La fecha no puede ser futura.");
        }
        if (d.fecha().isBefore(FECHA_MINIMA)) {
            throw new ValidacionException("La fecha no puede ser anterior al año 2000.");
        }
        String contraparte = Validaciones.opcional(d.contraparte(), d.tipo().contraparte(), 120);
        String documento = Validaciones.opcional(d.documentoContraparte(), "Documento", 20);
        String comprobante = Validaciones.opcional(d.comprobante(), "Comprobante", 40);
        String observaciones = Validaciones.opcional(d.observaciones(), "Observaciones", 500);

        BigDecimal tasa = d.tasaImpuesto() == null ? BigDecimal.ZERO : d.tasaImpuesto();
        if (tasa.signum() < 0 || tasa.compareTo(TASA_MAXIMA) > 0) {
            throw new ValidacionException("El impuesto debe estar entre 0 % y 100 %.");
        }
        if (tasa.stripTrailingZeros().scale() > 2) {
            throw new ValidacionException("El impuesto admite como máximo 2 decimales.");
        }

        if (d.lineas() == null || d.lineas().isEmpty()) {
            throw new ValidacionException("Agrega al menos un producto o servicio.");
        }
        if (d.lineas().size() > MAXIMO_LINEAS) {
            throw new ValidacionException("Una operación admite como máximo " + MAXIMO_LINEAS + " líneas.");
        }
        Set<Long> productos = new HashSet<>();
        List<DatosLinea> lineas = new ArrayList<>();
        for (DatosLinea linea : d.lineas()) {
            if (!productos.add(linea.productoId())) {
                throw new ValidacionException("Un mismo producto aparece en dos líneas. Suma las cantidades en una sola.");
            }
            BigDecimal cantidad = linea.cantidad();
            if (cantidad == null || cantidad.signum() <= 0) {
                throw new ValidacionException("Las cantidades deben ser mayores que cero.");
            }
            if (cantidad.compareTo(CANTIDAD_MAXIMA) > 0 || cantidad.stripTrailingZeros().scale() > 4) {
                throw new ValidacionException("Cantidad no válida: máximo 10 000 000 y 4 decimales.");
            }
            BigDecimal precio = ValidadorCatalogo.monto(linea.precioUnitario(), "Precio unitario");
            lineas.add(new DatosLinea(linea.productoId(), cantidad, precio));
        }

        return new DatosOperacion(d.tipo(), d.fecha(), contraparte, documento, comprobante, tasa, observaciones,
                lineas, d.actualizarCostos());
    }
}
