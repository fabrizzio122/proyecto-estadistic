package com.businessanalyzer.exception;

import java.util.List;

/**
 * Una venta pide más unidades de las que hay en stock. La interfaz puede preguntar al usuario
 * si desea registrarla igualmente (stock negativo).
 */
public class StockInsuficienteExceptionFGEM extends ValidacionExceptionFGEM {

    private final List<String> faltantesFGEM;

    public StockInsuficienteExceptionFGEM(List<String> faltantesFGEM) {
        super("Stock insuficiente para: " + String.join("; ", faltantesFGEM) + ".");
        this.faltantesFGEM = List.copyOf(faltantesFGEM);
    }

    public List<String> faltantesFGEM() {
        return faltantesFGEM;
    }
}
