package com.businessanalyzer.model;

/** Una operación comercial es una venta (sale mercadería) o una compra (entra mercadería). */
public enum TipoOperacion {

    VENTA("Venta", "Ventas", "V", "Cliente"),
    COMPRA("Compra", "Compras", "C", "Proveedor");

    private final String descripcion;
    private final String plural;
    private final String prefijoNumero;
    private final String contraparte;

    TipoOperacion(String descripcion, String plural, String prefijoNumero, String contraparte) {
        this.descripcion = descripcion;
        this.plural = plural;
        this.prefijoNumero = prefijoNumero;
        this.contraparte = contraparte;
    }

    public String descripcion() {
        return descripcion;
    }

    public String plural() {
        return plural;
    }

    /** Prefijo de la numeración automática: V-000001, C-000001. */
    public String prefijoNumero() {
        return prefijoNumero;
    }

    /** Cómo se llama la otra parte: cliente o proveedor. */
    public String contraparte() {
        return contraparte;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
