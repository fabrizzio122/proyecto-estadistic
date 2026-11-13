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
├── model         entidades (desde la Fase 2)
├── analytics     motores financiero, estadístico y predictivo (desde la Fase 7)
├── exception     errores de datos y de validación
└── ui            navegación, notificaciones, diálogos y componentes visuales
```

El esquema de la base de datos se versiona con scripts en
`src/main/resources/db/migration` (`V1__...sql`, `V2__...sql`...), que se aplican
automáticamente al iniciar y dentro de una transacción.

## Fases

1. Base: estructura, SQLite y migraciones, ventana principal y tema oscuro ✅
2. Usuarios, roles y permisos
3. Empresas
4. Productos y servicios
5. Ventas y compras
6. Gestión financiera
7. Análisis financiero
8. Análisis estadístico
9. Predicciones
10. Simulación de escenarios
11. Diagnóstico empresarial
12. Informes (PDF y Excel)
13. Configuración, copias de seguridad e instalador
