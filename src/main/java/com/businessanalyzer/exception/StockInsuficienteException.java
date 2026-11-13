package com.businessanalyzer.exception;

import java.util.List;

/**
 * Una venta pide más unidades de las que hay en stock. La interfaz puede preguntar al usuario
 * si desea registrarla igualmente (stock negativo).
 */
public class StockInsuficienteException extends ValidacionException {

    private final List<String> faltantes;

    public StockInsuficienteException(List<String> faltantes) {
        super("Stock insuficiente para: " + String.join("; ", faltantes) + ".");
        this.faltantes = List.copyOf(faltantes);
    }

    public List<String> faltantes() {
        return faltantes;
    }
}
