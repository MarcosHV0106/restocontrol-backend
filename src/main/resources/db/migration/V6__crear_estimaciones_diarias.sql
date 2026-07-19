CREATE TABLE IF NOT EXISTS estimaciones_diarias (
    id_estimacion_diaria INT NOT NULL AUTO_INCREMENT,
    fecha DATE NOT NULL,
    id_alimento INT NOT NULL,
    porciones INT NOT NULL,
    id_usuario INT NULL,
    fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_actualizacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    eliminado BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id_estimacion_diaria),
    CONSTRAINT uk_estimacion_fecha_alimento UNIQUE (fecha, id_alimento),
    CONSTRAINT fk_estimacion_alimento FOREIGN KEY (id_alimento) REFERENCES alimentos (id_alimento),
    CONSTRAINT fk_estimacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
    INDEX idx_estimacion_fecha_eliminado (fecha, eliminado),
    INDEX idx_estimacion_alimento (id_alimento),
    CONSTRAINT chk_estimacion_porciones CHECK (porciones >= 0)
);
