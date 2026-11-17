# Guion de la demostración — Business Analyzer Desktop 1.2

Curso: Ingeniería de Software 2 · Duración: **15 minutos** + preguntas.

La demo cuenta una historia: *el dueño de una bodega registra su día a día y el sistema le dice
cómo está su negocio, qué va a pasar y qué hacer*. Después se muestra que el mismo motor sirve a un
productor agropecuario y al ingeniero que lo inspecciona.

---

## 1. Preparación (el día anterior)

| # | Tarea | Hecho |
|---|---|---|
| 1 | Instalar `Business Analyzer-1.2.1.exe` en la laptop de la presentación (o descomprimir el `.zip` portable). | ☐ |
| 2 | Abrir la app, crear el administrador y entrar. | ☐ |
| 3 | En **Empresas** → **Empresa de demostración** → **Bodega (comercio)**: se crea **Bodega Demo** (12 meses, una venta por día). | ☐ |
| 4 | En **Empresas** → **Empresa de demostración** → **Fundo ganadero (agropecuario)**: se crea **Fundo Demo**. | ☐ |
| 5 | En **Usuarios y permisos** crear un usuario `operador` con rol **Operador** y acceso a Bodega Demo. Entrar una vez con él para cambiar la contraseña temporal. | ☐ |
| 6 | Generar una vez cada informe que se va a mostrar (la primera compilación de JasperReports tarda unos segundos; después es inmediata). | ☐ |
| 7 | Dejar abiertos en otra ventana los diagramas `docs/uml/png/02-arquitectura.png` y `05-secuencia-registrar-venta.png`. | ☐ |
| 8 | Pantalla: resolución 1920×1080 o menor, escala 100–125 %; cerrar notificaciones de Windows. | ☐ |
| 9 | **Plan B**: grabar la demo completa en video y llevar el `.zip` portable en una USB. | ☐ |

> La aplicación no necesita Internet ni servidor: todo funciona desde la laptop.

---

## 2. Guion minuto a minuto

### 0:00 – 1:00 · El problema

> «Los pequeños negocios —una bodega, un restaurante, un productor de alpacas— registran ventas y
> gastos, pero no saben si ganan dinero, cuándo dejan de perder ni qué pasará el próximo mes. Las
> herramientas profesionales son caras o necesitan un contador. Business Analyzer convierte sus
> registros diarios en análisis financiero, estadístico y predictivo, con recomendaciones en
> lenguaje sencillo.»

### 1:00 – 2:30 · Cómo está construido

Mostrar `02-arquitectura.png`.

- Aplicación de escritorio **Java 17 + JavaFX 21**, base de datos **SQLite** local.
- **Arquitectura en capas**: controladores (pantalla) → servicios (reglas y permisos) → repositorios (SQL).
- El paquete **analytics** son cálculos puros, sin base de datos: por eso se prueban fácil (302 pruebas JUnit 5).
- **Sin frameworks pesados**: JDBC puro y composición manual en `AppContextFGEM`, para entender cada capa.
- El esquema se versiona con **14 migraciones** que se aplican solas al iniciar.

### 2:30 – 4:00 · Seguridad y roles

1. Entrar como `operador`. Señalar que el menú **no muestra** Análisis, Usuarios ni Configuración.
2. Cerrar sesión y entrar como **administrador**: aparece el menú completo.
3. Decir: «Contraseñas con PBKDF2, bloqueo de 15 minutos tras 5 intentos fallidos y cada servicio
   vuelve a revisar el permiso aunque alguien salte la interfaz.»

### 4:00 – 5:00 · Dashboard

Empresa activa: **Bodega Demo**.

- Indicadores del mes en curso comparados con **los mismos días del mes anterior** (comparación justa).
- Hacer clic en una tarjeta → abre el módulo correspondiente.

### 5:00 – 7:00 · Registrar y corregir una venta

1. **Ventas y compras** → nueva venta de *Arroz 5 kg* con una cantidad mayor al stock.
2. El sistema avisa **stock insuficiente** y pregunta si registrar igual → cancelar y bajar la cantidad.
3. Registrar. Se asigna el número `V-…` y el IGV 18 % se calcula solo.
4. Seleccionar la venta → **Editar** → cambiar la cantidad. Mostrar que la original queda
   **anulada como historial** («Corregida: reemplazada por V-…») y la nueva dice «Corrige V-…».
5. Mostrar `05-secuencia-registrar-venta.png`: todo ocurre en **una transacción**; si algo falla no se
   pierde ni se duplica nada.

### 7:00 – 8:30 · Análisis financiero

**Análisis financiero**, últimos 3 meses:

- Estado de resultados: ventas → costo de ventas → utilidad bruta → gastos → utilidad neta.
- **Punto de equilibrio**: cuánto hay que vender para no perder y el margen de seguridad.
- **Rentabilidad por producto**: *Agua mineral (caja x15)* se vende a S/ 15,00 y cuesta S/ 16,00 →
  **margen negativo**. «El sistema detecta lo que el dueño no ve.»

### 8:30 – 9:30 · Estadística y predicciones

- **Análisis estadístico**: tendencia creciente, diciembre es el pico y febrero el mes flojo; ABC de productos.
- **Predicciones**: el sistema prueba tres métodos (tendencia lineal, promedio móvil, Holt) con los
  meses reales, **elige el de menor error** y muestra el intervalo de confianza.

### 9:30 – 10:30 · Simulación de escenarios

**Simulación de escenarios** → «¿Qué pasa si subo los precios 5 % y vendo 3 % menos?»

- Mostrar el cambio en la utilidad y la tabla de **sensibilidad ±10 %**: qué variable pesa más.
- **Guardar escenario** para compararlo después.

### 10:30 – 12:00 · Diagnóstico empresarial

**Diagnóstico empresarial**:

- Puntaje de salud y **semáforo** por indicador (rentabilidad, solidez, crecimiento, riesgo comercial).
- Recomendaciones **con cifras** calculadas por el simulador (por ejemplo, cuánto subir el precio para llegar a la meta).
- Señalar el hallazgo de **productos vendidos con pérdida** (el agua mineral).

### 12:00 – 12:30 · Informes

**Informes y reportes** → **Generar informe…** → PDF del diagnóstico o del estado de resultados.
Abrirlo: está listo para entregar al banco o al contador. También se exporta a Excel.

### 12:30 – 14:00 · El mismo motor en el campo

Cambiar la empresa activa a **Fundo Demo** (sector Agropecuario): aparece la sección **Agropecuario**.

- **Ganadería**: hato de vacunos, alpacas y ovinos; natalidad y mortalidad de crías con semáforo.
- **Agricultura**: campaña de papa en *Parcela Alta*, costos por rubro, rendimiento frente al promedio nacional.
- **Visitas de inspección**: visita de la *Ing. Rosa Quispe*, compromisos cumplidos y no cumplidos →
  **Informe técnico (PDF)** con firmas.
- «Lo validamos con un ingeniero agrario, que nos pidió poder corregir los registros: eso ya está en esta versión.»

### 14:00 – 15:00 · Calidad y cierre

- **302 pruebas automáticas**, 14 migraciones, copias de seguridad diarias con restauración en dos pasos.
- **Instalador** `.exe` con Java incluido: el usuario no instala nada más.
- Cierre: «Business Analyzer convierte registros diarios en decisiones. Gracias.»

---

## 3. Preguntas frecuentes del jurado

| Pregunta | Respuesta corta |
|---|---|
| ¿Por qué SQLite y no MySQL/PostgreSQL? | Es una app de escritorio para un negocio pequeño: un archivo local, sin servidor ni instalación, con transacciones ACID. Las copias de seguridad son copiar un archivo (`VACUUM INTO`). |
| ¿Por qué no usaron Spring ni JPA? | Requisito del proyecto: JDBC puro para controlar el SQL y mostrar cada capa. La inyección de dependencias se hace por constructor en `AppContextFGEM`. |
| ¿Cómo guardan el dinero sin errores de redondeo? | `BigDecimal` en Java y enteros ×10 000 en SQLite (`util.MontosFGEM`). Nunca `double` para dinero. |
| ¿Cómo eligen el método de predicción? | Validación *rolling-origin*: cada método pronostica meses que ya ocurrieron y se elige el de menor error medio absoluto (MAE). |
| ¿Qué pasa si dos personas usan el sistema? | Cada usuario tiene su rol y solo ve las empresas asignadas. Es monousuario por equipo; una versión en red sería trabajo futuro. |
| ¿Cómo protegen las contraseñas? | PBKDF2-HMAC-SHA256 con sal, política de contraseñas, bloqueo tras 5 intentos y registro de accesos. |
| ¿Cómo probaron el sistema? | 302 pruebas JUnit 5: cálculos puros con datos en memoria y servicios con una base SQLite temporal y un reloj controlado. |
| ¿Por qué no se pueden borrar ventas? | Por trazabilidad: se **anulan** con motivo y usuario; corregir = anular + registrar en una transacción. |
| ¿Qué falta? | Fórmulas rurales específicas del ingeniero agrario, versión multiusuario en red y Holt-Winters estacional con 24+ meses de datos. |

---

## 4. Si algo sale mal

| Problema | Qué hacer |
|---|---|
| La app no abre | Usar el `.zip` portable de la USB: descomprimir y abrir `Business Analyzer.exe`. |
| El informe PDF tarda | Es la primera compilación; seguir hablando, aparece en segundos (por eso el paso 6 de la preparación). |
| Se olvidó la contraseña | Entrar con otro administrador → **Usuarios y permisos** → restablecer contraseña. |
| Falla todo | Pasar el video grabado (plan B) y explicar sobre él. |
