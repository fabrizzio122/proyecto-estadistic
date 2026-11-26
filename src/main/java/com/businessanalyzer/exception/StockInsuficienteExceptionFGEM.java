package com.businessanalyzer.exception;

import java.util.List;

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
