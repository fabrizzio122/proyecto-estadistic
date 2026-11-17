# Cambios

## Sin publicar

### Calidad
- **Integración continua con GitHub Actions**: en cada push o pull request a `main` se compila el proyecto
  en Windows con JDK 17 y se ejecutan las 302 pruebas; el estado se ve en el distintivo del README.
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
