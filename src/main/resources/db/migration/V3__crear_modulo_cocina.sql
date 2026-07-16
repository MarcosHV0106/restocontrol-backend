SET @existe_fecha_inicio_preparacion = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pedidos'
      AND column_name = 'fecha_inicio_preparacion'
);

SET @crear_fecha_inicio_preparacion = IF(
    @existe_fecha_inicio_preparacion = 0,
    'ALTER TABLE pedidos ADD COLUMN fecha_inicio_preparacion DATETIME(6) NULL',
    'SELECT 1'
);

PREPARE crear_fecha_inicio_preparacion_stmt FROM @crear_fecha_inicio_preparacion;
EXECUTE crear_fecha_inicio_preparacion_stmt;
DEALLOCATE PREPARE crear_fecha_inicio_preparacion_stmt;

SET @existe_fecha_listo = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pedidos'
      AND column_name = 'fecha_listo'
);

SET @crear_fecha_listo = IF(
    @existe_fecha_listo = 0,
    'ALTER TABLE pedidos ADD COLUMN fecha_listo DATETIME(6) NULL',
    'SELECT 1'
);

PREPARE crear_fecha_listo_stmt FROM @crear_fecha_listo;
EXECUTE crear_fecha_listo_stmt;
DEALLOCATE PREPARE crear_fecha_listo_stmt;

SET @existe_fecha_entregado = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pedidos'
      AND column_name = 'fecha_entregado'
);

SET @crear_fecha_entregado = IF(
    @existe_fecha_entregado = 0,
    'ALTER TABLE pedidos ADD COLUMN fecha_entregado DATETIME(6) NULL',
    'SELECT 1'
);

PREPARE crear_fecha_entregado_stmt FROM @crear_fecha_entregado;
EXECUTE crear_fecha_entregado_stmt;
DEALLOCATE PREPARE crear_fecha_entregado_stmt;

INSERT INTO roles (nombre_rol, descripcion, eliminado)
SELECT 'COCINERO', 'Gestiona la preparacion y entrega de pedidos en cocina', 0
WHERE NOT EXISTS (
    SELECT 1 FROM roles
    WHERE UPPER(nombre_rol) = 'COCINERO'
      AND eliminado = 0
);

UPDATE usuarios usuario
JOIN roles rol_anterior
    ON rol_anterior.id_rol = usuario.id_rol
JOIN roles rol_cocinero
    ON UPPER(rol_cocinero.nombre_rol) = 'COCINERO'
   AND rol_cocinero.eliminado = 0
SET usuario.id_rol = rol_cocinero.id_rol
WHERE UPPER(rol_anterior.nombre_rol) = 'COCINA';

UPDATE roles
SET eliminado = 1
WHERE UPPER(nombre_rol) = 'COCINA';

INSERT INTO estados_pedidos (nombre, eliminado)
SELECT 'EN PREPARACION', 0
WHERE NOT EXISTS (
    SELECT 1 FROM estados_pedidos
    WHERE UPPER(nombre) IN ('EN PREPARACION', 'EN PREPARACIÓN', 'PREPARANDO')
      AND eliminado = 0
);

INSERT INTO estados_pedidos (nombre, eliminado)
SELECT 'LISTO', 0
WHERE NOT EXISTS (
    SELECT 1 FROM estados_pedidos
    WHERE UPPER(nombre) = 'LISTO'
      AND eliminado = 0
);

INSERT INTO estados_pedidos (nombre, eliminado)
SELECT 'ENTREGADO', 0
WHERE NOT EXISTS (
    SELECT 1 FROM estados_pedidos
    WHERE UPPER(nombre) = 'ENTREGADO'
      AND eliminado = 0
);

UPDATE pedidos pedido
JOIN estados_pedidos estado
    ON estado.id_estado_pedido = pedido.id_estado_pedido
SET pedido.fecha_inicio_preparacion = COALESCE(pedido.fecha_inicio_preparacion, pedido.fecha)
WHERE UPPER(estado.nombre) IN ('EN PREPARACION', 'EN PREPARACIÓN', 'PREPARANDO', 'LISTO', 'ENTREGADO');

UPDATE pedidos pedido
JOIN estados_pedidos estado
    ON estado.id_estado_pedido = pedido.id_estado_pedido
SET pedido.fecha_listo = COALESCE(pedido.fecha_listo, pedido.fecha_inicio_preparacion, pedido.fecha)
WHERE UPPER(estado.nombre) IN ('LISTO', 'ENTREGADO');

UPDATE pedidos pedido
JOIN estados_pedidos estado
    ON estado.id_estado_pedido = pedido.id_estado_pedido
SET pedido.fecha_entregado = COALESCE(pedido.fecha_entregado, pedido.fecha_listo, pedido.fecha)
WHERE UPPER(estado.nombre) = 'ENTREGADO';
