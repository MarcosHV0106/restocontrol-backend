SET @existe_fecha_consumo_inventario = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pedidos'
      AND column_name = 'fecha_consumo_inventario'
);

SET @crear_fecha_consumo_inventario = IF(
    @existe_fecha_consumo_inventario = 0,
    'ALTER TABLE pedidos ADD COLUMN fecha_consumo_inventario DATETIME(6) NULL',
    'SELECT 1'
);

PREPARE crear_fecha_consumo_inventario_stmt FROM @crear_fecha_consumo_inventario;
EXECUTE crear_fecha_consumo_inventario_stmt;
DEALLOCATE PREPARE crear_fecha_consumo_inventario_stmt;

SET @existe_id_pedido_movimiento = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'movimientos_inventarios'
      AND column_name = 'id_pedido'
);

SET @crear_id_pedido_movimiento = IF(
    @existe_id_pedido_movimiento = 0,
    'ALTER TABLE movimientos_inventarios ADD COLUMN id_pedido INT NULL',
    'SELECT 1'
);

PREPARE crear_id_pedido_movimiento_stmt FROM @crear_id_pedido_movimiento;
EXECUTE crear_id_pedido_movimiento_stmt;
DEALLOCATE PREPARE crear_id_pedido_movimiento_stmt;

SET @existe_fk_movimientos_pedido = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'movimientos_inventarios'
      AND constraint_name = 'fk_movimientos_pedido'
      AND constraint_type = 'FOREIGN KEY'
);

SET @crear_fk_movimientos_pedido = IF(
    @existe_fk_movimientos_pedido = 0,
    'ALTER TABLE movimientos_inventarios ADD CONSTRAINT fk_movimientos_pedido FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido)',
    'SELECT 1'
);

PREPARE crear_fk_movimientos_pedido_stmt FROM @crear_fk_movimientos_pedido;
EXECUTE crear_fk_movimientos_pedido_stmt;
DEALLOCATE PREPARE crear_fk_movimientos_pedido_stmt;

SET @existe_idx_movimientos_pedido = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'movimientos_inventarios'
      AND index_name = 'idx_movimientos_pedido'
);

SET @crear_idx_movimientos_pedido = IF(
    @existe_idx_movimientos_pedido = 0,
    'CREATE INDEX idx_movimientos_pedido ON movimientos_inventarios (id_pedido, fecha_movimiento)',
    'SELECT 1'
);

PREPARE crear_idx_movimientos_pedido_stmt FROM @crear_idx_movimientos_pedido;
EXECUTE crear_idx_movimientos_pedido_stmt;
DEALLOCATE PREPARE crear_idx_movimientos_pedido_stmt;

UPDATE insumos insumo
SET insumo.stock_actual = (
    SELECT COALESCE(SUM(lote.cantidad_actual), 0)
    FROM lotes_insumos lote
    WHERE lote.id_insumo = insumo.id_insumo
      AND lote.eliminado = 0
      AND UPPER(lote.estado) = 'ACTIVO'
      AND lote.cantidad_actual > 0
      AND (
          lote.fecha_vencimiento IS NULL
          OR lote.fecha_vencimiento >= DATE(DATE_SUB(UTC_TIMESTAMP(), INTERVAL 5 HOUR))
      )
)
WHERE insumo.eliminado = 0;
