# Guion de la demostración — Business Analyzer Desktop (hasta la fase 33)

Curso: Ingeniería de Software 2 · Duración: **15 minutos** + preguntas.

La demo cuenta una historia: *el dueño de una bodega registra su día a día y el sistema le dice
cómo está su negocio, si lo que ve es real o casualidad, y qué hacer hoy*. Al final se muestra que el
mismo motor sirve a un productor agropecuario.

---

## 1. Preparación (el día anterior)

| # | Tarea | Hecho |
|---|---|---|
| 1 | **Usar la versión actual del código**: el instalador 1.2.1 publicado no tiene las fases 25 a 33. Ejecutar `.\mvnw.cmd javafx:run` o generar un instalador nuevo con `.\empaquetar.ps1`. | ☐ |
| 2 | Abrir la app, crear el administrador y entrar. | ☐ |
| 3 | En **Empresas** → **Empresa de demostración** → **Bodega (comercio)**: se crea **Bodega Demo** (12 meses, una venta por día). | ☐ |
| 4 | En **Empresas** → **Empresa de demostración** → **Fundo ganadero (agropecuario)**: se crea **Fundo Demo**. | ☐ |
| 5 | En **Usuarios y permisos** crear un usuario `operador` con rol **Operador** y acceso a Bodega Demo. Entrar una vez con él para cambiar la contraseña temporal. | ☐ |
| 6 | **Para que Reposición tenga productos** (Bodega Demo tiene stock de todo): crear el producto *Leche evaporada 400 g* (costo 3.40, precio 4.20), registrar una compra de 30 unidades fechada hace 20 días y cuatro ventas de 7 unidades en días distintos. Queda con 2 unidades (alcanzan para unos 4 días): aparece en **Reposición** como «Se agota esta semana» y en **Atender hoy**. | ☐ |
| 7 | **Para la antigüedad de saldos**: registrar una venta **al crédito** a *Juana Quispe* fechada hace 95 días con vencimiento hace 80 días. Bodega Demo ya trae otra vencida (Restaurante Doña Rosa). | ☐ |
| 8 | Generar una vez cada informe que se va a mostrar (la primera compilación de JasperReports tarda unos segundos; después es inmediata). | ☐ |
| 9 | Dejar abiertos en otra ventana los diagramas `docs/uml/png/02-arquitectura.png` y `04-clases-analisis.png`. | ☐ |
| 10 | Pantalla: resolución 1920×1080 o menor, escala 100–125 %; cerrar notificaciones de Windows. | ☐ |
| 11 | **Plan B**: grabar la demo completa en video y llevar una copia de seguridad de la base preparada en una USB. | ☐ |

> La aplicación no necesita Internet ni servidor: todo funciona desde la laptop.

---

## 2. Guion minuto a minuto

### 0:00 – 1:00 · El problema

> «Los pequeños negocios —una bodega, un restaurante, un productor de alpacas— registran ventas y
> gastos, pero no saben si ganan dinero, si un cambio en sus ventas es real o casualidad, ni qué hacer
> hoy. Business Analyzer convierte sus registros diarios en análisis financiero, estadístico y
> predictivo, y termina en acciones concretas.»

### 1:00 – 2:00 · Cómo está construido

Mostrar `02-arquitectura.png`.

- Aplicación de escritorio **Java 17 + JavaFX 21**, base de datos **SQLite** local, **arquitectura en capas**.
- El paquete **analytics** son cálculos puros, sin base de datos: por eso se prueban fácil.
- **340 pruebas JUnit 5** que GitHub Actions ejecuta en cada cambio; **92 %** de cobertura en la lógica de negocio.
- El esquema se versiona con **15 migraciones** que se aplican solas al iniciar.

### 2:00 – 2:45 · Seguridad y roles

1. Entrar como `operador`: el menú **no muestra** Análisis, Usuarios ni Configuración.
2. Entrar como **administrador**: aparece el menú completo. «Cada servicio vuelve a revisar el permiso aunque alguien salte la interfaz.»

### 2:45 – 4:00 · Dashboard y «Atender hoy»

Empresa activa: **Bodega Demo**.

- Indicadores del mes comparados con **los mismos días del mes anterior** (comparación justa).
- Bajar hasta **Atender hoy**: «Apenas abre la app, el dueño ve lo urgente: el agua mineral se vende con
  pérdida y la leche se agota esta semana.» → **Ver sugerencias**.

### 4:00 – 5:15 · Registrar y corregir una venta

1. **Ventas y compras** → nueva venta de *Arroz 5 kg* con una cantidad mayor al stock: el sistema avisa **stock insuficiente**.
2. Registrar con la cantidad correcta: número `V-…` e IGV 18 % automáticos.
3. **Editar** la venta: la original queda **anulada como historial** y la nueva dice «Corrige V-…», todo en una transacción.

### 5:15 – 6:15 · Análisis financiero

- Estado de resultados, **punto de equilibrio** y margen de seguridad.
- **Rentabilidad por producto**: *Agua mineral (caja x15)* se vende a S/ 15,00 y cuesta S/ 16,00 → margen negativo.

### 6:15 – 8:45 · Estadística e inferencia (lo central del curso)

**Análisis estadístico**, últimos 6 meses:

1. **Resumen descriptivo**: media, mediana, desviación, coeficiente de variación y, en *Ventas por día*,
   el **intervalo de confianza al 95 %**: «con 95 % de confianza, la venta diaria promedio está entre… y …»
   (t de Student, media ± t · s / √n).
2. **Días de la semana**: el sábado vende más. «¿Es real o casualidad?» → fila **ANOVA**: F y p < 0,001,
   se rechaza que todos los días vendan igual. Pasar el mouse sobre la fila para mostrar la hipótesis nula.
3. **Tendencia**: regresión lineal con R² y valor p, y la **prueba t de Welch** contra el período anterior:
   «las ventas diarias subieron y el cambio es significativo».
4. **Predicciones**: prueba tres métodos con los meses reales y elige el de menor error.

### 8:45 – 10:45 · Sugerencias: de los números a la acción

**Sugerencias**:

1. **Precios**: el agua mineral con su **precio sugerido** = costo ÷ (1 − margen objetivo) y la ganancia extra al mes.
2. **Reposición**: la leche se agota esta semana; cuánto comprar para 14 días (3 si es perecible).
3. **Registrar compra** → se abre la compra ya llena en Ventas y compras; completar el proveedor y registrar.
4. **Lista de compras → PDF**: agrupada por categoría, lista para llevar al proveedor.
5. **Sin movimiento**: dinero parado y qué hacer.

### 10:45 – 11:45 · Cobranza

**Tesorería** → **Por cobrar y por pagar**: los cuatro recuadros de **antigüedad** (al día, 1-30, 31-60,
más de 60 días) y **Por cliente**. Seleccionar a *Juana Quispe*: «su deuda tiene más de 60 días, es la más difícil de cobrar».

### 11:45 – 12:45 · Diagnóstico e informes

- **Diagnóstico empresarial**: puntaje de salud, **semáforo** por indicador y recomendaciones con cifras.
- **Informes y reportes** → PDF del informe ejecutivo, listo para el banco o el contador.

### 12:45 – 14:00 · El mismo motor en el campo

Cambiar a **Fundo Demo**: aparece la sección **Agropecuario** (ganadería, campañas agrícolas y visitas de
inspección con **informe técnico en PDF**). «Lo validamos con un ingeniero agrario.»

### 14:00 – 15:00 · Calidad y cierre

- 33 fases incrementales, 340 pruebas, integración continua, 15 migraciones, copias de seguridad con restauración.
- Probado con el historial de una **bodega real** importado desde Excel.
- Cierre: «Business Analyzer convierte registros diarios en decisiones respaldadas por estadística. Gracias.»

---

## 3. Preguntas frecuentes del jurado

| Pregunta | Respuesta corta |
|---|---|
| ¿Por qué SQLite y no MySQL/PostgreSQL? | Es una app de escritorio para un negocio pequeño: un archivo local, sin servidor ni instalación, con transacciones ACID. Las copias de seguridad son copiar un archivo (`VACUUM INTO`). |
| ¿Por qué no usaron Spring ni JPA? | JDBC puro para controlar el SQL y mostrar cada capa. La inyección de dependencias se hace por constructor en `AppContextFGEM`. |
| ¿Cómo guardan el dinero sin errores de redondeo? | `BigDecimal` en Java y enteros ×10 000 en SQLite (`util.MontosFGEM`). Nunca `double` para dinero. |
| ¿Qué hipótesis prueba el ANOVA? | H₀: todos los días de la semana venden lo mismo en promedio. H₁: al menos uno es distinto. Con p < 0,05 se rechaza H₀. η² dice qué parte de la variación explica el día. |
| ¿Por qué prueba t de Welch y no la clásica? | Porque no supone que los dos períodos tengan la misma varianza; es más segura cuando las ventas cambian de un período a otro. |
| ¿Qué supuestos tienen las pruebas? | Que los días son observaciones independientes. Con muchos días, la prueba t y el ANOVA son robustos a que las ventas no sean exactamente normales. Un modelo de series de tiempo (ARIMA) sería el siguiente paso. |
| ¿Cómo comprobaron que las fórmulas están bien? | Las pruebas usan ejemplos que se resuelven a mano: lunes {1, 2, 3} y martes {4, 5, 6} dan F(1, 4) = 13,5 y p = 0,0213; con dos grupos, t² = F. |
| ¿Cómo eligen el método de predicción? | Validación *rolling-origin*: cada método pronostica meses que ya ocurrieron y se elige el de menor error medio absoluto (MAE). |
| ¿Cómo calculan cuánto reponer? | Venta diaria = vendido ÷ días; se repone lo que alcanza para menos de 14 días (3 si es perecible) y se compra venta diaria × días − stock. |
| ¿Cómo probaron el sistema? | 340 pruebas JUnit 5 que corren en cada cambio con GitHub Actions: cálculos puros con datos en memoria y servicios con una base SQLite temporal y un reloj controlado. |
| ¿Por qué no se pueden borrar ventas? | Por trazabilidad: se **anulan** con motivo y usuario; corregir = anular + registrar en una transacción. |
| ¿Qué falta? | Versión multiusuario en red, series de tiempo para la inferencia, fórmulas rurales del ingeniero agrario y publicar la versión 1.3.0. |

---

## 4. Si algo sale mal

| Problema | Qué hacer |
|---|---|
| La app no abre | Ejecutar desde el código con `.\mvnw.cmd javafx:run` (requiere `JAVA_HOME` apuntando al JDK 17). |
| Reposición sale vacía | Es correcto si hay stock para dos semanas: explicarlo y mostrar la captura del manual (capítulo Sugerencias). |
| El informe PDF tarda | Es la primera compilación; seguir hablando, aparece en segundos (por eso el paso 8 de la preparación). |
| Se olvidó la contraseña | Entrar con otro administrador → **Usuarios y permisos** → restablecer contraseña. |
| Falla todo | Pasar el video grabado (plan B) y explicar sobre él. |
