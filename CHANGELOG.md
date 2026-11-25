# Cambios

## Sin publicar

### Nuevo
- **Lista de compras** (*Sugerencias → Reposición → Lista de compras*): los productos por reponer agrupados por
  categoría, con el estado, el stock, la cantidad a comprar y el costo aproximado, con subtotales y total. En PDF para
  imprimir o en Excel para ajustar cantidades (el costo y el total se recalculan solos); queda en el historial de informes.
- **Sugerencias por producto** (*Análisis → Sugerencias*): qué precios ajustar (productos con pérdida o con menos
  margen que el objetivo, con el precio sugerido y la ganancia extra al mes), qué reponer y cuánto comprar para dos
  semanas según las ventas recientes, y qué productos tienen dinero inmovilizado porque no se venden.
- **Productos perecibles**: nueva casilla en el producto (y columna «Perecible» al importar). En las sugerencias se
  reponen para 3 días y, si tardarían más de 15 días en venderse, aparecen primero con la indicación de ofrecerlos
  ya en promoción.
- Sugerencias afinadas tras probarlas con una bodega real: la venta lenta se mide con 180 días y se separa de los
  productos sin ventas, con el % del inventario; un producto agotado sin ventas recientes se repone según su
  historial; los precios de productos que casi no se venden van al final en gris; la ganancia extra indica el
  supuesto («si vendes la misma cantidad») y cuánto reduce la pérdida mensual; «1 día» y ritmos como «1 en 60 días».
- **Importar desde Excel** (*Gestión → Importar desde Excel*): plantilla con instrucciones y listas desplegables
  (también con tres meses de ejemplo de una bodega), revisión completa del archivo sin guardar nada y luego
  importación de productos, categorías, ventas, compras y gastos a la empresa activa. Si alguna fila tiene un
  error no se guarda nada y se indica la hoja y la fila. Antes de importar se guarda una copia de seguridad.
- Volver a importar un archivo (o una versión más completa) **no duplica datos**: las boletas y comprobantes ya
  registrados, un segundo inventario inicial y los gastos idénticos se marcan como error, y los productos se
  reconocen por código o por nombre.

### Corregido
- **Activos y deudas**: con el activo total en cero o negativo (por ejemplo, caja negativa porque no se registró el
  aporte inicial) ya no se calculan la rentabilidad de los activos ni el endeudamiento, que daban valores engañosos
  (una pérdida ÷ activos negativos salía como rentabilidad positiva). Se muestra «No aplica» (el motivo, en la ayuda)
  y se oculta la columna «% del activo» del balance. La lectura rápida explica que la caja está en rojo y qué registrar.
- **Diagnóstico**: la liquidez y el endeudamiento ya no dicen «Sin datos» cuando hay cuentas en Tesorería; el dinero de
  las cuentas cuenta como activo. Con activos negativos muestran «No aplica» con la explicación.
- **Simulación de escenarios**: los promedios mensuales y los resultados ya no se redondean a céntimos antes de
  calcular; con márgenes pequeños eso movía el punto de equilibrio varios soles respecto del diagnóstico. El punto
  de equilibrio se calcula en una sola división (fijos × ventas ÷ margen).
- **Valoración**: la utilidad de la tarjeta dice «Utilidad anualizada» e indica cuántos meses se llevaron a 12.
- **Análisis financiero**: el color de la variación del costo de ventas se juzga por su % sobre las ventas (si se
  vende menos, el costo baja sin que sea una mejora). Las etiquetas de los meses en los gráficos ya no se enciman.

### Calidad
- **Integración continua con GitHub Actions**: en cada push o pull request a `main` se compila el proyecto
  en Windows con JDK 17 y se ejecutan las pruebas (329); el estado se ve en el distintivo del README.
- **Cobertura de pruebas con JaCoCo**: la lógica de negocio (todo menos la interfaz) tiene 92 % de líneas
  cubiertas; informe en `target/site/jacoco/index.html` y resumen por capa en cada ejecución de GitHub.

## 1.2.1 — 16/11/2026

### Corregido
- **Simulación de escenarios**: con todos los supuestos en 0 % el escenario ahora da exactamente la utilidad de la
  base (antes difería unos céntimos porque se releía la tasa de impuesto redondeada que muestra el campo).
- Los textos de ayuda de los formularios bajan de línea en lugar de cortarse con «…», y las etiquetas largas
  («Rendimiento de referencia», «Valor de adquisición») ya no se recortan.

### Documentación
- **Informe final** (`docs/informe-final.docx` / `.pdf`) y **Manual de usuario** (`docs/manual-usuario.docx` / `.pdf`)
  con 66 capturas de la aplicación (`docs/manual/capturas`).
- Guion de la demostración actualizado a la versión 1.2 (302 pruebas, 14 migraciones).

## 1.2.0 — 16/11/2026

### Nuevo
- **Tesorería**: cuentas de dinero (caja, bancos, billeteras digitales) cuyo saldo se calcula solo, transferencias
  entre cuentas y aportes o retiros del dueño.
- **Ventas y compras al crédito**: quedan por cobrar o por pagar, con fecha de vencimiento y cobros o pagos parciales.
- Cada venta, compra, ingreso, gasto, compra de activo, préstamo y pago de deuda indica la cuenta por la que se movió
  el dinero.
- **Pago de cuotas con capital e intereses**: los intereses se registran solos como gasto financiero.
- **Estado de flujo de efectivo** (método directo) y evolución mensual de entradas, salidas y dinero disponible.
- **Proyección de caja** a 6 meses, con escenario pesimista y aviso del mes en que faltaría dinero.
- Diagnóstico: nuevo indicador «Caja proyectada». Dashboard: tarjeta «Dinero disponible».

### Mejorado
- El balance general toma el efectivo de las cuentas y suma las ventas y compras al crédito pendientes.
- La Bodega Demo usa caja, banco y Yape, deposita la venta dos veces al mes y tiene ventas y compras al crédito.

### Base de datos
- Migración V13 (tesorería). «Caja y bancos» pasa a una cuenta «Caja» con el mismo saldo; los datos existentes se conservan.

## 1.1.0 — 2026-11-15

### Nuevo
- **Activos, deudas y patrimonio**: balance general, depreciación en línea recta, pagos de deudas,
  liquidez y endeudamiento (también en el diagnóstico).
- **Sector agropecuario** (solo empresas de ese sector):
  - **Ganadería**: hato de vacunos, ovinos, alpacas y llamas; nacimientos, muertes, compras, ventas,
    producción de leche, fibra y lana; indicadores técnicos y valor del hato.
  - **Agricultura**: parcelas, campañas, costos por rubro, cosechas, rendimiento frente a la referencia
    nacional y margen por campaña.
  - **Visitas de inspección**: aspectos evaluados, compromisos, fotos e **informe técnico en PDF**.
  - Las ventas de animales, producción y cosechas entran en los análisis financieros, estadísticos y en el
    diagnóstico (4 indicadores agropecuarios).
- **Valoración del negocio**: método patrimonial, capitalización de utilidades y flujos descontados, con
  sensibilidad e informe PDF.
- **Dashboard** rediseñado: indicadores del mes en curso y acceso directo a cada módulo.
- **Editar registros**: ventas, compras, movimientos de ganado, producción, costos y cosechas se pueden
  corregir. La corrección anula el original (queda como historial) y registra el nuevo en una sola transacción.
- Las **copias de seguridad** incluyen las fotos de las visitas (archivo `.zip`); se siguen aceptando las `.db` de la 1.0.
- Documentación del proyecto: diagramas UML (`docs/uml`) y guion de la demostración (`docs/guion-demo.md`).

### Mejorado
- El mes en curso se compara con los mismos días del mes anterior (antes, con el mes completo).
- La empresa de demostración «Bodega Demo» registra una venta por día con estacionalidad realista;
  nueva empresa de demostración «Fundo Demo» para el sector agropecuario.

### Base de datos
- Migraciones nuevas V9 (patrimonio), V10 (ganadería), V11 (agricultura) y V12 (visitas). Se aplican solas
  al abrir una base de la versión 1.0; los datos existentes se conservan.

## 1.0.0 — 2026-11-14

Primera versión: usuarios y roles, empresas, productos y servicios, ventas y compras, gestión financiera,
análisis financiero, estadística, predicciones, simulación de escenarios, diagnóstico empresarial,
informes PDF/Excel, configuración, copias de seguridad e instalador.
