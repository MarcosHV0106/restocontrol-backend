# Trabajo Colaborativo con GitHub y Branches

La matriz actual de requisitos, roles, flujos y condiciones de despliegue se encuentra en [docs/CUMPLIMIENTO_REQUISITOS.md](docs/CUMPLIMIENTO_REQUISITOS.md).

## IMPORTANTE

El proyecto será trabajado utilizando:

- NetBeans
- GitHub
- Branches individuales por integrante

NO trabajar directamente sobre:

```plaintext
master
```

---

# Requisitos Previos

Cada integrante debe tener instalado:

- NetBeans (versión reciente recomendada)
- Git integrado en NetBeans
- Java JDK 17+
- Acceso al repositorio GitHub

---

# Clonar el Repositorio

En NetBeans:

```plaintext
Team
→ Git
→ Clone
```

Pegar la URL del repositorio.

Ejemplo:

```plaintext
https://github.com/USUARIO/restocontrol-backend.git
```

---

# Abrir el Proyecto

Después de clonar:

```plaintext
Open Project
```

---

# IMPORTANTE SOBRE BRANCHES

Cada integrante debe trabajar en SU PROPIA branch.

NO modificar directamente:

```plaintext
master
```

---

# Crear una Branch Local

## Paso 1 — Hacer Pull

Antes de comenzar:

```plaintext
Team
→ Remote
→ Pull
```

---

## Paso 2 — Crear Branch

En NetBeans:

```plaintext
Team
→ Git
→ Branch/Tag
→ Create Branch
```

---

# Convención Recomendada

Formato:

```plaintext
feature/nombre-modulo
```

Ejemplos:

```plaintext
feature/alimentos
feature/usuarios
feature/pedidos
feature/inventario
```

---

## Paso 3 — Activar la Branch

Marcar:

```plaintext
Checkout Created Branch
```

Esto hará que NetBeans cambie automáticamente a esa branch.

---

# Flujo Recomendado de Trabajo

```plaintext
Pull
↓
Programar
↓
Add
↓
Commit
↓
Push
```

---

# Add

Agregar cambios realizados:

```plaintext
Team
→ Git
→ Add
```

---

# Commit

Guardar cambios localmente:

```plaintext
Team
→ Commit
```

Ejemplos de mensajes:

```plaintext
CRUD alimentos implementado
Entidad usuarios creada
Relación ManyToOne agregada
```

---

# Push de la Branch

Subir cambios a GitHub:

```plaintext
Team
→ Remote
→ Push
```

---

# Primera vez haciendo Push

GitHub detectará que la branch no existe remotamente.

NetBeans preguntará:

```plaintext
Create Remote Branch?
```

Seleccionar:

```plaintext
Yes
```

---

# Resultado

GitHub creará automáticamente:

```plaintext
origin/feature/alimentos
```

o la branch correspondiente.

---

# Importante

La branch local y la branch remota quedarán sincronizadas.

Ejemplo:

| Local | GitHub |
|---|---|
| feature/alimentos | origin/feature/alimentos |

---

# Cómo Seguir Trabajando

Cada vez que comiencen:

```plaintext
Pull
```

Cada vez que terminen cambios:

```plaintext
Add
↓
Commit
↓
Push
```

---

# Merge de Branches

El merge debe realizarse únicamente cuando el módulo esté:

- funcional
- probado
- sin errores
- compilando correctamente

---

# Pasos para Merge

## 1. Cambiar a master

```plaintext
Team
→ Git
→ Branch/Tag
→ Switch Branch
→ master
```

---

## 2. Hacer Pull de master

```plaintext
Team
→ Remote
→ Pull
```

---

## 3. Realizar Merge

```plaintext
Team
→ Git
→ Merge Revision
```

Seleccionar la branch correspondiente.

Ejemplo:

```plaintext
feature/alimentos
```

---

## 4. Verificar el Proyecto

Antes del Push final:

- Ejecutar Spring Boot
- Verificar errores
- Probar endpoints
- Confirmar que el proyecto compile correctamente

---

## 5. Push Final

```plaintext
Team
→ Remote
→ Push
```

---

# Recomendaciones Importantes

## NO trabajar sobre master

La rama:

```plaintext
master
```

debe mantenerse estable y funcional.

---

## Hacer Pull antes de programar

Esto evita conflictos y versiones desactualizadas.

---

## Hacer commits claros

NO usar:

```plaintext
avance
update
cambios
```

Usar mensajes descriptivos.

Ejemplos:

```plaintext
CRUD alimentos implementado
Relación OneToMany agregada
Validaciones usuarios implementadas
```

---

# Estructura Recomendada de Branches

```plaintext
master
 ├── feature/alimentos
 ├── feature/usuarios
 ├── feature/pedidos
 └── feature/inventario
```

---

# Objetivo del Flujo

Permitir que todos los integrantes trabajen simultáneamente sin interferir entre sí, manteniendo el proyecto estable y organizado.
