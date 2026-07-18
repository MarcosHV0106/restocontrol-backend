ALTER TABLE insumos
    MODIFY COLUMN stock_actual DECIMAL(12,4) NULL,
    MODIFY COLUMN stock_minimo DECIMAL(12,4) NULL;

ALTER TABLE lotes_insumos
    MODIFY COLUMN cantidad_inicial DECIMAL(12,4) NOT NULL,
    MODIFY COLUMN cantidad_actual DECIMAL(12,4) NOT NULL;

ALTER TABLE movimientos_inventarios
    MODIFY COLUMN cantidad DECIMAL(12,4) NULL;
