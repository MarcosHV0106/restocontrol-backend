SET @existe_bloqueado_cocina = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'alimentos'
      AND column_name = 'bloqueado_cocina'
);
SET @crear_bloqueado_cocina = IF(
    @existe_bloqueado_cocina = 0,
    'ALTER TABLE alimentos ADD COLUMN bloqueado_cocina BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1'
);
PREPARE crear_bloqueado_cocina_stmt FROM @crear_bloqueado_cocina;
EXECUTE crear_bloqueado_cocina_stmt;
DEALLOCATE PREPARE crear_bloqueado_cocina_stmt;

SET @existe_motivo_bloqueo_cocina = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'alimentos'
      AND column_name = 'motivo_bloqueo_cocina'
);
SET @crear_motivo_bloqueo_cocina = IF(
    @existe_motivo_bloqueo_cocina = 0,
    'ALTER TABLE alimentos ADD COLUMN motivo_bloqueo_cocina VARCHAR(200) NULL',
    'SELECT 1'
);
PREPARE crear_motivo_bloqueo_cocina_stmt FROM @crear_motivo_bloqueo_cocina;
EXECUTE crear_motivo_bloqueo_cocina_stmt;
DEALLOCATE PREPARE crear_motivo_bloqueo_cocina_stmt;

SET @existe_fecha_bloqueo_cocina = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'alimentos'
      AND column_name = 'fecha_bloqueo_cocina'
);
SET @crear_fecha_bloqueo_cocina = IF(
    @existe_fecha_bloqueo_cocina = 0,
    'ALTER TABLE alimentos ADD COLUMN fecha_bloqueo_cocina DATETIME(6) NULL',
    'SELECT 1'
);
PREPARE crear_fecha_bloqueo_cocina_stmt FROM @crear_fecha_bloqueo_cocina;
EXECUTE crear_fecha_bloqueo_cocina_stmt;
DEALLOCATE PREPARE crear_fecha_bloqueo_cocina_stmt;

SET @existe_usuario_bloqueo_cocina = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'alimentos'
      AND column_name = 'id_usuario_bloqueo_cocina'
);
SET @crear_usuario_bloqueo_cocina = IF(
    @existe_usuario_bloqueo_cocina = 0,
    'ALTER TABLE alimentos ADD COLUMN id_usuario_bloqueo_cocina INT NULL',
    'SELECT 1'
);
PREPARE crear_usuario_bloqueo_cocina_stmt FROM @crear_usuario_bloqueo_cocina;
EXECUTE crear_usuario_bloqueo_cocina_stmt;
DEALLOCATE PREPARE crear_usuario_bloqueo_cocina_stmt;

SET @existe_fk_alimento_usuario_bloqueo_cocina = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'alimentos'
      AND constraint_name = 'fk_alimento_usuario_bloqueo_cocina'
      AND constraint_type = 'FOREIGN KEY'
);
SET @crear_fk_alimento_usuario_bloqueo_cocina = IF(
    @existe_fk_alimento_usuario_bloqueo_cocina = 0,
    'ALTER TABLE alimentos ADD CONSTRAINT fk_alimento_usuario_bloqueo_cocina FOREIGN KEY (id_usuario_bloqueo_cocina) REFERENCES usuarios (id_usuario)',
    'SELECT 1'
);
PREPARE crear_fk_alimento_usuario_bloqueo_cocina_stmt FROM @crear_fk_alimento_usuario_bloqueo_cocina;
EXECUTE crear_fk_alimento_usuario_bloqueo_cocina_stmt;
DEALLOCATE PREPARE crear_fk_alimento_usuario_bloqueo_cocina_stmt;

SET @existe_idx_alimentos_bloqueo_cocina = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'alimentos'
      AND index_name = 'idx_alimentos_bloqueo_cocina'
);
SET @crear_idx_alimentos_bloqueo_cocina = IF(
    @existe_idx_alimentos_bloqueo_cocina = 0,
    'CREATE INDEX idx_alimentos_bloqueo_cocina ON alimentos (bloqueado_cocina, eliminado)',
    'SELECT 1'
);
PREPARE crear_idx_alimentos_bloqueo_cocina_stmt FROM @crear_idx_alimentos_bloqueo_cocina;
EXECUTE crear_idx_alimentos_bloqueo_cocina_stmt;
DEALLOCATE PREPARE crear_idx_alimentos_bloqueo_cocina_stmt;

SET @existe_idx_pedidos_historial_cocina = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'pedidos'
      AND index_name = 'idx_pedidos_historial_cocina'
);
SET @crear_idx_pedidos_historial_cocina = IF(
    @existe_idx_pedidos_historial_cocina = 0,
    'CREATE INDEX idx_pedidos_historial_cocina ON pedidos (fecha_entregado, eliminado)',
    'SELECT 1'
);
PREPARE crear_idx_pedidos_historial_cocina_stmt FROM @crear_idx_pedidos_historial_cocina;
EXECUTE crear_idx_pedidos_historial_cocina_stmt;
DEALLOCATE PREPARE crear_idx_pedidos_historial_cocina_stmt;
