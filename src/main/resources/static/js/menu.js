/* global Vue, bootstrap, JSON */

const { createApp } = Vue;

createApp({
    data() {
        return {
            categorias: [],
            modoEdicion: false,
            categoriaEditandoId: null,
            nuevaCategoria: {
                nombreCategoria: '',
                descripcion: ''
            }
        };
    },
    methods: {

        async cargarCategorias() {
            try {
                const response =
                    await fetch('/api/categorias');
                if (!response.ok) {
                    throw new Error('Error al obtener categorías');
                }
                const data =
                    await response.json();
                this.categorias =
                    data;
            } catch(error) {
                console.error(
                    'Error cargando categorías:',
                    error
                );
            }
        },
        async registrarCategoria() {
            if (!this.nuevaCategoria.nombreCategoria.trim()) {
                alert('El nombre de la categoría es obligatorio.');
                return;
            }
            try {
                const payload = {
                    nombreCategoria: this.nuevaCategoria.nombreCategoria.trim(),
                    descripcion: this.nuevaCategoria.descripcion ? this.nuevaCategoria.descripcion.trim() : null
                };
                const response = await fetch('/api/categorias', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });
                if (response.status === 201) {
                    alert('Categoría registrada correctamente.');
                    
                    this.limpiarFormulario();

                    const modalElement = document.getElementById('registrarCategoriaModal');
                    const modalInstance = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
                    modalInstance.hide();

                    await this.cargarCategorias();
                } else {
                    const errorData = await response.json().catch(() => null);
                    if (errorData && errorData.message) {
                        alert('Error: ' + errorData.message);
                    } else {
                        alert('No se pudo registrar. Verifique si el nombre ya existe.');
                    }
                }
            } catch (error) {
                console.error('Error al registrar la categoría:', error);
                alert('Error de comunicación con el servidor. Inténtelo más tarde.');
            }
        },
        async guardarCategoria() {
            if(this.modoEdicion){
                await this.actualizarCategoria();
            } else {
                await this.registrarCategoria();
            }
        },
        async actualizarCategoria() {
            try {
                const response =
                    await fetch(
                        `/api/categorias/${this.categoriaEditandoId}`,
                        {
                            method: 'PUT',
                            headers: {
                                'Content-Type':
                                    'application/json'
                            },
                            body: JSON.stringify(
                                this.nuevaCategoria
                            )
                        }
                    );
                if(response.ok){
                    await this.cargarCategorias();
                    this.limpiarFormulario();
                    bootstrap.Modal
                        .getInstance(
                            document.getElementById(
                                'registrarCategoriaModal'
                            )
                        )
                        .hide();
                }
            } catch(error){
                console.error(error);
            }
        },
        abrirEditar(categoria) {

            if (categoria.eliminado) {
                return;
            }

            this.modoEdicion = true;
            this.categoriaEditandoId =
                categoria.idCategoria;
            this.nuevaCategoria = {
                nombreCategoria:
                    categoria.nombreCategoria,
                descripcion:
                    categoria.descripcion
            };
            const modal =
                new bootstrap.Modal(
                    document.getElementById(
                        'registrarCategoriaModal'
                    )
                );
            modal.show();
        },
        reiniciarModal() {
            this.modoEdicion = false;
            this.categoriaEditandoId = null;
            this.nuevaCategoria = {
                nombreCategoria: '',
                descripcion: ''
            };
        },
        async cambiarEstado(categoria) {
            try {
                const response =
                    await fetch(
                        `/api/categorias/${categoria.idCategoria}/estado`,
                        {
                            method: 'PATCH'
                        }
                    );
                if(response.ok){
                    await this.cargarCategorias();
                }
            } catch(error){
                console.error(error);
            }
        },
        limpiarFormulario() {
            this.reiniciarModal();
        }
    },
    mounted() {

        this.cargarCategorias();

        const modal =
            document.getElementById(
                'registrarCategoriaModal'
            );

        modal.addEventListener(
            'hidden.bs.modal',
            () => {
                this.reiniciarModal();
            }
        );

    }
}).mount('#app');