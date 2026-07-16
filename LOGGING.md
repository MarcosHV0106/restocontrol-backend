# Logging del backend

El backend usa SLF4J con Logback mediante la configuración de
`src/main/resources/logback-spring.xml`.

## Archivos generados

Por defecto se crean dentro de `logs/`:

- `restocontrol.log`: actividad general, peticiones y eventos de la aplicación.
- `restocontrol-error.log`: únicamente eventos de nivel `ERROR`.
- `restocontrol-audit.log`: operaciones de servicio que modifican información.
- `archive/`: históricos diarios o archivos que superaron el tamaño máximo,
  comprimidos en formato `.gz`.

La carpeta está excluida de Git.

## Trazabilidad HTTP

Cada petición acepta opcionalmente `X-Request-ID`. Si no se recibe un valor
seguro, el backend genera un UUID. El mismo identificador aparece:

- en el encabezado `X-Request-ID` de la respuesta;
- en todas las líneas de log generadas durante la petición;
- en el campo `requestId` de las respuestas de error controladas.

Los parámetros de consulta y cuerpos HTTP no se registran. Los tokens incluidos
en rutas de activación se reemplazan por `[REDACTED]`.

## Variables de entorno

| Variable | Valor predeterminado | Uso |
| --- | --- | --- |
| `LOG_PATH` | `logs` | Directorio de salida |
| `ROOT_LOG_LEVEL` | `INFO` | Nivel raíz |
| `APP_LOG_LEVEL` | `INFO` | Nivel del código de RestoControl |
| `SERVICE_LOG_LEVEL` | `INFO` | Cambiar a `DEBUG` para tiempos de todas las lecturas de servicios |
| `SECURITY_LOG_LEVEL` | `WARN` | Nivel interno de Spring Security |
| `HIBERNATE_SQL_LOG_LEVEL` | `WARN` | SQL de Hibernate |
| `HIBERNATE_BIND_LOG_LEVEL` | `WARN` | Parámetros JDBC; no habilitar en producción |
| `HTTP_REQUEST_LOG_ENABLED` | `true` | Activar o desactivar el log HTTP |
| `REQUEST_ID_HEADER` | `X-Request-ID` | Nombre del encabezado de correlación |
| `LOG_MAX_FILE_SIZE` | `20MB` | Tamaño máximo antes de rotar |
| `LOG_MAX_HISTORY` | `30` | Días para logs generales y errores |
| `AUDIT_LOG_MAX_HISTORY` | `90` | Días para auditoría |
| `LOG_TOTAL_SIZE_CAP` | `2GB` | Límite total por grupo de históricos |

En producción deben mantenerse desactivados el cuerpo de las peticiones, los
tokens, las contraseñas y los parámetros JDBC dentro de los logs.
