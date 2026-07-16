CREATE TABLE IF NOT EXISTS cobros (
    id_cobro INT NOT NULL AUTO_INCREMENT,
    id_pedido INT NOT NULL,
    fecha_cobro DATETIME(6) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    igv DECIMAL(12, 2) NOT NULL,
    descuento DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_cobrado DECIMAL(12, 2) NOT NULL,
    total_recibido DECIMAL(12, 2) NOT NULL,
    vuelto DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    id_usuario_cajero INT NOT NULL,
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id_cobro),
    CONSTRAINT uq_cobros_pedido UNIQUE (id_pedido),
    CONSTRAINT fk_cobros_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedidos (id_pedido),
    CONSTRAINT fk_cobros_usuario_cajero
        FOREIGN KEY (id_usuario_cajero) REFERENCES usuarios (id_usuario),
    INDEX idx_cobros_fecha (fecha_cobro),
    INDEX idx_cobros_cajero (id_usuario_cajero),
    INDEX idx_cobros_eliminado (eliminado)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pagos_cobro (
    id_pago_cobro INT NOT NULL AUTO_INCREMENT,
    id_cobro INT NOT NULL,
    secuencia INT NOT NULL,
    metodo_pago VARCHAR(30) NOT NULL,
    monto DECIMAL(12, 2) NOT NULL,
    monto_recibido DECIMAL(12, 2) NOT NULL,
    vuelto DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tipo_comprobante VARCHAR(20) NOT NULL,
    numero_comprobante VARCHAR(30) NULL,
    documento_cliente VARCHAR(20) NULL,
    razon_social VARCHAR(150) NULL,
    referencia VARCHAR(80) NULL,
    eliminado TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id_pago_cobro),
    CONSTRAINT uq_pagos_cobro_comprobante UNIQUE (numero_comprobante),
    CONSTRAINT fk_pagos_cobro_cobro
        FOREIGN KEY (id_cobro) REFERENCES cobros (id_cobro),
    INDEX idx_pagos_cobro_cobro (id_cobro),
    INDEX idx_pagos_cobro_metodo (metodo_pago),
    INDEX idx_pagos_cobro_eliminado (eliminado)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO roles (nombre_rol, descripcion, eliminado)
SELECT 'CAJERO', 'Gestiona cobros y emision de comprobantes internos', 0
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE UPPER(nombre_rol) = 'CAJERO'
      AND eliminado = 0
);
