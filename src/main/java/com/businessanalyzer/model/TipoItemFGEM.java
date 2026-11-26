package com.businessanalyzer.model;

public enum TipoItemFGEM {

    PRODUCTO("Producto", "P"),
    SERVICIO("Servicio", "S");

    private final String descripcionFGEM;
    private final String prefijoCodigoFGEM;

    TipoItemFGEM(String descripcionFGEM, String prefijoCodigoFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.prefijoCodigoFGEM = prefijoCodigoFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public String prefijoCodigoFGEM() {
        return prefijoCodigoFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
