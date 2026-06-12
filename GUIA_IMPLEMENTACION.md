# Guía de Implementación - Modificaciones Lógica Gráfica

## Estado Actual de la Implementación

Se han completado los siguientes cambios en la rama `modificaciones-logica-grafica`:

### ✅ Completado

1. **Rama Git**
   - ✅ Creada rama `modificaciones-logica-grafica`

2. **Navbar Fragmento**
   - ✅ Crear fragmento navbar reutilizable (`fragments/navbar.html`)
   - ✅ Información de usuario con fecha de inicio de sesión
   - ✅ Rol del usuario en dropdown
   - ✅ Botón de cierre de sesión en dropdown
   - ✅ Remover info de usuario del sidebar

3. **Estilos Separados**
   - ✅ `css/navbar.css` - Estilos del navbar
   - ✅ `css/login.css` - Estilos del login (extraído de HTML)
   - ✅ `css/menu.css` - Estilos del menú
   - ✅ `css/modal.css` - Estilos de modales
   - ✅ `css/spinner.css` - Estilos de spinners de carga

4. **Spring Security**
   - ✅ Agregada dependencia en pom.xml
   - ✅ Configuración en `Config/SecurityConfig.java`
   - ✅ UserDetailsService en `Config/UserDetailsServiceImpl.java`
   - ✅ Rutas protegidas por rol

5. **Modales Mejorados**
   - ✅ Creada clase `RestoModal` en `js/modal-manager.js`
   - ✅ Clases extendidas: `AlertModal`, `ConfirmModal`, `SuccessModal`, `ErrorModal`, `LoadingModal`
   - ✅ Helper de funciones comunes: `ModalHelper`
   - ✅ Ejemplo de implementación en `js/modales-mesas-ejemplo.js`

6. **Actualización de Páginas**
   - ✅ Reemplazado header estático por navbar en `mesas.html`
   - ✅ Reemplazado header estático por navbar en `menu.html`
   - ✅ Actualizado `sidebar.html` con nuevos CSS

### 📋 Por Hacer

Los siguientes cambios aún necesitan implementarse:

1. **Integración de Modales en Mesas**
   - [ ] Reemplazar `alert()` por `ModalHelper`
   - [ ] Implementar modales para crear/editar/eliminar mesas
   - [ ] Agregar animaciones

2. **Integración de Modales en Pedidos**
   - [ ] Reemplazar `alert()` por `ModalHelper`
   - [ ] Implementar modales para acciones de pedidos

3. **Spinner de Carga**
   - [ ] Agregar spinner cuando no hay datos
   - [ ] Implementar en módulos: usuarios, roles, categorías, platos

4. **Migración Vue 3**
   - [ ] Actualizar scripts de JS a Vue 3
   - [ ] Mejorar reactividad y estado

5. **Testing**
   - [ ] Compilar proyecto
   - [ ] Verificar Spring Security
   - [ ] Probar navbar en todas las páginas
   - [ ] Validar modales

---

## Cómo Usar la Nueva Estructura

### 1. Usar el Navbar Fragmento

En cualquier página que necesite navbar, reemplaza el header con:

```html
<div th:replace="~{fragments/navbar :: navbar}"></div>
```

**Ya actualizado en:**
- `mesas.html`
- `menu.html`

**Aún necesita actualización:**
- `pedidos.html`
- `usuarios-roles.html`
- `disponible.html`
- Otras páginas sin navbar

### 2. Usar Modales en lugar de Alerts

**Antes:**
```javascript
alert('¿Estás seguro?');
```

**Después:**
```javascript
ModalHelper.confirm(
    'Confirmación',
    '¿Estás seguro de que deseas continuar?',
    () => {
        // Acción confirmada
        ModalHelper.success('Éxito', 'La acción se realizó correctamente');
    },
    () => {
        // Cancelado
    }
);
```

### 3. Modales Disponibles

```javascript
// Alerta simple
ModalHelper.alert('Título', 'Mensaje', callback);

// Confirmación
ModalHelper.confirm('Título', 'Mensaje', onConfirm, onCancel);

// Éxito
ModalHelper.success('Título', 'Mensaje', callback);

// Error
ModalHelper.error('Título', 'Mensaje', callback);

// Carga
const loadingModal = ModalHelper.loading('Procesando...');
setTimeout(() => loadingModal.hide(), 3000);
```

### 4. Modal Personalizado

```javascript
const modal = new RestoModal({
    id: 'my-modal',
    title: 'Mi Modal',
    body: '<p>Contenido aquí</p>',
    buttons: {
        aceptar: {
            label: 'Aceptar',
            variant: 'primary',
            icon: 'check',
            callback: () => console.log('Aceptado')
        },
        cancelar: {
            label: 'Cancelar',
            variant: 'secondary',
            dismiss: true
        }
    },
    size: 'md', // 'sm', 'md', 'lg', 'xl'
    centered: true
});

modal.show();
```

### 5. Integrar Spinners de Carga

Para páginas que cargan datos inicialmente:

```html
<!-- Mientras carga -->
<div class="loading-container">
    <div class="spinner-semicircle"></div>
    <p class="loading-text">Cargando datos...</p>
</div>

<!-- Cuando hay datos -->
<div v-if="!isLoading" class="content">
    <!-- Tu contenido -->
</div>
```

---

## Spring Security - Rutas Protegidas

Las rutas están configuradas por rol:

```
PUBLIC (sin autenticación):
- /login
- /registro
- /static/** (CSS, JS, imágenes)

ADMIN:
- /usuarios-roles
- /configuracion
- /api/admin/**
- /api/usuarios/**
- /api/roles/**

MESERO, ADMIN, COCINA:
- /api/pedidos/**

MESERO, ADMIN:
- /api/mesas/**
- /api/alimentos/**
- /api/categorias/**

AUTHENTICATED (cualquier usuario autenticado):
- /dashboard
- /menu
- /pedidos
- /mesas
```

---

## Próximos Pasos Recomendados

1. **Compilar el proyecto:**
   ```bash
   mvn clean compile
   ```

2. **Revisar errores de compilación** especialmente en `UserDetailsServiceImpl` si las entidades tienen nombres diferentes

3. **Actualizar los archivos HTML restantes** con el navbar fragmento

4. **Integrar modales** en los JavaScript existentes

5. **Probar Login y Spring Security** asegurándose de que funcione correctamente

6. **Implementar spinners de carga** en páginas de usuarios, roles, etc.

---

## Archivos Modificados/Creados

### Creados:
- `src/main/java/com/utp/RestoControl/Config/SecurityConfig.java`
- `src/main/java/com/utp/RestoControl/Config/UserDetailsServiceImpl.java`
- `src/main/resources/templates/fragments/navbar.html`
- `src/main/resources/templates/fragments/head-global.html`
- `src/main/resources/static/css/navbar.css`
- `src/main/resources/static/css/login.css`
- `src/main/resources/static/css/menu.css`
- `src/main/resources/static/css/modal.css`
- `src/main/resources/static/css/spinner.css`
- `src/main/resources/static/js/modal-manager.js`
- `src/main/resources/static/js/modales-mesas-ejemplo.js`

### Modificados:
- `pom.xml` - Agregadas dependencias de Spring Security
- `src/main/resources/templates/login.html` - Removido CSS embebido
- `src/main/resources/templates/mesas.html` - Reemplazado header con navbar
- `src/main/resources/templates/menu.html` - Reemplazado header con navbar
- `src/main/resources/templates/fragments/sidebar.html` - Limpiado

---

## Comandos Útiles

```bash
# Ver rama actual
git branch

# Ver cambios en la rama
git log modificaciones-logica-grafica --oneline

# Compilar proyecto
mvn clean package

# Ejecutar tests
mvn test

# Hacer merge a main (cuando esté listo)
git checkout main
git merge modificaciones-logica-grafica
```

---

## Notas Importantes

1. **Usuario y Contraseña en BD**: Asegúrate de que los usuarios en la BD tengan contraseñas codificadas con BCrypt
2. **Campos en Usuario Entity**: El `UserDetailsServiceImpl` asume que existe el campo `activo` en la entidad Usuario
3. **Rol Entity**: La clase Rol debe tener un campo `nombreRol`
4. **CORS**: Está configurado para `http://localhost:8080`, ajusta según necesario

---

## Support

Para más información sobre las clases y métodos disponibles, revisar:
- `modal-manager.js` - Documentación de clases de modal
- `modales-mesas-ejemplo.js` - Ejemplos de uso
- Comentarios en `SecurityConfig.java` - Configuración de seguridad
