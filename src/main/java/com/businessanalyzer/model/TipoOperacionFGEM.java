package com.businessanalyzer.model;

/** Una operación comercial es una venta (sale mercadería) o una compra (entra mercadería). */
public enum TipoOperacionFGEM {

    VENTA("Venta", "Ventas", "V", "Cliente"),
    COMPRA("Compra", "Compras", "C", "Proveedor");

    private final String descripcionFGEM;
    private final String pluralFGEM;
    private final String prefijoNumeroFGEM;
    private final String contraparteFGEM;

    TipoOperacionFGEM(String descripcionFGEM, String pluralFGEM, String prefijoNumeroFGEM, String contraparteFGEM) {
        this.descripcionFGEM = descripcionFGEM;
        this.pluralFGEM = pluralFGEM;
        this.prefijoNumeroFGEM = prefijoNumeroFGEM;
        this.contraparteFGEM = contraparteFGEM;
    }

    public String descripcionFGEM() {
        return descripcionFGEM;
    }

    public String pluralFGEM() {
        return pluralFGEM;
    }

    /** Prefijo de la numeración automática: V-000001, C-000001. */
    public String prefijoNumeroFGEM() {
        return prefijoNumeroFGEM;
    }

    /** Cómo se llama la otra parte: cliente o proveedor. */
    public String contraparteFGEM() {
        return contraparteFGEM;
    }

    @Override
    public String toString() {
        return descripcionFGEM;
    }
}
