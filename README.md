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
6. Gestión financiera
7. Análisis financiero
8. Análisis estadístico
9. Predicciones
10. Simulación de escenarios
11. Diagnóstico empresarial
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
