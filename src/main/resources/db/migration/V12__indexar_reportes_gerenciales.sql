SET @existe_idx_pedidos_cancelacion = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'pedidos'
      AND index_name = 'idx_pedidos_cancelacion_eliminado'
);

SET @crear_idx_pedidos_cancelacion = IF(
    @existe_idx_pedidos_cancelacion = 0,
    'CREATE INDEX idx_pedidos_cancelacion_eliminado ON pedidos (fecha_cancelacion, eliminado)',
    'SELECT 1'
);
PREPARE crear_idx_pedidos_cancelacion_stmt FROM @crear_idx_pedidos_cancelacion;
EXECUTE crear_idx_pedidos_cancelacion_stmt;
DEALLOCATE PREPARE crear_idx_pedidos_cancelacion_stmt;

SET @existe_idx_movimientos_fecha = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'movimientos_inventarios'
      AND index_name = 'idx_movimientos_fecha_eliminado'
);

SET @crear_idx_movimientos_fecha = IF(
    @existe_idx_movimientos_fecha = 0,
    'CREATE INDEX idx_movimientos_fecha_eliminado ON movimientos_inventarios (fecha_movimiento, eliminado)',
    'SELECT 1'
);
PREPARE crear_idx_movimientos_fecha_stmt FROM @crear_idx_movimientos_fecha;
EXECUTE crear_idx_movimientos_fecha_stmt;
DEALLOCATE PREPARE crear_idx_movimientos_fecha_stmt;
