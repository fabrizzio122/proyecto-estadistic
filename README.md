# Business Analyzer Desktop 1.2

[![Pruebas](https://github.com/fabrizzio122/proyecto-estadistic/actions/workflows/pruebas.yml/badge.svg)](https://github.com/fabrizzio122/proyecto-estadistic/actions/workflows/pruebas.yml)

Aplicación de escritorio para analizar la situación económica y financiera de negocios y
empresas: rentabilidad, punto de equilibrio, tendencias, predicciones, escenarios y diagnóstico.

## Documentación del proyecto

| Documento | Contenido |
|---|---|
| [docs/informe-final.pdf](docs/informe-final.pdf) | Informe final del proyecto (también en `.docx`) |
| [docs/manual-usuario.pdf](docs/manual-usuario.pdf) | Manual de usuario con capturas de todas las pantallas (también en `.docx`) |
| [docs/uml](docs/uml/README.md) | 9 diagramas UML: casos de uso, arquitectura, clases, secuencia, estados, modelo de datos y despliegue |
| [docs/guion-demo.md](docs/guion-demo.md) | Guion de la demostración (15 minutos) con preguntas frecuentes |
| [CHANGELOG.md](CHANGELOG.md) | Cambios de cada versión |

## Requisitos

- JDK 17 (en IntelliJ: *File → Project Structure → SDK*).
- No hace falta instalar Maven: el proyecto incluye el wrapper (`mvnw` / `mvnw.cmd`).

## Ejecutar

```powershell
.\mvnw.cmd javafx:run      # arrancar la aplicación
.\mvnw.cmd test            # ejecutar las pruebas
```

En IntelliJ IDEA: abrir la carpeta como proyecto Maven y ejecutar
`com.businessanalyzer.LauncherFGEM` (o el goal Maven `javafx:run`).

## Pruebas, cobertura e integración continua

- **335 pruebas** JUnit 5 (clases `*TestFGEM`): cálculos puros con datos en memoria y servicios con una
  base SQLite temporal y un reloj controlado.
- **Cobertura con JaCoCo**: `.\mvnw.cmd test` deja el informe en `target/site/jacoco/index.html`.
  Resumen por capa: `.\.github\scripts\resumen-cobertura.ps1`.
- **Integración continua con GitHub Actions** (`.github/workflows/pruebas.yml`): en cada push o pull
  request a `main`, GitHub compila el proyecto en Windows con JDK 17, ejecuta las pruebas y publica la
  cobertura en el resumen de la ejecución (pestaña *Actions*). El distintivo de arriba muestra el estado.

| Capa | Cobertura de líneas |
|---|---:|
| Analítica (cálculos puros) | 93 % |
| Servicios (reglas de negocio) | 93 % |
| Repositorios (SQL) | 96 % |
| Informes (PDF y Excel) | 93 % |
| **Lógica de negocio (todo menos la interfaz)** | **92 %** |

La interfaz JavaFX se verifica con capturas de la aplicación real, no con pruebas unitarias; por eso
el total del proyecto, incluida la interfaz, es 47 %.

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
├── analytics     cálculos puros (sin BD ni JavaFX): financiero, estadística, predicción, simulación,
│                 diagnóstico, patrimonio, ganadería, agricultura, valoración y sugerencias
├── informes      contenido de los informes, PDF (JasperReports) y Excel (Apache POI)
├── importacion   plantilla y lectura de los libros de Excel a importar (Fase 26)
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
17. Visitas de inspección: verificación en campo, compromisos, fotos e informe técnico en PDF ✅
18. Integración agropecuaria: ventas agro en los análisis, evolución del hato, comparación de campañas y diagnóstico del productor ✅
19. Valoración del negocio: patrimonial, capitalización de utilidades, flujos descontados, sensibilidad e informe PDF ✅
20. Pulido para la presentación: editar registros, Dashboard nuevo, comparación del mes en curso y datos demo realistas ✅
21. Material del curso: diagramas UML, guion de la demostración y versión 1.1.0 con instalador ✅
22. Tesorería: cuentas de dinero, ventas y compras al crédito, flujo de efectivo y proyección de caja ✅
23. Rediseño visual «pizarra de bodega»: tema nuevo, color de acento por usuario y Dashboard renovado ✅
24. Iniciales del autor (FGEM) en todos los identificadores del código ✅
25. Integración continua (GitHub Actions) y cobertura de pruebas (JaCoCo) ✅
26. Importación desde Excel: plantilla, revisión previa sin guardar e importación de productos, ventas, compras y gastos ✅
27. Sugerencias por producto: precios a ajustar, reposición y productos sin movimiento ✅
28. Productos perecibles y sugerencias afinadas con datos reales: venta lenta con 180 días, agotados según su historial, ganancia en contexto ✅
29. Lista de compras en PDF y Excel desde la reposición sugerida, agrupada por categoría ✅
30. «Atender hoy» en el Dashboard: agotados, perecibles por vencer, productos con pérdida y compra sugerida ✅
31. Antigüedad de saldos por cobrar y por pagar (al día, 1-30, 31-60 y más de 60 días) y resumen por cliente o proveedor ✅
32. Registrar la compra sugerida: de la reposición a una compra lista para revisar en «Ventas y compras» ✅

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

- **Activos:** cuentas por cobrar y otros corrientes (el efectivo está en «Tesorería») (se registra el saldo y se actualiza cuando cambia); terrenos, edificios, maquinaria, vehículos, muebles y equipos de cómputo (se registra el valor de adquisición).
- **Depreciación:** en línea recta, por meses completos, según la vida útil y el valor residual. Los terrenos no se deprecian. Una vida útil vacía significa que el bien no se deprecia.
- **Inventario:** no se registra a mano. Se calcula con el stock de cada producto por su costo unitario.
- **Deudas:** monto, acreedor, tasa anual, cuota mensual y vencimiento (estos tres son opcionales). Cada **pago** indica el capital (reduce el saldo), los intereses (se registran solos como gasto financiero) y la cuenta de la que salió el dinero. Con saldo cero la deuda queda «Cancelada».
- **Corto y largo plazo:** sin vencimiento, o si vence dentro de 12 meses, toda la deuda es de corto plazo. Si vence después, se considera de corto plazo la parte proporcional a 12 meses.
- **Balance general** al día de hoy: activo corriente y no corriente, pasivo corriente y no corriente, y patrimonio (activo − pasivo).
- **Indicadores:** liquidez corriente, prueba ácida, capital de trabajo, endeudamiento, deuda por cada 1 de patrimonio, rentabilidad del patrimonio y de los activos (con la utilidad de los últimos 12 meses llevada a un año) y cobertura de cuotas (utilidad operativa mensual ÷ cuotas).
- **Nada se elimina:** los activos se dan de baja con fecha y motivo; las deudas y los pagos se anulan.
- **Permisos:** los mismos de Gestión financiera (ver y registrar).

## Tesorería (Fase 22)

El dinero de la empresa vive en **cuentas**: la caja, cuentas bancarias y billeteras digitales (Yape, Plin). El saldo de cada cuenta **no se escribe a mano**: se calcula con su saldo inicial más las entradas menos las salidas.

| Registro | Qué pasa con el dinero |
|---|---|
| Venta o compra **al contado** | Entra a (o sale de) la cuenta elegida el mismo día |
| Venta o compra **al crédito** | Queda por cobrar (o por pagar) hasta registrar los cobros o pagos, que pueden ser parciales |
| Ingreso o gasto | Entra a (o sale de) la cuenta elegida |
| Compra de un activo | Sale de la cuenta con que se pagó (o de ninguna, si ya se tenía o se financió) |
| Préstamo recibido | Entra a la cuenta donde se depositó |
| Pago de una cuota | Sale el capital y los intereses; los intereses se registran solos como gasto financiero |
| Transferencia, aporte o retiro del dueño | Solo cambia el dinero de lugar o cuánto puso o sacó el dueño: no afecta la utilidad |

- **Estado de flujo de efectivo** (método directo, NIC 7): actividades de operación, inversión y financiamiento; dinero al inicio y al final del período.
- **Proyección de caja** a 6 meses: flujo de operación pronosticado (el mejor método de la Fase 9) menos el capital de las cuotas; escenario base y pesimista, con aviso del primer mes en que faltaría dinero.
- **Balance general**: el efectivo es el saldo de las cuentas; las ventas y compras al crédito pendientes son cuentas por cobrar y por pagar.
- **Antigüedad de saldos** (Fase 31): en *Por cobrar y por pagar*, lo pendiente se reparte según los días desde el vencimiento (al día o sin fecha límite, 1-30, 31-60 y más de 60 días) y se resume por cliente o proveedor, ordenado por lo vencido. Se agrupa por nombre sin distinguir mayúsculas ni espacios; el cálculo es puro (`analytics/tesoreria/AntiguedadSaldosFGEM`).
- **Diagnóstico**: nuevo indicador «Caja proyectada» en Solidez.
- **Migración V13**: cada empresa recibe una cuenta «Caja» con el saldo que tenía como «Caja y bancos». Los registros anteriores se conservan en los análisis, pero no mueven el saldo (ya está incluido en el saldo inicial).
- **Permisos**: ver con `FINANZAS_VER`; cuentas y movimientos con `FINANZAS_GESTIONAR`; cobros y pagos de ventas y compras con `OPERACIONES_GESTIONAR`.

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

## Visitas de inspección (empresas agropecuarias)

- **Visita:** fecha, tipo (evaluación inicial, supervisión, seguimiento de crédito, asistencia técnica, evaluación final), inspector, lugar, coordenadas GPS (grados decimales), quién atendió, observaciones, conclusión y próxima visita.
- **Verificación en campo:** 12 aspectos en tres áreas (ganadería, agricultura y generales). Cada uno se marca «Cumple», «Cumple en parte», «No cumple» o «No aplica». El **cumplimiento** vale 1, 0.5 y 0 sin contar los que no aplican, y propone la conclusión: desde 80 % favorable, desde 50 % con observaciones y, por debajo, desfavorable.
- **Compromisos:** se acuerdan en una visita con un plazo y, en la visita siguiente, aparecen para marcarlos como cumplidos o no cumplidos. Los pendientes con el plazo vencido se resaltan.
- **Fotos:** JPG o PNG de hasta 15 MB. Se copian a la carpeta de datos, en `fotos-visitas/`, y se incluyen en las copias de seguridad.
- **Informe técnico (PDF):** datos de la visita, verificación en campo, compromisos, indicadores del hato y de las campañas de los 12 meses previos, situación patrimonial, observaciones, conclusión, hasta 6 fotos y espacio para las firmas. Queda en el historial de informes.
- **Permisos:** las ven todos los roles con acceso a la empresa; las registran el administrador, el gerente y el analista (el rol del inspector). El operador (productor) las ve pero no las modifica.

## Integración agropecuaria

- **El productor registra todo en Ganadería y Agricultura** (no en «Ventas y compras»). Esas ventas entran solas al estado de resultados, la estadística, las predicciones, la simulación, el diagnóstico y el Dashboard:
  - **Venta de animales** con su valor total, en la fecha de la venta.
  - **Leche, fibra y lana:** los registros de producción **con precio** se consideran vendidos.
  - **Cosechas vendidas con precio.** Su costo es el costo por kilo de la campaña. Al cerrar la campaña se reconoce el resto del costo (semilla, forraje para los animales, merma o campaña perdida). Lo que consume la familia no es un costo del negocio.
  - Los gastos del productor (pastor, vacunas, fletes, intereses...) se registran en Gestión financiera, como en cualquier empresa.
  - El módulo «Ventas y compras» sigue mostrando solo sus propias operaciones.
- **Ganadería → Evolución:** animales al cierre de cada mes por especie, nacimientos, muertes y salidas, producción mensual con su tendencia (regresión lineal) y una lectura en lenguaje sencillo (12, 24 o 36 meses).
- **Ganadería → Indicadores:** cada indicador muestra entre paréntesis el valor del período anterior, si ya había animales registrados antes de ese período.
- **Agricultura → Comparación:** campañas cerradas por cultivo (rendimiento, porcentaje de la referencia, costo y margen por hectárea, mejor campaña) y gráfico de rendimiento frente a la referencia.
- **Diagnóstico del productor:** nueva área «Producción agropecuaria» con natalidad, mortalidad de crías (umbrales por especie), rendimiento de los cultivos frente a la referencia y cumplimiento de compromisos de inspección. En productores, las ventas se comparan con el mismo período del año anterior por la estacionalidad.

## Dashboard y edición de registros (Fase 20)

- **Dashboard:** indicadores del mes en curso comparados con los mismos días del mes anterior, ventas y utilidad de los últimos 12 meses, salud del negocio (diagnóstico), productos que más aportan al margen, ventas esperadas del próximo mes y «Atender hoy» (lo urgente de las sugerencias, Fase 30). Cada tarjeta abre su módulo.
- **Mes en curso:** «Este mes» va del día 1 a hoy y se compara con los mismos días del mes anterior (no medio mes contra un mes entero); «Este año» se compara con las mismas fechas del año pasado.
- **Editar:** ventas, compras, movimientos del hato, producción, costos y cosechas tienen botón «Editar». El registro original queda anulado como historial (con la referencia del que lo reemplaza) y se guarda el corregido en la misma transacción; el stock y las existencias se recalculan solos.
- **Bodega Demo:** una venta por día (cierre de caja) repartida según el día de la semana, con un febrero flojo; los datos salen siempre iguales.

## Valoración del negocio

- **Patrimonial:** activos − deudas del balance general (activos fijos a su valor actual, inventario y cultivos en proceso al costo, ganado a precio de mercado).
- **Capitalización de utilidades:** utilidad neta anual (últimos 12 meses completos, llevada a un año) ÷ tasa de descuento.
- **Flujos descontados:** flujo para el dueño (utilidad + depreciación) proyectado 5 años con el crecimiento elegido, más un valor terminal con 2 % de crecimiento, traído a valor de hoy.
- **Valor de referencia:** el promedio de los dos métodos por rentabilidad si supera al patrimonial (la diferencia es el **fondo de comercio**); si no, el valor patrimonial. Se muestra también el rango.
- **Supuestos por empresa:** tasa de descuento (20 % por defecto, entre 5 % y 60 %) y crecimiento anual (por defecto, el de la tendencia de las ventas del último año, acotado entre −10 % y 10 %).
- **Sensibilidad:** valor por flujos descontados con la tasa ±5 puntos y el crecimiento ±2 puntos.
- **Informe PDF** desde el módulo; queda en el historial de informes. Permiso: ver análisis (exportar, generar informes).

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
- **Indicadores:** 14 (18 en productores agropecuarios), con semáforo (verde, amarillo o rojo), agrupados en 4 áreas:
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
- **Plantilla única:** `src/main/resources/informes/informe.jrxml`. Cada informe es una lista de bloques (`ContenidoInformeFGEM`), así todos comparten el mismo diseño.
- **Archivos seguros:** se escriben primero en un temporal; si el destino está abierto en otro programa, se avisa sin dejar el archivo a medias.
- **Historial** de informes generados por empresa (migración V8), con «Abrir» y «Mostrar en la carpeta».
- **Permiso:** `INFORMES_GENERAR` (Administrador, Gerente y Analista).

## Sugerencias por producto (Fase 27)

*Análisis → Sugerencias* convierte el análisis en acciones concretas, con las ventas de los últimos 60 días
(o desde la primera venta, si es más reciente), y de los últimos 180 días para la venta lenta:

| Pestaña | Qué muestra |
|---|---|
| **Precios** | Productos que se venden con pérdida o con menos margen que el objetivo (20 % por defecto, se recuerda por empresa). Precio sugerido = costo ÷ (1 − margen), redondeado hacia arriba a 10 céntimos, y la ganancia extra al mes si se vende la misma cantidad, comparada con la utilidad mensual del diagnóstico. Los productos que casi no se venden aparecen al final, en gris. |
| **Reposición** | Productos que se agotan en menos de 14 días (3 si son perecibles) al ritmo de los últimos 60 días, y cuánto comprar. Un producto agotado sin ventas recientes usa su ritmo de los últimos 180 días: no se vendió porque no había. |
| **Sin movimiento** | Perecibles que se vencerán antes de venderse (más de 15 días de stock), productos sin ventas en 180 días y productos que tardarían más de 180 días en venderse, con el % del inventario que representan. |

**Lista de compras** (Fase 29): desde la pestaña *Reposición*, el botón *Lista de compras* guarda los productos por
reponer agrupados por categoría (los agotados y urgentes primero), con la cantidad y el costo aproximado:
en **PDF** para imprimir, o en **Excel** con el costo como fórmula y una columna «Comprado» para marcar.
Requiere el permiso `INFORMES_GENERAR` y queda en el historial de *Informes y reportes*.

**Registrar compra** (Fase 32): abre una compra nueva en *Ventas y compras* con los mismos productos y cantidades, al
costo registrado de cada uno, para revisarla, quitar lo que no se compre y registrarla (permiso `OPERACIONES_GESTIONAR`).

Los productos se marcan como **perecibles** en su formulario o con la columna «Perecible» (Sí/No) de la hoja
*Productos* al importar (migración V15).

Los cálculos son puros (`analytics/sugerencias`) y se prueban sin base de datos.

## Importar desde Excel (Fase 26)

Para cargar el historial de un negocio real sin escribirlo a mano (*Gestión → Importar desde Excel*):

1. **Descargar la plantilla**: un libro con las hojas *Productos*, *Ventas*, *Compras* y *Gastos*, instrucciones y
   listas desplegables. Hay una versión con **tres meses de ejemplo de una bodega** para probar.
2. **Elegir el archivo**: se **revisa todo sin guardar nada** y se muestra qué se va a importar (productos, ventas,
   compras, gastos y período) o la lista de errores con **hoja y fila**.
3. **Importar**: antes se crea una copia de seguridad «Antes de importar».

Reglas:

- **Todo o nada**: la importación ocurre en una sola transacción; si una fila falla no se guarda ninguna.
- Cada fila pasa por **los mismos servicios que el registro manual** (validaciones, permisos, numeración, historial de
  precios, tesorería). La transacción de la importación envuelve a las de los servicios (`DatabaseFGEM` reutiliza la
  conexión abierta en el hilo) y cada fila usa un *savepoint* para informar todos los errores de una vez.
- Las columnas se reconocen **por su encabezado**, sin importar el orden, mayúsculas, tildes ni palabras como «N°»
  o «de». Fechas como fecha de Excel o texto `dd/mm/aaaa`; montos con punto o coma decimal (`1.234,50`).
- En *Ventas* y *Compras*, las filas con la **misma fecha y documento** forman una sola operación. El producto se
  indica por **código o nombre**. Precio vacío = precio del catálogo; IGV vacío = el habitual de la empresa.
- Las operaciones se registran **por orden de fecha** (compras antes que ventas del mismo día), así cada venta usa
  el costo vigente ese día. Si una venta deja el stock en negativo, se importa y se avisa.
- Los productos que ya existen (mismo código **o** mismo nombre) **no se modifican ni se duplican**: solo se usan.
- **Volver a importar no duplica**: una boleta o comprobante ya registrado (misma fecha y número), un segundo
  «Inventario inicial» o un gasto idéntico a uno ya registrado se marcan como error. Si la empresa ya tiene datos
  importados, la revisión lo avisa (las filas sin documento no se pueden comprobar).
- En *Gastos*, la categoría acepta el nombre de la lista o palabras comunes («luz», «sueldo», «alquiler»…).

## Configuración y copias de seguridad

- **Copias de seguridad** con `VACUUM INTO`: una copia consistente y compacta, aunque la aplicación esté en uso. Cada copia se verifica (`PRAGMA quick_check`) al crearla.
- **Formato:** cada copia es un solo archivo `.zip` con la base de datos y las fotos de las visitas de inspección. Basta con llevar ese archivo a otra computadora. Las copias antiguas (`.db`, solo la base de datos) se siguen pudiendo restaurar; en ese caso las fotos actuales se conservan.
- **Copia automática** al iniciar la aplicación, como máximo una vez al día. Solo se conservan las últimas N automáticas (10 por defecto); las manuales nunca se borran solas.
- **Carpeta de copias configurable**, por ejemplo una memoria USB o una carpeta sincronizada con la nube. La predeterminada es `%USERPROFILE%\BusinessAnalyzer\copias`.
- **Restauración segura en dos pasos:**
  1. Se valida la copia: que sea SQLite, esté íntegra, sea de Business Analyzer y no venga de una versión más nueva.
  2. Se guarda una copia del estado actual (con sus fotos) y la restauración se aplica al reiniciar, antes de abrir la base de datos: se reemplazan la base y la carpeta de fotos. Si la copia es de una versión anterior, el migrador la actualiza.
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
| `BusinessAnalyzer-1.1.0-portable.zip` | La misma carpeta comprimida, para copiarla a otro equipo y descomprimirla |
| `Business Analyzer-1.1.0.exe` | Instalador con acceso directo y menú Inicio; se instala por usuario, sin permisos de administrador. Requiere WiX Toolset 3.14: basta con descomprimir `wix314-binaries.zip` (de [sus releases](https://github.com/wixtoolset/wix3/releases)) en `%USERPROFILE%\tools\wix314`; el script lo encuentra solo |

Los datos no se guardan dentro de la carpeta de la aplicación, sino en `%USERPROFILE%\BusinessAnalyzer`, así que reinstalar o actualizar no los borra.

> El script ejecuta `mvn clean`: cierra la aplicación si la estás ejecutando desde IntelliJ antes de empaquetar.

## Publicar una versión nueva

La versión se define **solo** en `pom.xml` (`<version>x.y.z</version>`). La leen la aplicación (título, menú lateral, Configuración → Sistema), el pie de los informes PDF y `empaquetar.ps1`.

Numeración: `1.1.1` para correcciones, `1.2.0` para funciones nuevas compatibles y `2.0.0` para cambios grandes.

1. Cambia `<version>` en `pom.xml`, por ejemplo a `1.1.1`.
2. Si el cambio modifica la base de datos, agrega una migración nueva (la siguiente es `V15__descripcion.sql`) y regístrala en `MigradorFGEM`. Nunca modifiques una migración ya publicada.
3. Ejecuta `.\empaquetar.ps1`: corre las pruebas y genera `Business Analyzer-1.1.1.exe` y el `.zip` portable.
4. Haz commit, crea la etiqueta (`git tag -a v1.1.1 -m "..."`) y súbela con `git push --follow-tags`.
5. Publica la release:

   ```
   gh release create v1.1.1 "target/instalador/Business Analyzer-1.1.1.exe" target/instalador/BusinessAnalyzer-1.1.1-portable.zip --title "Business Analyzer Desktop 1.1.1" --notes "Cambios: ..."
   ```

El instalador nuevo actualiza la versión instalada (identificador de actualización fijo) y conserva los datos de `%USERPROFILE%\BusinessAnalyzer`. Al abrirla, las migraciones pendientes se aplican solas.
