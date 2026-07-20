CREATE TABLE IF NOT EXISTS proveedores (
    id_proveedor INT NOT NULL AUTO_INCREMENT,
    razon_social VARCHAR(150) NOT NULL,
    ruc VARCHAR(11) NOT NULL,
    contacto VARCHAR(120) NULL,
    telefono VARCHAR(20) NULL,
    correo VARCHAR(120) NULL,
    direccion VARCHAR(250) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id_proveedor),
    CONSTRAINT uq_proveedores_ruc UNIQUE (ruc),
    INDEX idx_proveedores_activo (activo, eliminado)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS compras_abastecimiento (
    id_compra INT NOT NULL AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    id_usuario_almacenero INT NOT NULL,
    fecha_compra DATE NOT NULL,
    fecha_registro DATETIME(6) NOT NULL,
    numero_documento VARCHAR(60) NOT NULL,
    observacion VARCHAR(250) NULL,
    total DECIMAL(14,4) NOT NULL DEFAULT 0,
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id_compra),
    CONSTRAINT uq_compra_proveedor_documento UNIQUE (id_proveedor, numero_documento),
    CONSTRAINT fk_compra_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedores (id_proveedor),
    CONSTRAINT fk_compra_usuario FOREIGN KEY (id_usuario_almacenero) REFERENCES usuarios (id_usuario),
    INDEX idx_compras_fecha (fecha_compra),
    INDEX idx_compras_usuario (id_usuario_almacenero)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS compras_abastecimiento_detalles (
    id_compra_detalle INT NOT NULL AUTO_INCREMENT,
    id_compra INT NOT NULL,
    id_insumo INT NOT NULL,
    id_lote INT NOT NULL,
    cantidad DECIMAL(12,4) NOT NULL,
    costo_unitario DECIMAL(12,4) NOT NULL,
    subtotal DECIMAL(14,4) NOT NULL,
    PRIMARY KEY (id_compra_detalle),
    CONSTRAINT uq_compra_detalle_insumo UNIQUE (id_compra, id_insumo),
    CONSTRAINT uq_compra_detalle_lote UNIQUE (id_lote),
    CONSTRAINT fk_compra_detalle_compra FOREIGN KEY (id_compra) REFERENCES compras_abastecimiento (id_compra),
    CONSTRAINT fk_compra_detalle_insumo FOREIGN KEY (id_insumo) REFERENCES insumos (id_insumo),
    CONSTRAINT fk_compra_detalle_lote FOREIGN KEY (id_lote) REFERENCES lotes_insumos (id_lote),
    INDEX idx_compra_detalle_compra (id_compra),
    INDEX idx_compra_detalle_insumo (id_insumo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Los pedidos que V8 identificó como heredados tienen la misma fecha de creación y envío.
-- Se conserva su historial sin intentar descontar hoy un inventario que antes no estaba integrado.
UPDATE pedidos pedido
JOIN estados_pedidos estado ON estado.id_estado_pedido = pedido.id_estado_pedido
SET pedido.fecha_consumo_inventario = pedido.fecha_envio_cocina
WHERE pedido.eliminado = 0
  AND pedido.fecha_consumo_inventario IS NULL
  AND pedido.fecha_envio_cocina IS NOT NULL
  AND pedido.fecha_envio_cocina = pedido.fecha
  AND UPPER(estado.nombre) NOT IN ('PAGADO', 'COBRADO', 'CANCELADO');
