package com.businessanalyzer.model;

/** Un ítem del catálogo es un bien físico (producto) o un servicio. */
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

    /** Prefijo de los códigos automáticos: P-0001, S-0001. */
    public String prefijoCodigoFGEM() {
        return prefijoCodigoFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
