# Business Analyzer Desktop 1.0

Aplicación de escritorio para analizar la situación económica y financiera de negocios y
empresas: rentabilidad, punto de equilibrio, tendencias, predicciones, escenarios y diagnóstico.

## Requisitos

- JDK 17 (en IntelliJ: *File → Project Structure → SDK*).
- No hace falta instalar Maven: el proyecto incluye el wrapper (`mvnw` / `mvnw.cmd`).

## Ejecutar

```powershell
.\mvnw.cmd javafx:run      # arrancar la aplicación
.\mvnw.cmd test            # ejecutar las pruebas
```

En IntelliJ IDEA: abrir la carpeta como proyecto Maven y ejecutar
`com.businessanalyzer.Launcher` (o el goal Maven `javafx:run`).

## Datos locales

| Qué | Dónde |
|---|---|
| Base de datos SQLite | `%USERPROFILE%\BusinessAnalyzer\business-analyzer.db` |
| Registros (logs) | `%USERPROFILE%\BusinessAnalyzer\logs\` |

Se puede usar otra carpeta con `-Dba.data.dir=C:\ruta`.

## Arquitectura

```
com.businessanalyzer
├── Launcher, BusinessAnalyzerApp   arranque y ciclo de vida JavaFX
├── config        conexión SQLite (JDBC), migraciones, contexto de dependencias
├── controller    controladores de las vistas FXML (sin SQL ni cálculos)
├── service       lógica de negocio y validaciones
├── repository    acceso a datos con SQL parametrizado
├── model         entidades: Usuario, Rol, Permiso, Empresa...
├── seguridad     hash de contraseñas (PBKDF2), política de contraseñas y sesión
├── analytics     cálculos de negocio: márgenes y resumen del catálogo (base del motor financiero)
├── util          montos (BigDecimal), fechas y validaciones
├── exception     errores de datos y de validación
└── ui            navegación, notificaciones, diálogos y componentes visuales
```

El esquema de la base de datos se versiona con scripts en
`src/main/resources/db/migration` (`V1__...sql`, `V2__...sql`...), que se aplican
automáticamente al iniciar y dentro de una transacción.

## Fases

1. Base: estructura, SQLite y migraciones, ventana principal y tema oscuro ✅
2. Usuarios, roles, permisos e inicio de sesión ✅
3. Empresas, acceso por empresa y empresa activa ✅
4. Productos y servicios: catálogo, categorías, márgenes e historial de precios ✅
5. Ventas y compras: operaciones con detalle, impuestos, stock y anulación ✅
6. Gestión financiera: ingresos y gastos, costos fijos y variables ✅
7. Análisis financiero: estado de resultados, márgenes, punto de equilibrio y rentabilidad ✅
8. Análisis estadístico: descriptiva, tendencia, estacionalidad, ABC y correlaciones ✅
9. Predicciones: proyección de ventas y utilidad con selección automática del método ✅
10. Simulación de escenarios: qué pasaría si…, sensibilidad, metas y comparación ✅
11. Diagnóstico empresarial: puntaje de salud, semáforo de 14 indicadores y recomendaciones ✅
12. Informes (PDF y Excel): JasperReports y Apache POI ✅
13. Configuración, copias de seguridad e instalador ✅
14. Activos, deudas y patrimonio: balance general, depreciación, liquidez y endeudamiento ✅
15. Ganadería (sector agropecuario): hato, movimientos, producción de leche y fibra, indicadores técnicos y valor del hato ✅
16. Agricultura (sector agropecuario): parcelas, campañas, costos, cosechas, rendimiento y margen por campaña ✅

## Seguridad

- **Primer arranque:** el sistema pide crear la cuenta de administrador.
- **Contraseñas:** se guardan con PBKDF2-HMAC-SHA256 (600.000 iteraciones y sal aleatoria). Nunca se guardan en texto plano.
- **Bloqueo:** 5 intentos fallidos bloquean la cuenta durante 15 minutos.
- **Roles:** Administrador, Gerente, Analista y Operador. Los permisos se comprueban en los servicios, no solo en la interfaz.
- **Registro de accesos:** inicios de sesión, fallos, bloqueos y cambios en las cuentas quedan auditados.

## Empresas

- Cada usuario trabaja sobre una **empresa activa**, que elige en el selector de la barra superior. El sistema recuerda la última que usó.
- Los **administradores** ven todas las empresas. El resto de usuarios ve solo las que tiene asignadas (desde *Acciones → Usuarios con acceso*). Quien registra una empresa queda asignado a ella.
- Las empresas no se eliminan: se **archivan** para conservar su historial y se pueden reactivar.

## Montos de dinero

- En Java se usa siempre `BigDecimal`, nunca `double`.
- En SQLite se guardan como enteros en diezmilésimas (4 decimales): `12.50` se guarda como `125000`. Así los montos son exactos y se pueden sumar directamente en SQL.
- Los precios del catálogo se registran sin impuestos.

## Ventas, compras y stock

- Cada venta o compra se guarda en **una sola transacción**: cabecera, líneas y, en compras, la actualización de costos. O se guarda todo o nada.
- **Numeración** automática por empresa: V-000001 para ventas y C-000001 para compras.
- **Stock** = compras − ventas registradas. Se calcula; no se guarda aparte, así nunca se desincroniza.
- Cada venta guarda el **costo del producto en ese momento**, así el margen histórico no cambia aunque el costo cambie después.
- Si una venta supera el stock, el sistema **avisa y pide confirmación** (el stock puede quedar negativo).
- Las operaciones no se borran: se **anulan** con un motivo y dejan de contar en el stock y los totales.

## Gestión financiera

- Ingresos y gastos que **no** son ventas ni compras de mercadería: alquiler, sueldos, servicios, publicidad, intereses, etc.
- Cada gasto es **fijo** (no cambia con las ventas) o **variable** (sube o baja con ellas). La categoría lo sugiere y el usuario puede cambiarlo. Es la base del punto de equilibrio.
- Cada categoría pertenece a un **grupo** del estado de resultados: gasto operativo, gasto financiero, impuesto a la renta, ingreso financiero u otro ingreso.
- **Copiar gastos fijos del mes anterior** los repite en el mes actual sin duplicarlos.
- Se aceptan fechas hasta el fin del mes en curso, para registrar por adelantado la planilla y el alquiler.

## Activos, deudas y patrimonio

- **Activos:** caja y bancos, cuentas por cobrar y otros corrientes (se registra el saldo y se actualiza cuando cambia); terrenos, edificios, maquinaria, vehículos, muebles y equipos de cómputo (se registra el valor de adquisición).
- **Depreciación:** en línea recta, por meses completos, según la vida útil y el valor residual. Los terrenos no se deprecian. Una vida útil vacía significa que el bien no se deprecia.
- **Inventario:** no se registra a mano. Se calcula con el stock de cada producto por su costo unitario.
- **Deudas:** monto, acreedor, tasa anual, cuota mensual y vencimiento (estos tres son opcionales). Los **pagos de capital** reducen el saldo; los intereses se registran como gasto en Gestión financiera. Con saldo cero la deuda queda «Cancelada».
- **Corto y largo plazo:** sin vencimiento, o si vence dentro de 12 meses, toda la deuda es de corto plazo. Si vence después, se considera de corto plazo la parte proporcional a 12 meses.
- **Balance general** al día de hoy: activo corriente y no corriente, pasivo corriente y no corriente, y patrimonio (activo − pasivo).
- **Indicadores:** liquidez corriente, prueba ácida, capital de trabajo, endeudamiento, deuda por cada 1 de patrimonio, rentabilidad del patrimonio y de los activos (con la utilidad de los últimos 12 meses llevada a un año) y cobertura de cuotas (utilidad operativa mensual ÷ cuotas).
- **Nada se elimina:** los activos se dan de baja con fecha y motivo; las deudas y los pagos se anulan.
- **Permisos:** los mismos de Gestión financiera (ver y registrar).

## Ganadería (empresas agropecuarias)

- Se activa al elegir el sector **«Agropecuario»** en la empresa: aparece la sección «Agropecuario» del menú. En las demás empresas no se muestra.
- **Especies y categorías:** vacunos (vacas, vaquillonas, terneras, terneros, toretes, toros), ovinos (ovejas, borregas, corderos, borregos, carneros), alpacas y llamas (madres, tuis hembras y machos, crías, padres, capones).
- **Las existencias se calculan con los movimientos:** inventario inicial, nacimiento (solo en crías), compra, venta, muerte, pérdida o robo, consumo familiar y cambio de categoría. Nunca pueden quedar negativas: un movimiento que deje sin animales una categoría en alguna fecha se rechaza.
- **Producción:** leche (litros, por vaca en ordeño y por día), fibra de alpaca y llama y lana de ovino (kilos por animal esquilado). Se puede registrar por día o el total del mes.
- **Indicadores por especie y período:** natalidad, mortalidad, mortalidad de crías, extracción, crecimiento del hato, hembras por reproductor y producción promedio, con semáforo según referencias de la ganadería familiar y mediana del Perú.
- **Valor del hato:** cabezas × precio de mercado por cabeza; se suma al balance general como «Ganado (valor de mercado)».
- **Permisos nuevos:** «Ver ganadería y agricultura» (todos los roles) y «Registrar ganado…» (administrador, gerente y operador, que es el rol pensado para el productor).
- **Demostración:** Empresas → Empresa de demostración → «Fundo ganadero (agropecuario)».

## Agricultura (empresas agropecuarias)

- **Parcelas:** nombre, hectáreas, tenencia (propia, alquilada, comunal, al partir) y si tiene riego. Se desactivan en lugar de borrarse.
- **Campañas:** un cultivo en una parcela (papa, maíz, quinua, cebada, trigo, habas, arveja, frijol, olluco, oca, avena forrajera, alfalfa u otro), con variedad, fecha de siembra, área y un **rendimiento de referencia** (se propone el promedio nacional aproximado y se puede ajustar a la zona).
- **Costos por rubro:** preparación del terreno, semilla, fertilizantes, abono orgánico, control de plagas, mano de obra, riego, cosecha, transporte, alquiler del terreno y otros. Se aceptan hasta 6 meses antes de la siembra.
- **Cosechas por destino:** venta (con precio por kilo), consumo familiar, semilla, alimento para animales, trueque y pérdida poscosecha.
- **Cierre:** la campaña se cierra como **cosechada** (requiere al menos una cosecha) o **perdida** (helada, granizada, sequía…, con la causa). Se puede reabrir para corregir.
- **Resultado de cada campaña:** rendimiento (kg/ha) frente a la referencia con semáforo, costo por hectárea y por kilo, rendimiento mínimo para no perder, valor de la producción, margen y destino de la cosecha. Lo no vendido se valoriza al precio en chacra (o al precio promedio de venta).
- **Balance:** lo invertido en las campañas en curso entra al activo corriente como «Cultivos en proceso (al costo)».

## Análisis financiero

- **Estado de resultados** del período elegido (ventas − costo de ventas − gastos operativos ± resultados financieros − impuesto a la renta), con el % sobre las ventas y la comparación con el período anterior equivalente.
- **Márgenes** bruto, operativo y neto, con su variación en puntos porcentuales.
- **Punto de equilibrio** en ventas: costos fijos ÷ razón de contribución. Incluye el margen de seguridad y un gráfico de ingresos frente a costos.
- **Rentabilidad por producto**: unidades, ventas, margen y aporte al margen total. Usa el costo vigente en cada venta.
- **Evolución de los últimos 12 meses** y una **lectura rápida** en lenguaje sencillo.
- Gráficos con **JFreeChart**. Con clic derecho sobre un gráfico se puede guardarlo como imagen.
- Los ratios de liquidez y endeudamiento se ven en el módulo «Activos y deudas» (Fase 14).

## Análisis estadístico

Se calcula con **Apache Commons Math**:

- **Descriptiva** del monto por venta y de las ventas diarias: media, mediana, desviación estándar, coeficiente de variación, cuartiles y asimetría. Incluye un histograma de frecuencias (regla de Sturges).
- **Tendencia**: regresión lineal de las ventas mensuales con pendiente, crecimiento mensual, R² y valor p. Solo se considera tendencia real si p < 0.05. Se usan solo meses completos: el mes en curso se excluye.
- **Días de la semana**: venta promedio e índice estacional de cada día (se cuentan los días sin ventas).
- **Análisis ABC (Pareto)**: A = primer 80 % de las ventas, B = hasta 95 %, C = resto.
- **Correlaciones de Pearson** entre ventas, margen bruto %, gastos operativos, publicidad y utilidad neta (mensuales), con su valor p.
- Cada pestaña incluye una **interpretación en lenguaje sencillo**.

## Predicciones

- Proyecta las **ventas netas** y la **utilidad neta** de los próximos 3, 6 o 12 meses.
- Compara tres métodos: **tendencia lineal**, **promedio móvil de 3 meses** y **suavizado exponencial de Holt** (α y β se ajustan automáticamente).
- **Validación histórica:** cada método pronostica cada mes pasado usando solo los meses anteriores. Se elige el de menor error medio.
- **Rango probable al 95 %:** pronóstico ± 1.96 × error histórico × √(meses de distancia).
- **Confiabilidad** alta, media o baja según el error típico (menos de 10 %, hasta 25 %, más).
- Usa solo meses completos (hasta 24) y necesita al menos 4. Holt-Winters con estacionalidad anual requiere 24 meses de historia y aún no se incluye.

## Simulación de escenarios

- **Base:** resultados reales del último mes completo o el promedio mensual de los últimos 3 o 6 meses, separados en ventas, costo de ventas, gastos variables, costos fijos y otros ingresos.
- **Supuestos:** cambios % en precio, unidades vendidas, costo de los productos, gastos variables y costos fijos, más un costo fijo adicional y la tasa de impuesto a la renta.
- **Resultados:** se recalculan al instante (estado de resultados, margen neto, punto de equilibrio y margen de seguridad) frente a la base.
- **Escenarios predefinidos:** pesimista, optimista, subir precios 5 % y recortar fijos 10 %.
- **Sensibilidad:** gráfico de tornado con el efecto de mover cada variable ±10 % en la utilidad neta.
- **Meta de utilidad:** cuánto subir precios, o cuánto más vender, para ganar un monto mensual.
- **Escenarios guardados por empresa** (migración V7) y su comparación lado a lado.

## Diagnóstico empresarial

- **Período:** evalúa los últimos 3 meses completos frente a los 3 anteriores y usa hasta 24 meses de historia.
- **Indicadores:** 14, con semáforo (verde, amarillo o rojo), agrupados en 4 áreas:
  - **Rentabilidad:** margen neto, margen bruto y meses con pérdida.
  - **Solidez:** margen de seguridad, peso de los costos fijos, carga financiera, liquidez y endeudamiento. Estos dos últimos se evalúan solo si hay activos registrados en «Activos y deudas».
  - **Crecimiento:** ventas frente al trimestre anterior, tendencia de 12 meses y proyección a 3 meses.
  - **Riesgo comercial:** regularidad de las ventas, dependencia del producto principal y productos vendidos con pérdida.
- **Umbrales:** los de margen neto y margen bruto dependen del sector de la empresa.
- **Puntaje de salud (0–100):** verde = 100, amarillo = 50 y rojo = 0, promediados según el peso de cada indicador.
  - **Niveles:** Saludable (≥ 75), Estable con alertas (≥ 55), En riesgo (≥ 35) y Crítico.
  - **Límite:** si un indicador clave (peso 3) está en rojo, la calificación no puede ser mejor que «En riesgo».
- **Recomendaciones:** concretas y ordenadas por urgencia, con cifras calculadas: cuánto subir precios, cuánto vender o cuánto recortar costos fijos.
- **Fortalezas:** los indicadores en verde.
- **Empresa de demostración:** el botón «Empresa de demostración» del módulo Empresas crea «Bodega Demo» con 12 meses de datos ficticios para probar los análisis.

## Informes y reportes

- **Informes en PDF** (JasperReports 6.21, fuentes DejaVu incrustadas para que las tildes y los símbolos se vean en cualquier equipo):
  - **Informe ejecutivo:** indicadores clave, resumen, estado de resultados comparado, evolución mensual (gráfico), punto de equilibrio, productos más rentables y diagnóstico con su semáforo y recomendaciones.
  - **Estado de resultados:** estado comparado con el período anterior, márgenes, punto de equilibrio y resultado de cada mes.
  - **Rentabilidad por producto:** gráfico y detalle de ventas, margen y margen % de cada producto.
- **Libro de Excel** (Apache POI) con seis hojas: Resumen, Evolución mensual, Productos, Ventas, Compras, e Ingresos y gastos.
  - Montos, fechas y porcentajes se guardan como números con formato.
  - Cada hoja tiene filtros automáticos, encabezados inmovilizados y totales con fórmulas (`SUMIF`/`SUMIFS`) que excluyen lo anulado.
- **Plantilla única:** `src/main/resources/informes/informe.jrxml`. Cada informe es una lista de bloques (`ContenidoInforme`), así todos comparten el mismo diseño.
- **Archivos seguros:** se escriben primero en un temporal; si el destino está abierto en otro programa, se avisa sin dejar el archivo a medias.
- **Historial** de informes generados por empresa (migración V8), con «Abrir» y «Mostrar en la carpeta».
- **Permiso:** `INFORMES_GENERAR` (Administrador, Gerente y Analista).

## Configuración y copias de seguridad

- **Copias de seguridad** con `VACUUM INTO`: una copia consistente y compacta, aunque la aplicación esté en uso. Cada copia se verifica (`PRAGMA quick_check`) al crearla.
- **Copia automática** al iniciar la aplicación, como máximo una vez al día. Solo se conservan las últimas N automáticas (10 por defecto); las manuales nunca se borran solas.
- **Carpeta de copias configurable**, por ejemplo una memoria USB o una carpeta sincronizada con la nube. La predeterminada es `%USERPROFILE%\BusinessAnalyzer\copias`.
- **Restauración segura en dos pasos:**
  1. Se valida la copia: que sea SQLite, esté íntegra, sea de Business Analyzer y no venga de una versión más nueva.
  2. Se guarda una copia del estado actual y la restauración se aplica al reiniciar, antes de abrir la base de datos. Si la copia es de una versión anterior, el migrador la actualiza.
- **Pestaña Sistema:** versión de la aplicación, del esquema, de Java y de JavaFX; rutas y tamaño de la base de datos; verificación de integridad; acceso a la carpeta de datos y a los registros.
- **Permiso:** `CONFIGURACION_GESTIONAR` (solo Administrador).

## Instalación para usuarios finales

El script `empaquetar.ps1` usa `jpackage` del JDK 17 para empaquetar la aplicación con su propio Java, así que el usuario no necesita instalar nada:

```powershell
.\empaquetar.ps1              # compila, prueba y empaqueta
.\empaquetar.ps1 -SinPruebas  # más rápido
```

Genera en `target\instalador`:

| Resultado | Descripción |
|---|---|
| `Business Analyzer\Business Analyzer.exe` | Aplicación lista para usar (unos 160 MB, con Java incluido) |
| `BusinessAnalyzer-1.0.0-portable.zip` | La misma carpeta comprimida, para copiarla a otro equipo y descomprimirla |
| `Business Analyzer-1.0.0.exe` | Instalador con acceso directo y menú Inicio; se instala por usuario, sin permisos de administrador. Requiere WiX Toolset 3.14: basta con descomprimir `wix314-binaries.zip` (de [sus releases](https://github.com/wixtoolset/wix3/releases)) en `%USERPROFILE%\tools\wix314`; el script lo encuentra solo |

Los datos no se guardan dentro de la carpeta de la aplicación, sino en `%USERPROFILE%\BusinessAnalyzer`, así que reinstalar o actualizar no los borra.

> El script ejecuta `mvn clean`: cierra la aplicación si la estás ejecutando desde IntelliJ antes de empaquetar.

## Publicar una versión nueva

La versión se define **solo** en `pom.xml` (`<version>x.y.z</version>`). La leen la aplicación (título, menú lateral, Configuración → Sistema), el pie de los informes PDF y `empaquetar.ps1`.

Numeración: `1.0.1` para correcciones, `1.1.0` para funciones nuevas compatibles y `2.0.0` para cambios grandes.

1. Cambia `<version>` en `pom.xml`, por ejemplo a `1.0.1`.
2. Si el cambio modifica la base de datos, agrega una migración `V9__descripcion.sql` y regístrala en `Migrador`. Nunca modifiques una migración ya publicada.
3. Ejecuta `.\empaquetar.ps1`: corre las pruebas y genera `Business Analyzer-1.0.1.exe` y el `.zip` portable.
4. Haz commit, crea la etiqueta (`git tag -a v1.0.1 -m "..."`) y súbela con `git push --follow-tags`.
5. Publica la release:

   ```
   gh release create v1.0.1 "target/instalador/Business Analyzer-1.0.1.exe" target/instalador/BusinessAnalyzer-1.0.1-portable.zip --title "Business Analyzer Desktop 1.0.1" --notes "Cambios: ..."
   ```

El instalador nuevo actualiza la versión instalada (identificador de actualización fijo) y conserva los datos de `%USERPROFILE%\BusinessAnalyzer`. Al abrirla, las migraciones pendientes se aplican solas.
