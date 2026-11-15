# Diagramas UML — Business Analyzer Desktop 1.1

Los diagramas están escritos en [PlantUML](https://plantuml.com) (`.puml`), así que se versionan
junto al código. Las imágenes están en `png/` (para diapositivas) y `svg/` (para el informe escrito).

| # | Diagrama | Tipo UML | Qué muestra |
|---|---|---|---|
| 01 | [Casos de uso](01-casos-de-uso.puml) | Casos de uso | Actores por rol y lo que puede hacer cada uno |
| 02 | [Arquitectura](02-arquitectura.puml) | Componentes | Capas: presentación, lógica de negocio y datos |
| 03 | [Modelo de dominio](03-clases-dominio.puml) | Clases | Entidades del paquete `model` y sus relaciones |
| 04 | [Motor de análisis](04-clases-analisis.puml) | Clases | Paquete `analytics` y los servicios que lo usan (patrón Estrategia en predicciones) |
| 05 | [Registrar una venta](05-secuencia-registrar-venta.puml) | Secuencia | Permisos, validación, transacción, stock insuficiente y corrección |
| 06 | [Diagnóstico](06-secuencia-diagnostico.puml) | Secuencia | Cómo se reúnen los datos y se evalúan los indicadores |
| 07 | [Estados de una campaña](07-estados-campania.puml) | Máquina de estados | En curso, cosechada, perdida y anulada |
| 08 | [Modelo de datos](08-modelo-datos.puml) | Entidad-relación | Las 30 tablas de SQLite (migraciones V1–V12) |
| 09 | [Despliegue](09-despliegue.puml) | Despliegue | Instalación en Windows y carpeta de datos |

## Regenerar las imágenes

No hace falta instalar Graphviz: los diagramas usan el motor `smetana` incluido en PlantUML.

```powershell
.\docs\uml\generar.ps1
```

El script descarga PlantUML con el wrapper de Maven la primera vez y genera `png/` y `svg/`.

En IntelliJ IDEA también se pueden ver al instante con el plugin **PlantUML Integration**.
