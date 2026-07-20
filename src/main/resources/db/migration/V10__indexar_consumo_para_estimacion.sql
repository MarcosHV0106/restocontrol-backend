SET @existe_idx_pedidos_consumo = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'pedidos'
      AND index_name = 'idx_pedidos_consumo_eliminado'
);

SET @crear_idx_pedidos_consumo = IF(
    @existe_idx_pedidos_consumo = 0,
    'CREATE INDEX idx_pedidos_consumo_eliminado ON pedidos (fecha_consumo_inventario, eliminado)',
    'SELECT 1'
);

PREPARE crear_idx_pedidos_consumo_stmt FROM @crear_idx_pedidos_consumo;
EXECUTE crear_idx_pedidos_consumo_stmt;
DEALLOCATE PREPARE crear_idx_pedidos_consumo_stmt;
