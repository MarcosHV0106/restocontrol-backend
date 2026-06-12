/**
 * Modal Manager para RestoControl
 * Proporciona una clase para crear y gestionar modales Bootstrap
 * Utiliza Bootstrap 5.3.8
 */

class RestoModal {
    /**
     * Constructor del Modal
     * @param {Object} options - Opciones de configuración del modal
     * @param {string} options.id - ID único del modal
     * @param {string} options.title - Título del modal
     * @param {string} options.body - Contenido HTML del modal
     * @param {Object} options.buttons - Botones del modal
     * @param {string} options.size - Tamaño: 'sm', 'lg', 'xl' (default: 'md')
     * @param {boolean} options.centered - Centrado verticalmente (default: true)
     * @param {boolean} options.backdropStatic - Backdrop no clickeable (default: false)
     */
    constructor(options = {}) {
        this.id = options.id || `modal-${Date.now()}`;
        this.title = options.title || 'Modal';
        this.body = options.body || '';
        this.buttons = options.buttons || {};
        this.size = options.size || 'md';
        this.centered = options.centered !== false;
        this.backdropStatic = options.backdropStatic || false;
        this.modal = null;
        this.bootstrapModal = null;
        this.create();
    }

    /**
     * Crear el elemento del modal
     */
    create() {
        const sizeClass = this.size === 'md' ? '' : `modal-${this.size}`;
        const centeredClass = this.centered ? 'modal-dialog-centered' : '';
        
        const modalHTML = `
            <div class="modal fade" id="${this.id}" tabindex="-1" aria-labelledby="${this.id}Label" aria-hidden="true" data-bs-backdrop="${this.backdropStatic ? 'static' : 'true'}" data-bs-keyboard="${!this.backdropStatic}">
                <div class="modal-dialog ${sizeClass} ${centeredClass}">
                    <div class="modal-content" style="border-radius: 12px; border: none; box-shadow: 0 10px 40px rgba(0,0,0,0.2);">
                        <div class="modal-header" style="border-bottom: 1px solid #e8e0d7; padding: 1.5rem;">
                            <h1 class="modal-title fs-5" id="${this.id}Label" style="color: #2c1a11; font-weight: 700;">
                                ${this.title}
                            </h1>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar" style="filter: invert(0.5);"></button>
                        </div>
                        <div class="modal-body" style="padding: 1.5rem; color: #2c1a11;">
                            ${this.body}
                        </div>
                        <div class="modal-footer" style="border-top: 1px solid #e8e0d7; padding: 1.5rem;">
                            ${this.renderButtons()}
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Crear elemento temporal para parsear HTML
        const parser = new DOMParser();
        const doc = parser.parseFromString(modalHTML, 'text/html');
        this.modal = doc.body.firstElementChild;

        // Agregar al DOM
        document.body.appendChild(this.modal);

        // Inicializar Bootstrap Modal
        this.bootstrapModal = new bootstrap.Modal(this.modal);

        // Agregar event listeners a los botones
        this.attachButtonListeners();
    }

    /**
     * Renderizar los botones del modal
     * @returns {string} HTML de los botones
     */
    renderButtons() {
        if (Object.keys(this.buttons).length === 0) {
            return `
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
            `;
        }

        return Object.entries(this.buttons)
            .map(([key, button]) => {
                const variant = button.variant || 'secondary';
                const isDismiss = button.dismiss !== false;
                return `
                    <button type="button" class="btn btn-${variant}" id="${this.id}-btn-${key}" ${isDismiss ? 'data-bs-dismiss="modal"' : ''}>
                        ${button.icon ? `<i class="bi bi-${button.icon} me-2"></i>` : ''}
                        ${button.label || key}
                    </button>
                `;
            })
            .join('');
    }

    /**
     * Adjuntar event listeners a los botones
     */
    attachButtonListeners() {
        Object.entries(this.buttons).forEach(([key, button]) => {
            const btnElement = document.getElementById(`${this.id}-btn-${key}`);
            if (btnElement && button.callback) {
                btnElement.addEventListener('click', (e) => {
                    e.preventDefault();
                    button.callback();
                });
            }
        });
    }

    /**
     * Mostrar el modal
     */
    show() {
        if (this.bootstrapModal) {
            this.bootstrapModal.show();
        }
    }

    /**
     * Ocultar el modal
     */
    hide() {
        if (this.bootstrapModal) {
            this.bootstrapModal.hide();
        }
    }

    /**
     * Destruir el modal
     */
    destroy() {
        if (this.bootstrapModal) {
            this.bootstrapModal.dispose();
        }
        if (this.modal) {
            this.modal.remove();
        }
    }

    /**
     * Actualizar el contenido del modal
     * @param {string} newBody - Nuevo contenido HTML
     */
    updateBody(newBody) {
        const bodyElement = this.modal.querySelector('.modal-body');
        if (bodyElement) {
            bodyElement.innerHTML = newBody;
        }
    }

    /**
     * Actualizar el título del modal
     * @param {string} newTitle - Nuevo título
     */
    updateTitle(newTitle) {
        const titleElement = this.modal.querySelector('.modal-title');
        if (titleElement) {
            titleElement.innerHTML = newTitle;
        }
    }
}

/**
 * Alert Modal - Modal de confirmación/alerta simplificado
 */
class AlertModal extends RestoModal {
    constructor(options = {}) {
        const defaultOptions = {
            size: 'sm',
            centered: true,
            ...options,
            buttons: {
                aceptar: {
                    label: options.acceptText || 'Aceptar',
                    variant: options.type === 'danger' ? 'danger' : 'primary',
                    icon: options.icon || 'check',
                    dismiss: true,
                    callback: options.onAccept || (() => {}),
                    ...options.buttons?.aceptar
                }
            }
        };
        super(defaultOptions);
    }
}

/**
 * Confirm Modal - Modal de confirmación con dos botones
 */
class ConfirmModal extends RestoModal {
    constructor(options = {}) {
        const defaultOptions = {
            size: 'sm',
            centered: true,
            backdropStatic: true,
            ...options,
            buttons: {
                cancelar: {
                    label: 'Cancelar',
                    variant: 'secondary',
                    icon: 'x',
                    dismiss: true,
                    callback: options.onCancel || (() => {}),
                    ...options.buttons?.cancelar
                },
                aceptar: {
                    label: options.confirmText || 'Aceptar',
                    variant: options.type === 'danger' ? 'danger' : 'primary',
                    icon: options.icon || 'check',
                    dismiss: false,
                    callback: options.onConfirm || (() => {}),
                    ...options.buttons?.aceptar
                }
            }
        };
        super(defaultOptions);
    }
}

/**
 * Success Modal - Modal de éxito
 */
class SuccessModal extends AlertModal {
    constructor(options = {}) {
        const defaultOptions = {
            icon: 'check-circle',
            type: 'success',
            acceptText: 'Aceptar',
            ...options
        };
        super(defaultOptions);
    }
}

/**
 * Error Modal - Modal de error
 */
class ErrorModal extends AlertModal {
    constructor(options = {}) {
        const defaultOptions = {
            icon: 'exclamation-circle',
            type: 'danger',
            acceptText: 'Aceptar',
            ...options
        };
        super(defaultOptions);
    }
}

/**
 * Loading Modal - Modal de carga
 */
class LoadingModal extends RestoModal {
    constructor(options = {}) {
        const defaultOptions = {
            size: 'sm',
            centered: true,
            backdropStatic: true,
            ...options,
            body: `
                <div class="d-flex flex-column align-items-center">
                    <div class="spinner-border text-primary mb-3" role="status" style="width: 3rem; height: 3rem;">
                        <span class="visually-hidden">Cargando...</span>
                    </div>
                    <p class="text-muted">${options.message || 'Cargando...'}</p>
                </div>
            `,
            buttons: {}
        };
        super(defaultOptions);
    }
}

/**
 * Helper functions para modales comunes
 */
const ModalHelper = {
    /**
     * Mostrar alerta
     */
    alert: function(title, message, onClose = null) {
        const modal = new AlertModal({
            title,
            body: message,
            onAccept: onClose
        });
        modal.show();
        return modal;
    },

    /**
     * Mostrar confirmación
     */
    confirm: function(title, message, onConfirm, onCancel = null) {
        const modal = new ConfirmModal({
            title,
            body: message,
            onConfirm,
            onCancel
        });
        modal.show();
        return modal;
    },

    /**
     * Mostrar mensaje de éxito
     */
    success: function(title, message, onClose = null) {
        const modal = new SuccessModal({
            title,
            body: message,
            onAccept: onClose
        });
        modal.show();
        return modal;
    },

    /**
     * Mostrar mensaje de error
     */
    error: function(title, message, onClose = null) {
        const modal = new ErrorModal({
            title,
            body: message,
            onAccept: onClose
        });
        modal.show();
        return modal;
    },

    /**
     * Mostrar modal de carga
     */
    loading: function(message = 'Cargando...') {
        return new LoadingModal({
            title: 'Procesando',
            message
        });
    }
};

// Exportar para uso global
window.RestoModal = RestoModal;
window.AlertModal = AlertModal;
window.ConfirmModal = ConfirmModal;
window.SuccessModal = SuccessModal;
window.ErrorModal = ErrorModal;
window.LoadingModal = LoadingModal;
window.ModalHelper = ModalHelper;
