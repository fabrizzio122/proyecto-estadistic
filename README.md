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
11. Diagnóstico empresarial: puntaje de salud, semáforo de 12 indicadores y recomendaciones ✅
12. Informes (PDF y Excel)
13. Configuración, copias de seguridad e instalador

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

## Análisis financiero

- **Estado de resultados** del período elegido (ventas − costo de ventas − gastos operativos ± resultados financieros − impuesto a la renta), con el % sobre las ventas y la comparación con el período anterior equivalente.
- **Márgenes** bruto, operativo y neto, con su variación en puntos porcentuales.
- **Punto de equilibrio** en ventas: costos fijos ÷ razón de contribución. Incluye el margen de seguridad y un gráfico de ingresos frente a costos.
- **Rentabilidad por producto**: unidades, ventas, margen y aporte al margen total. Usa el costo vigente en cada venta.
- **Evolución de los últimos 12 meses** y una **lectura rápida** en lenguaje sencillo.
- Gráficos con **JFreeChart**. Con clic derecho sobre un gráfico se puede guardarlo como imagen.
- Los ratios de liquidez y endeudamiento necesitan datos de balance (activos y pasivos), que el sistema aún no registra.

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
- **Indicadores:** 12, con semáforo (verde, amarillo o rojo), agrupados en 4 áreas:
  - **Rentabilidad:** margen neto, margen bruto y meses con pérdida.
  - **Solidez:** margen de seguridad, peso de los costos fijos y carga financiera.
  - **Crecimiento:** ventas frente al trimestre anterior, tendencia de 12 meses y proyección a 3 meses.
  - **Riesgo comercial:** regularidad de las ventas, dependencia del producto principal y productos vendidos con pérdida.
- **Umbrales:** los de margen neto y margen bruto dependen del sector de la empresa.
- **Puntaje de salud (0–100):** verde = 100, amarillo = 50 y rojo = 0, promediados según el peso de cada indicador.
  - **Niveles:** Saludable (≥ 75), Estable con alertas (≥ 55), En riesgo (≥ 35) y Crítico.
  - **Límite:** si un indicador clave (peso 3) está en rojo, la calificación no puede ser mejor que «En riesgo».
- **Recomendaciones:** concretas y ordenadas por urgencia, con cifras calculadas: cuánto subir precios, cuánto vender o cuánto recortar costos fijos.
- **Fortalezas:** los indicadores en verde.
- **Empresa de demostración:** el botón «Empresa de demostración» del módulo Empresas crea «Bodega Demo» con 12 meses de datos ficticios para probar los análisis.
