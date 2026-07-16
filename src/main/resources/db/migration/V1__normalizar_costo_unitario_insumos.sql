-- Corrige instalaciones existentes donde Hibernate agregó costo_unitario como
-- nullable antes de intentar convertirla al tipo definitivo NOT NULL.
-- La sentencia es condicional para permitir también bases de datos nuevas.

SET @existe_costo_unitario = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'insumos'
      AND column_name = 'costo_unitario'
);

SET @normalizar_costo_unitario = IF(
    @existe_costo_unitario > 0,
    'UPDATE insumos SET costo_unitario = 0.0000 WHERE costo_unitario IS NULL',
    'SELECT 1'
);

PREPARE normalizar_costo_unitario_stmt FROM @normalizar_costo_unitario;
EXECUTE normalizar_costo_unitario_stmt;
DEALLOCATE PREPARE normalizar_costo_unitario_stmt;
