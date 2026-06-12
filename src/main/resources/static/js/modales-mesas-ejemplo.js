/**
 * Ejemplo de Modales en Mesas
 * Aquí se muestra cómo usar la clase RestoModal y ModalHelper en la página de mesas
 */

// Ejemplo 1: Mostrar alerta simple
function mostrarAlerta() {
    ModalHelper.alert(
        'Información',
        'Esta es una alerta simple',
        () => console.log('Alerta cerrada')
    );
}

// Ejemplo 2: Mostrar confirmación
function mostrarConfirmacion() {
    ModalHelper.confirm(
        '¿Desea continuar?',
        '¿Está seguro de que desea realizar esta acción?',
        () => {
            console.log('Confirmado');
            ModalHelper.success('Éxito', 'La acción se realizó correctamente');
        },
        () => console.log('Cancelado')
    );
}

// Ejemplo 3: Modal personalizado para crear mesa
function abrirModalCrearMesa() {
    const modal = new RestoModal({
        id: 'modal-crear-mesa',
        title: 'Crear Nueva Mesa',
        body: `
            <form id="form-crear-mesa">
                <div class="form-group mb-3">
                    <label class="form-label" for="numeroMesa">Número de Mesa</label>
                    <input type="text" class="form-control" id="numeroMesa" placeholder="Ej: 1, A1, etc." required>
                </div>
                <div class="form-group mb-3">
                    <label class="form-label" for="capacidad">Capacidad de Personas</label>
                    <input type="number" class="form-control" id="capacidad" placeholder="Ej: 4" min="1" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="piso">Piso</label>
                    <select class="form-select" id="piso" required>
                        <option value="">Seleccionar piso...</option>
                        <option value="1">Piso 1</option>
                        <option value="2">Piso 2</option>
                        <option value="3">Piso 3</option>
                    </select>
                </div>
            </form>
        `,
        buttons: {
            guardar: {
                label: 'Guardar',
                variant: 'primary',
                icon: 'save',
                dismiss: false,
                callback: () => guardarMesa(modal)
            },
            cancelar: {
                label: 'Cancelar',
                variant: 'secondary',
                icon: 'x',
                dismiss: true
            }
        },
        size: 'md'
    });
    modal.show();
}

// Función para guardar mesa (con validación)
function guardarMesa(modal) {
    const numeroMesa = document.getElementById('numeroMesa').value;
    const capacidad = document.getElementById('capacidad').value;
    const piso = document.getElementById('piso').value;

    if (!numeroMesa || !capacidad || !piso) {
        ModalHelper.error('Error de Validación', 'Por favor complete todos los campos');
        return;
    }

    // Simular envío al servidor
    const loadingModal = ModalHelper.loading('Creando mesa...');

    setTimeout(() => {
        loadingModal.hide();
        modal.hide();
        ModalHelper.success('Éxito', `Mesa ${numeroMesa} creada correctamente`);
    }, 1500);
}

// Ejemplo 4: Modal para editar mesa
function abrirModalEditarMesa(idMesa, numeroMesa, capacidad, piso) {
    const modal = new RestoModal({
        id: `modal-editar-mesa-${idMesa}`,
        title: 'Editar Mesa',
        body: `
            <form id="form-editar-mesa">
                <div class="form-group mb-3">
                    <label class="form-label" for="numeroMesaEdit">Número de Mesa</label>
                    <input type="text" class="form-control" id="numeroMesaEdit" value="${numeroMesa}" required>
                </div>
                <div class="form-group mb-3">
                    <label class="form-label" for="capacidadEdit">Capacidad de Personas</label>
                    <input type="number" class="form-control" id="capacidadEdit" value="${capacidad}" min="1" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="pisoEdit">Piso</label>
                    <select class="form-select" id="pisoEdit" required>
                        <option value="1" ${piso == 1 ? 'selected' : ''}>Piso 1</option>
                        <option value="2" ${piso == 2 ? 'selected' : ''}>Piso 2</option>
                        <option value="3" ${piso == 3 ? 'selected' : ''}>Piso 3</option>
                    </select>
                </div>
            </form>
        `,
        buttons: {
            guardar: {
                label: 'Guardar Cambios',
                variant: 'primary',
                icon: 'save',
                dismiss: false,
                callback: () => actualizarMesa(idMesa, modal)
            },
            cancelar: {
                label: 'Cancelar',
                variant: 'secondary',
                icon: 'x',
                dismiss: true
            }
        },
        size: 'md'
    });
    modal.show();
}

// Función para actualizar mesa
function actualizarMesa(idMesa, modal) {
    const numeroMesa = document.getElementById('numeroMesaEdit').value;
    const capacidad = document.getElementById('capacidadEdit').value;
    const piso = document.getElementById('pisoEdit').value;

    if (!numeroMesa || !capacidad || !piso) {
        ModalHelper.error('Error de Validación', 'Por favor complete todos los campos');
        return;
    }

    const loadingModal = ModalHelper.loading('Actualizando mesa...');

    setTimeout(() => {
        loadingModal.hide();
        modal.hide();
        ModalHelper.success('Éxito', `Mesa ${numeroMesa} actualizada correctamente`);
    }, 1500);
}

// Ejemplo 5: Modal de confirmación para eliminar
function abrirModalEliminarMesa(idMesa, numeroMesa) {
    ModalHelper.confirm(
        '¿Eliminar Mesa?',
        `¿Está seguro de que desea eliminar la mesa ${numeroMesa}? Esta acción no se puede deshacer.`,
        () => eliminarMesa(idMesa, numeroMesa),
        null,
        'danger'
    );
}

// Función para eliminar mesa
function eliminarMesa(idMesa, numeroMesa) {
    const loadingModal = ModalHelper.loading('Eliminando mesa...');

    setTimeout(() => {
        loadingModal.hide();
        ModalHelper.success('Éxito', `Mesa ${numeroMesa} eliminada correctamente`);
    }, 1500);
}

// Ejemplo 6: Modal para ver detalles de mesa
function abrirModalDetallesMesa(idMesa, numeroMesa, estado, clientesCuentan, tiempo) {
    const estadoColor = {
        'libre': 'success',
        'ocupada': 'danger',
        'reservada': 'warning',
        'por-cobrar': 'info'
    }[estado] || 'secondary';

    const modal = new RestoModal({
        id: `modal-detalles-mesa-${idMesa}`,
        title: `Detalles - Mesa ${numeroMesa}`,
        body: `
            <div class="mb-3">
                <span class="badge bg-${estadoColor} px-3 py-2 fs-6">
                    ${estado.toUpperCase()}
                </span>
            </div>
            <div class="row mb-3">
                <div class="col-6 mb-3">
                    <small class="text-muted">Número de Personas</small>
                    <div class="fs-5 fw-bold">${clientesCuentan}</div>
                </div>
                <div class="col-6 mb-3">
                    <small class="text-muted">Tiempo de Ocupación</small>
                    <div class="fs-5 fw-bold">${tiempo}</div>
                </div>
            </div>
            <div class="alert alert-info" role="alert">
                <i class="bi bi-info-circle me-2"></i>
                Aquí puedes ver más detalles de la mesa
            </div>
        `,
        buttons: {
            cerrar: {
                label: 'Cerrar',
                variant: 'secondary',
                icon: 'x',
                dismiss: true
            }
        },
        size: 'sm'
    });
    modal.show();
}

// Ejemplo de uso con Vue 3
// Este es un ejemplo de cómo integrar los modales con Vue
const app = Vue.createApp({
    data() {
        return {
            mesas: [],
            isLoading: false
        };
    },
    methods: {
        crearMesa() {
            abrirModalCrearMesa();
        },
        editarMesa(mesa) {
            abrirModalEditarMesa(mesa.id, mesa.numero, mesa.capacidad, mesa.piso);
        },
        eliminarMesa(mesa) {
            abrirModalEliminarMesa(mesa.id, mesa.numero);
        },
        verDetalles(mesa) {
            abrirModalDetallesMesa(mesa.id, mesa.numero, mesa.estado, mesa.personas, mesa.tiempo);
        }
    }
});

// Exportar funciones globales
window.mostrarAlerta = mostrarAlerta;
window.mostrarConfirmacion = mostrarConfirmacion;
window.abrirModalCrearMesa = abrirModalCrearMesa;
window.abrirModalEditarMesa = abrirModalEditarMesa;
window.abrirModalEliminarMesa = abrirModalEliminarMesa;
window.abrirModalDetallesMesa = abrirModalDetallesMesa;
window.mesasApp = app;
