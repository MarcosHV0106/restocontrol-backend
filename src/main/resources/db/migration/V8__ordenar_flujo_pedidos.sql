SET @id_mesa_requerida = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pedidos'
      AND column_name = 'id_mesa'
      AND is_nullable = 'NO'
);

SET @permitir_pedido_sin_mesa = IF(
    @id_mesa_requerida > 0,
    'ALTER TABLE pedidos MODIFY COLUMN id_mesa INT NULL',
    'SELECT 1'
);

PREPARE permitir_pedido_sin_mesa_stmt FROM @permitir_pedido_sin_mesa;
EXECUTE permitir_pedido_sin_mesa_stmt;
DEALLOCATE PREPARE permitir_pedido_sin_mesa_stmt;

SET @existe_fecha_envio_cocina = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND column_name = 'fecha_envio_cocina'
);
SET @crear_fecha_envio_cocina = IF(
    @existe_fecha_envio_cocina = 0,
    'ALTER TABLE pedidos ADD COLUMN fecha_envio_cocina DATETIME(6) NULL',
    'SELECT 1'
);
PREPARE crear_fecha_envio_cocina_stmt FROM @crear_fecha_envio_cocina;
EXECUTE crear_fecha_envio_cocina_stmt;
DEALLOCATE PREPARE crear_fecha_envio_cocina_stmt;

SET @existe_fecha_solicitud_cuenta = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND column_name = 'fecha_solicitud_cuenta'
);
SET @crear_fecha_solicitud_cuenta = IF(
    @existe_fecha_solicitud_cuenta = 0,
    'ALTER TABLE pedidos ADD COLUMN fecha_solicitud_cuenta DATETIME(6) NULL',
    'SELECT 1'
);
PREPARE crear_fecha_solicitud_cuenta_stmt FROM @crear_fecha_solicitud_cuenta;
EXECUTE crear_fecha_solicitud_cuenta_stmt;
DEALLOCATE PREPARE crear_fecha_solicitud_cuenta_stmt;

SET @existe_fecha_cancelacion = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND column_name = 'fecha_cancelacion'
);
SET @crear_fecha_cancelacion = IF(
    @existe_fecha_cancelacion = 0,
    'ALTER TABLE pedidos ADD COLUMN fecha_cancelacion DATETIME(6) NULL',
    'SELECT 1'
);
PREPARE crear_fecha_cancelacion_stmt FROM @crear_fecha_cancelacion;
EXECUTE crear_fecha_cancelacion_stmt;
DEALLOCATE PREPARE crear_fecha_cancelacion_stmt;

SET @existe_motivo_cancelacion = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND column_name = 'motivo_cancelacion'
);
SET @crear_motivo_cancelacion = IF(
    @existe_motivo_cancelacion = 0,
    'ALTER TABLE pedidos ADD COLUMN motivo_cancelacion VARCHAR(200) NULL',
    'SELECT 1'
);
PREPARE crear_motivo_cancelacion_stmt FROM @crear_motivo_cancelacion;
EXECUTE crear_motivo_cancelacion_stmt;
DEALLOCATE PREPARE crear_motivo_cancelacion_stmt;

SET @existe_cliente_nombre = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND column_name = 'cliente_nombre'
);
SET @crear_cliente_nombre = IF(
    @existe_cliente_nombre = 0,
    'ALTER TABLE pedidos ADD COLUMN cliente_nombre VARCHAR(120) NULL',
    'SELECT 1'
);
PREPARE crear_cliente_nombre_stmt FROM @crear_cliente_nombre;
EXECUTE crear_cliente_nombre_stmt;
DEALLOCATE PREPARE crear_cliente_nombre_stmt;

SET @existe_cliente_telefono = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND column_name = 'cliente_telefono'
);
SET @crear_cliente_telefono = IF(
    @existe_cliente_telefono = 0,
    'ALTER TABLE pedidos ADD COLUMN cliente_telefono VARCHAR(20) NULL',
    'SELECT 1'
);
PREPARE crear_cliente_telefono_stmt FROM @crear_cliente_telefono;
EXECUTE crear_cliente_telefono_stmt;
DEALLOCATE PREPARE crear_cliente_telefono_stmt;

SET @existe_direccion_entrega = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND column_name = 'direccion_entrega'
);
SET @crear_direccion_entrega = IF(
    @existe_direccion_entrega = 0,
    'ALTER TABLE pedidos ADD COLUMN direccion_entrega VARCHAR(250) NULL',
    'SELECT 1'
);
PREPARE crear_direccion_entrega_stmt FROM @crear_direccion_entrega;
EXECUTE crear_direccion_entrega_stmt;
DEALLOCATE PREPARE crear_direccion_entrega_stmt;

INSERT INTO modalidades_pedidos (nombre, eliminado)
SELECT 'Delivery', 0
WHERE NOT EXISTS (
    SELECT 1 FROM modalidades_pedidos
    WHERE UPPER(nombre) IN ('DELIVERY', 'REPARTO') AND eliminado = 0
);

INSERT INTO estados_pedidos (nombre, eliminado)
SELECT 'CANCELADO', 0
WHERE NOT EXISTS (
    SELECT 1 FROM estados_pedidos
    WHERE UPPER(nombre) = 'CANCELADO' AND eliminado = 0
);

UPDATE pedidos pedido
JOIN estados_pedidos estado ON estado.id_estado_pedido = pedido.id_estado_pedido
SET pedido.fecha_envio_cocina = COALESCE(pedido.fecha_envio_cocina, pedido.fecha)
WHERE pedido.eliminado = 0
  AND UPPER(estado.nombre) NOT IN ('PAGADO', 'COBRADO', 'CANCELADO');

UPDATE pedidos pedido
JOIN estados_pedidos estado_pedido ON estado_pedido.id_estado_pedido = pedido.id_estado_pedido
JOIN mesas mesa ON mesa.id_mesa = pedido.id_mesa
JOIN estados_mesas estado_mesa ON estado_mesa.id_estado_mesa = mesa.id_estado_mesa
SET pedido.fecha_solicitud_cuenta = COALESCE(
    pedido.fecha_solicitud_cuenta,
    pedido.fecha_entregado,
    pedido.fecha
)
WHERE pedido.eliminado = 0
  AND LOWER(estado_mesa.descripcion) = 'cobrar'
  AND UPPER(estado_pedido.nombre) NOT IN ('PAGADO', 'COBRADO', 'CANCELADO');

SET @existe_idx_pedidos_flujo = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'pedidos' AND index_name = 'idx_pedidos_flujo'
);
SET @crear_idx_pedidos_flujo = IF(
    @existe_idx_pedidos_flujo = 0,
    'CREATE INDEX idx_pedidos_flujo ON pedidos (fecha_envio_cocina, fecha_solicitud_cuenta, id_estado_pedido, eliminado)',
    'SELECT 1'
);
PREPARE crear_idx_pedidos_flujo_stmt FROM @crear_idx_pedidos_flujo;
EXECUTE crear_idx_pedidos_flujo_stmt;
DEALLOCATE PREPARE crear_idx_pedidos_flujo_stmt;
