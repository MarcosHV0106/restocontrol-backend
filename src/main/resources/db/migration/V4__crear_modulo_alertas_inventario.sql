SET @existe_fecha_vencimiento = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'insumos'
      AND column_name = 'fecha_vencimiento'
);

SET @crear_fecha_vencimiento = IF(
    @existe_fecha_vencimiento = 0,
    'ALTER TABLE insumos ADD COLUMN fecha_vencimiento DATE NULL',
    'SELECT 1'
);
PREPARE crear_fecha_vencimiento_stmt FROM @crear_fecha_vencimiento;
EXECUTE crear_fecha_vencimiento_stmt;
DEALLOCATE PREPARE crear_fecha_vencimiento_stmt;

ALTER TABLE insumos
    MODIFY COLUMN stock_actual DECIMAL(12,4) NULL,
    MODIFY COLUMN stock_minimo DECIMAL(12,4) NULL;

CREATE TABLE IF NOT EXISTS lotes_insumos (
    id_lote INT NOT NULL AUTO_INCREMENT,
    id_insumo INT NOT NULL,
    codigo VARCHAR(60) NOT NULL,
    cantidad_inicial DECIMAL(12,4) NOT NULL,
    cantidad_actual DECIMAL(12,4) NOT NULL,
    fecha_ingreso DATE NOT NULL,
    fecha_vencimiento DATE NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id_lote),
    CONSTRAINT uq_lotes_insumos_codigo UNIQUE (codigo),
    CONSTRAINT fk_lotes_insumos_insumo FOREIGN KEY (id_insumo) REFERENCES insumos (id_insumo),
    INDEX idx_lotes_insumos_insumo (id_insumo),
    INDEX idx_lotes_insumos_vencimiento (fecha_vencimiento),
    INDEX idx_lotes_insumos_estado (estado, eliminado)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO lotes_insumos (
    id_insumo, codigo, cantidad_inicial, cantidad_actual,
    fecha_ingreso, fecha_vencimiento, estado, eliminado
)
SELECT
    i.id_insumo,
    CONCAT('LEGACY-', i.id_insumo),
    COALESCE(i.stock_actual, 0),
    COALESCE(i.stock_actual, 0),
    DATE(DATE_SUB(UTC_TIMESTAMP(), INTERVAL 5 HOUR)),
    i.fecha_vencimiento,
    CASE WHEN COALESCE(i.stock_actual, 0) > 0 THEN 'ACTIVO' ELSE 'AGOTADO' END,
    0
FROM insumos i
WHERE COALESCE(i.stock_actual, 0) > 0
  AND NOT EXISTS (
      SELECT 1 FROM lotes_insumos l WHERE l.id_insumo = i.id_insumo
  );

SET @existe_id_lote_movimiento = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'movimientos_inventarios'
      AND column_name = 'id_lote'
);
SET @crear_id_lote_movimiento = IF(
    @existe_id_lote_movimiento = 0,
    'ALTER TABLE movimientos_inventarios ADD COLUMN id_lote INT NULL, ADD CONSTRAINT fk_movimientos_lote FOREIGN KEY (id_lote) REFERENCES lotes_insumos (id_lote)',
    'SELECT 1'
);
PREPARE crear_id_lote_movimiento_stmt FROM @crear_id_lote_movimiento;
EXECUTE crear_id_lote_movimiento_stmt;
DEALLOCATE PREPARE crear_id_lote_movimiento_stmt;

SET @existe_id_usuario_movimiento = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'movimientos_inventarios'
      AND column_name = 'id_usuario'
);
SET @crear_id_usuario_movimiento = IF(
    @existe_id_usuario_movimiento = 0,
    'ALTER TABLE movimientos_inventarios ADD COLUMN id_usuario INT NULL, ADD CONSTRAINT fk_movimientos_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)',
    'SELECT 1'
);
PREPARE crear_id_usuario_movimiento_stmt FROM @crear_id_usuario_movimiento;
EXECUTE crear_id_usuario_movimiento_stmt;
DEALLOCATE PREPARE crear_id_usuario_movimiento_stmt;

SET @existe_eliminado_movimiento = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'movimientos_inventarios'
      AND column_name = 'eliminado'
);
SET @crear_eliminado_movimiento = IF(
    @existe_eliminado_movimiento = 0,
    'ALTER TABLE movimientos_inventarios ADD COLUMN eliminado TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE crear_eliminado_movimiento_stmt FROM @crear_eliminado_movimiento;
EXECUTE crear_eliminado_movimiento_stmt;
DEALLOCATE PREPARE crear_eliminado_movimiento_stmt;

ALTER TABLE movimientos_inventarios
    MODIFY COLUMN cantidad DECIMAL(12,4) NULL;

CREATE TABLE IF NOT EXISTS alertas_inventario (
    id_alerta INT NOT NULL AUTO_INCREMENT,
    tipo VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    detalle VARCHAR(250) NOT NULL,
    id_insumo INT NOT NULL,
    id_lote INT NULL,
    fecha_generacion DATETIME(6) NOT NULL,
    fecha_revision DATETIME(6) NULL,
    fecha_atencion DATETIME(6) NULL,
    accion VARCHAR(40) NULL,
    observacion VARCHAR(250) NULL,
    id_usuario_atencion INT NULL,
    clave_activa VARCHAR(100) NULL,
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id_alerta),
    CONSTRAINT uq_alertas_clave_activa UNIQUE (clave_activa),
    CONSTRAINT fk_alertas_insumo FOREIGN KEY (id_insumo) REFERENCES insumos (id_insumo),
    CONSTRAINT fk_alertas_lote FOREIGN KEY (id_lote) REFERENCES lotes_insumos (id_lote),
    CONSTRAINT fk_alertas_usuario FOREIGN KEY (id_usuario_atencion) REFERENCES usuarios (id_usuario),
    INDEX idx_alertas_estado (estado, eliminado),
    INDEX idx_alertas_tipo (tipo),
    INDEX idx_alertas_fecha (fecha_generacion)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO roles (nombre_rol, descripcion, eliminado)
SELECT 'ALMACENERO', 'Gestiona lotes, existencias y alertas de inventario', 0
WHERE NOT EXISTS (
    SELECT 1 FROM roles
    WHERE UPPER(nombre_rol) = 'ALMACENERO' AND eliminado = 0
);
