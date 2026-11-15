package com.businessanalyzer.analytics.ganaderia;

import com.businessanalyzer.analytics.Periodo;
import com.businessanalyzer.analytics.diagnostico.Semaforo;
import com.businessanalyzer.model.CategoriaGanado;
import com.businessanalyzer.model.Especie;
import com.businessanalyzer.model.Moneda;
import com.businessanalyzer.model.MovimientoGanado;
import com.businessanalyzer.model.ProduccionGanadera;
import com.businessanalyzer.model.ProductoGanadero;
import com.businessanalyzer.model.TipoMovimientoGanado;
import com.businessanalyzer.util.Montos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Indicadores técnicos de una especie en un período:
 * <ul>
 *   <li><b>Natalidad</b> = nacimientos ÷ hembras reproductoras al inicio.</li>
 *   <li><b>Mortalidad</b> = muertes ÷ (existencia inicial + nacimientos + compras).</li>
 *   <li><b>Mortalidad de crías</b> = muertes de crías ÷ nacimientos.</li>
 *   <li><b>Extracción</b> = (ventas + consumo familiar) ÷ existencia inicial.</li>
 *   <li><b>Crecimiento del hato</b> = (existencia final − inicial) ÷ inicial.</li>
 * </ul>
 * Un inventario inicial registrado dentro del período cuenta como existencia inicial, no como compra.
 *
 * @param hembrasPorMacho hembras reproductoras por cada macho reproductor al final; {@code null} sin machos
 * @param historiaCompleta  ya había animales registrados antes de empezar el período (sus cifras son comparables)
 */
public record IndicadoresEspecie(Especie especie, Periodo periodo,
                                 int existenciaInicial, int existenciaFinal, int hembrasInicio, int hembrasFinal,
                                 int machosFinal, int nacimientos, int compras, int ventas, int muertes,
                                 int muertesCrias, int perdidas, int consumo, BigDecimal ingresoVentas,
                                 BigDecimal natalidad, BigDecimal mortalidad, BigDecimal mortalidadCrias,
                                 BigDecimal extraccion, BigDecimal crecimiento, BigDecimal hembrasPorMacho,
                                 List<ResumenProduccion> produccion, boolean historiaCompleta) {

    private static final int ESCALA = 6;

    /** Producción de leche, fibra o lana de la especie en el período. */
    public record ResumenProduccion(ProductoGanadero producto, BigDecimal cantidad, BigDecimal promedio,
                                    BigDecimal valor) {

        /** "6.4 l por vaca al día" o "2.3 kg por animal esquilado". */
        public String promedioTexto() {
            if (promedio == null) {
                return "—";
            }
            return Montos.cantidad(promedio.setScale(2, RoundingMode.HALF_UP)) + " " + producto.abreviatura()
                    + (producto.porDia() ? " por vaca al día" : " por animal esquilado");
        }
    }

    public static IndicadoresEspecie calcular(Especie especie, Periodo periodo, Collection<MovimientoGanado> movimientos,
                                              Collection<ProduccionGanadera> registrosProduccion) {
        List<MovimientoGanado> deEspecie = movimientos.stream()
                .filter(m -> !m.anulado() && m.especie() == especie && !m.fecha().isAfter(periodo.hasta()))
                .toList();
        List<MovimientoGanado> antes = deEspecie.stream().filter(m -> m.fecha().isBefore(periodo.desde())).toList();
        List<MovimientoGanado> durante = deEspecie.stream().filter(m -> !m.fecha().isBefore(periodo.desde())).toList();
        List<MovimientoGanado> inicialDurante = durante.stream()
                .filter(m -> m.tipo() == TipoMovimientoGanado.INVENTARIO_INICIAL).toList();

        List<MovimientoGanado> baseInicial = new ArrayList<>(antes);
        baseInicial.addAll(inicialDurante);
        Map<CategoriaGanado, Integer> inicio = Hato.existencias(baseInicial, periodo.hasta());
        Map<CategoriaGanado, Integer> fin = Hato.existencias(deEspecie, periodo.hasta());

        int inicial = total(inicio);
        int finalHato = total(fin);
        int hembrasInicio = porRol(inicio, CategoriaGanado.Rol.HEMBRA_REPRODUCTORA);
        int hembrasFinal = porRol(fin, CategoriaGanado.Rol.HEMBRA_REPRODUCTORA);
        int machosFinal = porRol(fin, CategoriaGanado.Rol.MACHO_REPRODUCTOR);

        int nacimientos = suma(durante, TipoMovimientoGanado.NACIMIENTO);
        int compras = suma(durante, TipoMovimientoGanado.COMPRA);
        int ventas = suma(durante, TipoMovimientoGanado.VENTA);
        int muertes = suma(durante, TipoMovimientoGanado.MUERTE);
        int perdidas = suma(durante, TipoMovimientoGanado.PERDIDA);
        int consumo = suma(durante, TipoMovimientoGanado.CONSUMO);
        int muertesCrias = durante.stream()
                .filter(m -> m.tipo() == TipoMovimientoGanado.MUERTE && m.categoria().cria())
                .mapToInt(MovimientoGanado::cantidad).sum();
        BigDecimal ingresoVentas = durante.stream()
                .filter(m -> m.tipo() == TipoMovimientoGanado.VENTA && m.monto() != null)
                .map(MovimientoGanado::monto).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ResumenProduccion> produccion = new ArrayList<>();
        for (ProductoGanadero producto : ProductoGanadero.de(especie)) {
            List<ProduccionGanadera> registros = registrosProduccion.stream()
                    .filter(p -> !p.anulado() && p.especie() == especie && p.producto() == producto
                            && !p.fecha().isBefore(periodo.desde()) && !p.fecha().isAfter(periodo.hasta()))
                    .toList();
            if (!registros.isEmpty()) {
                produccion.add(resumir(producto, registros));
            }
        }

        return new IndicadoresEspecie(especie, periodo, inicial, finalHato, hembrasInicio, hembrasFinal, machosFinal,
                nacimientos, compras, ventas, muertes, muertesCrias, perdidas, consumo, ingresoVentas,
                dividir(nacimientos, hembrasInicio),
                dividir(muertes, inicial + nacimientos + compras),
                dividir(muertesCrias, nacimientos),
                dividir(ventas + consumo, inicial),
                inicial == 0 ? null : dividir(finalHato - inicial, inicial),
                dividir(hembrasFinal, machosFinal),
                produccion, !Hato.existencias(antes, periodo.desde().minusDays(1)).isEmpty());
    }

    private static ResumenProduccion resumir(ProductoGanadero producto, List<ProduccionGanadera> registros) {
        BigDecimal cantidad = registros.stream().map(ProduccionGanadera::cantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
        // El promedio usa solo los registros que indican cuántos animales produjeron.
        BigDecimal conAnimales = BigDecimal.ZERO;
        long baseAnimales = 0;
        for (ProduccionGanadera p : registros) {
            if (p.animales() != null) {
                conAnimales = conAnimales.add(p.cantidad());
                baseAnimales += (long) p.animales() * (producto.porDia() ? p.dias() : 1);
            }
        }
        BigDecimal promedio = baseAnimales == 0 ? null
                : conAnimales.divide(BigDecimal.valueOf(baseAnimales), ESCALA, RoundingMode.HALF_UP);
        BigDecimal valor = registros.stream().map(ProduccionGanadera::valor).filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenProduccion(producto, cantidad, promedio, valor);
    }

    /** Hay datos de esta especie en el período (existencias o movimientos). */
    public boolean tieneDatos() {
        return existenciaInicial > 0 || existenciaFinal > 0 || nacimientos > 0 || compras > 0;
    }

    // ------------------------------------------------------------------ evaluación

    /** Indicadores con semáforo y lectura, en el orden en que se muestran. */
    public List<IndicadorGanadero> evaluar(Moneda moneda) {
        List<IndicadorGanadero> lista = new ArrayList<>();
        String hembras = especie.hembras();

        if (natalidad == null) {
            lista.add(new IndicadorGanadero("Natalidad", "—", Semaforo.SIN_DATOS, null,
                    "No había " + hembras + " al inicio del período para calcularla."));
        } else {
            lista.add(new IndicadorGanadero("Natalidad", Montos.porcentaje(natalidad),
                    mayorEsMejor(natalidad, especie.natalidadVerde(), especie.natalidadAmarillo()),
                    "Bien desde " + Montos.porcentaje(especie.natalidadVerde()) + "; riesgo por debajo de "
                            + Montos.porcentaje(especie.natalidadAmarillo()) + ".",
                    nacimientos + " crías de " + hembrasInicio + " " + hembras + "."));
        }

        if (mortalidad == null) {
            lista.add(new IndicadorGanadero("Mortalidad", "—", Semaforo.SIN_DATOS, null, "Sin animales en el período."));
        } else {
            lista.add(new IndicadorGanadero("Mortalidad", Montos.porcentaje(mortalidad),
                    menorEsMejor(mortalidad, especie.mortalidadVerde(), especie.mortalidadAmarillo()),
                    "Bien hasta " + Montos.porcentaje(especie.mortalidadVerde()) + "; riesgo sobre "
                            + Montos.porcentaje(especie.mortalidadAmarillo()) + ".",
                    muertes == 0 ? "No murió ningún animal." : muertes + (muertes == 1 ? " animal murió" : " animales murieron")
                            + " de " + (existenciaInicial + nacimientos + compras) + " que hubo en el período."));
        }

        if (mortalidadCrias != null) {
            lista.add(new IndicadorGanadero("Mortalidad de crías", Montos.porcentaje(mortalidadCrias),
                    menorEsMejor(mortalidadCrias, especie.mortalidadCriasVerde(), especie.mortalidadCriasAmarillo()),
                    "Bien hasta " + Montos.porcentaje(especie.mortalidadCriasVerde()) + "; riesgo sobre "
                            + Montos.porcentaje(especie.mortalidadCriasAmarillo()) + ".",
                    muertesCrias + " de " + nacimientos + " crías nacidas murieron."));
        }

        if (extraccion != null) {
            lista.add(new IndicadorGanadero("Extracción", Montos.porcentaje(extraccion), Semaforo.SIN_DATOS, null,
                    (ventas + consumo) + " animales vendidos o consumidos"
                            + (ingresoVentas.signum() > 0 ? "; ventas por " + Montos.formatear(ingresoVentas, moneda) : "")
                            + "."));
        }

        if (crecimiento != null) {
            lista.add(new IndicadorGanadero("Crecimiento del hato",
                    (crecimiento.signum() > 0 ? "+" : "") + Montos.porcentaje(crecimiento),
                    crecimiento.signum() >= 0 ? Semaforo.VERDE
                            : crecimiento.compareTo(new BigDecimal("-0.10")) >= 0 ? Semaforo.AMARILLO : Semaforo.ROJO,
                    "Bien si se mantiene o crece; riesgo si cae más de 10 %.",
                    "De " + existenciaInicial + " a " + existenciaFinal + " animales."));
        }

        if (perdidas > 0) {
            lista.add(new IndicadorGanadero("Pérdidas y robos", String.valueOf(perdidas), Semaforo.AMARILLO, null,
                    perdidas + (perdidas == 1 ? " animal perdido o robado" : " animales perdidos o robados")
                            + " en el período."));
        }

        if (hembrasFinal > 0) {
            lista.add(new IndicadorGanadero(capital(hembras) + " por reproductor",
                    hembrasPorMacho == null ? "Sin reproductor" : Montos.cantidad(hembrasPorMacho.setScale(1, RoundingMode.HALF_UP)),
                    Semaforo.SIN_DATOS, null,
                    hembrasPorMacho == null
                            ? "No hay machos reproductores registrados (puede usarse monta prestada o inseminación)."
                            : hembrasFinal + " " + hembras + " y " + machosFinal + (machosFinal == 1 ? " reproductor." : " reproductores.")));
        }

        for (ResumenProduccion p : produccion) {
            lista.add(new IndicadorGanadero("Producción de " + p.producto().descripcion().toLowerCase(),
                    p.promedioTexto(), Semaforo.SIN_DATOS, null,
                    "Total: " + Montos.cantidad(p.cantidad().setScale(1, RoundingMode.HALF_UP)) + " " + p.producto().unidad()
                            + (p.valor().signum() > 0 ? ", valorizado en " + Montos.formatear(p.valor(), moneda) : "") + "."));
        }
        return lista;
    }

    private static String capital(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static Semaforo mayorEsMejor(BigDecimal valor, BigDecimal verde, BigDecimal amarillo) {
        valor = mostrado(valor);
        if (valor.compareTo(verde) >= 0) {
            return Semaforo.VERDE;
        }
        return valor.compareTo(amarillo) >= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
    }

    private static Semaforo menorEsMejor(BigDecimal valor, BigDecimal verde, BigDecimal amarillo) {
        valor = mostrado(valor);
        if (valor.compareTo(verde) <= 0) {
            return Semaforo.VERDE;
        }
        return valor.compareTo(amarillo) <= 0 ? Semaforo.AMARILLO : Semaforo.ROJO;
    }

    /** El semáforo compara el porcentaje tal como se muestra (con un decimal), para que coincida con el criterio. */
    private static BigDecimal mostrado(BigDecimal fraccion) {
        return fraccion.setScale(3, RoundingMode.HALF_UP);
    }

    private static int total(Map<CategoriaGanado, Integer> existencias) {
        return existencias.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static int porRol(Map<CategoriaGanado, Integer> existencias, CategoriaGanado.Rol rol) {
        return existencias.entrySet().stream().filter(e -> e.getKey().rol() == rol)
                .mapToInt(Map.Entry::getValue).sum();
    }

    private static int suma(List<MovimientoGanado> movimientos, TipoMovimientoGanado tipo) {
        return movimientos.stream().filter(m -> m.tipo() == tipo).mapToInt(MovimientoGanado::cantidad).sum();
    }

    private static BigDecimal dividir(int a, int b) {
        return b == 0 ? null : BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), ESCALA, RoundingMode.HALF_UP);
    }
}
