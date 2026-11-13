package com.businessanalyzer.model;

/** Un ítem del catálogo es un bien físico (producto) o un servicio. */
public enum TipoItem {

    PRODUCTO("Producto", "P"),
    SERVICIO("Servicio", "S");

    private final String descripcion;
    private final String prefijoCodigo;

    TipoItem(String descripcion, String prefijoCodigo) {
        this.descripcion = descripcion;
        this.prefijoCodigo = prefijoCodigo;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Prefijo de los códigos automáticos: P-0001, S-0001. */
    public String prefijoCodigo() {
        return prefijoCodigo;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
