UPDATE roles
SET descripcion = 'Consulta indicadores ejecutivos y reportes de gestion',
    eliminado = 0
WHERE UPPER(nombre_rol) = 'GERENTE';

INSERT INTO roles (nombre_rol, descripcion, eliminado)
SELECT 'GERENTE', 'Consulta indicadores ejecutivos y reportes de gestion', 0
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE UPPER(nombre_rol) = 'GERENTE'
);
