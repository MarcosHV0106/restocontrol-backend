/* global Vue, bootstrap, JSON, confirm, alert */

const { createApp } = Vue;

createApp({
    data() {
        return {
            alimentos: [],
            categorias: [],
            busquedaAlimento: '',
            busquedaCategoria: '',
            modoEdicionAlimento: false,
            alimentoEditandoId: null,
            modoEdicion: false,
            categoriaEditandoId: null,
            nuevoAlimento: {
                nombreAlimento: '',
                descripcion: '',
                precio: '',
                disponible: true,
                idCategoria: ''
            },
            nuevaCategoria: {
                nombreCategoria: '',
                descripcion: ''
            }
        };
    },
    computed: {
        categoriasActivas() {
            return this.categorias.filter(
                categoria => !categoria.eliminado
            );
        },
        alimentosFiltrados() {
            const termino = this.busquedaAlimento.trim().toLowerCase();

            if (!termino) {
                return this.alimentos;
            }

            return this.alimentos.filter(alimento => {
                const categoria = alimento.categoria
                    ? alimento.categoria.nombreCategoria
                    : '';

                return [
                    alimento.nombreAlimento,
                    alimento.descripcion,
                    categoria
                ].some(valor => (valor || '').toLowerCase().includes(termino));
            });
        },
        categoriasFiltradas() {
            const termino = this.busquedaCategoria.trim().toLowerCase();

            if (!termino) {
                return this.categorias;
            }

            return this.categorias.filter(categoria => {
                return [
                    categoria.nombreCategoria,
                    categoria.descripcion
                ].some(valor => (valor || '').toLowerCase().includes(termino));
            });
        }
    },
    methods: {
        async cargarAlimentos() {
            try {
                const response = await fetch('/api/alimentos');

                if (!response.ok) {
                    throw new Error('Error al obtener alimentos');
                }

                this.alimentos = await response.json();
            } catch (error) {
                console.error('Error cargando alimentos:', error);
                alert('No se pudieron cargar los alimentos.');
            }
        },
        async cargarCategorias() {
            try {
                const response = await fetch('/api/categorias');

                if (!response.ok) {
                    throw new Error('Error al obtener categorias');
                }

                this.categorias = await response.json();
            } catch(error) {
                console.error('Error cargando categorias:', error);
                alert('No se pudieron cargar las categorias.');
            }
        },
        async guardarAlimento() {
            if (!this.validarAlimento()) {
                return;
            }

            const payload = {
                nombreAlimento: this.nuevoAlimento.nombreAlimento.trim(),
                descripcion: this.nuevoAlimento.descripcion
                    ? this.nuevoAlimento.descripcion.trim()
                    : null,
                precio: Number(this.nuevoAlimento.precio),
                disponible: Boolean(this.nuevoAlimento.disponible),
                idCategoria: Number(this.nuevoAlimento.idCategoria)
            };

            try {
                const url = this.modoEdicionAlimento
                    ? `/api/alimentos/${this.alimentoEditandoId}`
                    : '/api/alimentos';
                const method = this.modoEdicionAlimento ? 'PUT' : 'POST';

                const response = await fetch(url, {
                    method,
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                if (!response.ok) {
                    const mensaje = await this.obtenerMensajeError(response);
                    alert(mensaje || 'No se pudo guardar el alimento.');
                    return;
                }

                await this.cargarAlimentos();
                this.cerrarModal('registrarAlimentoModal');
                this.reiniciarModalAlimento();
            } catch (error) {
                console.error('Error guardando alimento:', error);
                alert('Error de comunicacion con el servidor.');
            }
        },
        abrirEditarAlimento(alimento) {
            this.modoEdicionAlimento = true;
            this.alimentoEditandoId = alimento.idAlimento;
            this.nuevoAlimento = {
                nombreAlimento: alimento.nombreAlimento || '',
                descripcion: alimento.descripcion || '',
                precio: alimento.precio || '',
                disponible: alimento.disponible !== false,
                idCategoria: alimento.categoria
                    ? alimento.categoria.idCategoria
                    : ''
            };

            const modal = new bootstrap.Modal(
                document.getElementById('registrarAlimentoModal')
            );
            modal.show();
        },
        async eliminarAlimento(alimento) {
            if (!confirm(`Deseas eliminar "${alimento.nombreAlimento}"?`)) {
                return;
            }

            try {
                const response = await fetch(
                    `/api/alimentos/${alimento.idAlimento}`,
                    {
                        method: 'DELETE'
                    }
                );

                if (!response.ok) {
                    const mensaje = await this.obtenerMensajeError(response);
                    alert(mensaje || 'No se pudo eliminar el alimento.');
                    return;
                }

                await this.cargarAlimentos();
            } catch (error) {
                console.error('Error eliminando alimento:', error);
                alert('Error de comunicacion con el servidor.');
            }
        },
        validarAlimento() {
            if (!this.nuevoAlimento.nombreAlimento.trim()) {
                alert('El nombre del alimento es obligatorio.');
                return false;
            }

            if (!this.nuevoAlimento.idCategoria) {
                alert('La categoria del alimento es obligatoria.');
                return false;
            }

            if (!this.nuevoAlimento.precio || Number(this.nuevoAlimento.precio) <= 0) {
                alert('El precio debe ser mayor a cero.');
                return false;
            }

            return true;
        },
        reiniciarModalAlimento() {
            this.modoEdicionAlimento = false;
            this.alimentoEditandoId = null;
            this.nuevoAlimento = {
                nombreAlimento: '',
                descripcion: '',
                precio: '',
                disponible: true,
                idCategoria: ''
            };
        },
        formatearPrecio(precio) {
            const numero = Number(precio || 0);
            return numero.toFixed(2);
        },
        contarAlimentosPorCategoria(idCategoria) {
            return this.alimentos.filter(alimento => {
                return alimento.categoria
                    && alimento.categoria.idCategoria === idCategoria;
            }).length;
        },
        async registrarCategoria() {
            if (!this.nuevaCategoria.nombreCategoria.trim()) {
                alert('El nombre de la categoria es obligatorio.');
                return;
            }

            try {
                const payload = {
                    nombreCategoria: this.nuevaCategoria.nombreCategoria.trim(),
                    descripcion: this.nuevaCategoria.descripcion
                        ? this.nuevaCategoria.descripcion.trim()
                        : null
                };
                const response = await fetch('/api/categorias', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                if (response.status === 201) {
                    this.cerrarModal('registrarCategoriaModal');
                    this.limpiarFormulario();
                    await this.cargarCategorias();
                    return;
                }

                const mensaje = await this.obtenerMensajeError(response);
                alert(mensaje || 'No se pudo registrar la categoria.');
            } catch (error) {
                console.error('Error al registrar la categoria:', error);
                alert('Error de comunicacion con el servidor.');
            }
        },
        async guardarCategoria() {
            if (this.modoEdicion) {
                await this.actualizarCategoria();
            } else {
                await this.registrarCategoria();
            }
        },
        async actualizarCategoria() {
            if (!this.nuevaCategoria.nombreCategoria.trim()) {
                alert('El nombre de la categoria es obligatorio.');
                return;
            }

            try {
                const response = await fetch(
                    `/api/categorias/${this.categoriaEditandoId}`,
                    {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            nombreCategoria: this.nuevaCategoria.nombreCategoria.trim(),
                            descripcion: this.nuevaCategoria.descripcion
                                ? this.nuevaCategoria.descripcion.trim()
                                : null
                        })
                    }
                );

                if (!response.ok) {
                    const mensaje = await this.obtenerMensajeError(response);
                    alert(mensaje || 'No se pudo actualizar la categoria.');
                    return;
                }

                await this.cargarCategorias();
                this.cerrarModal('registrarCategoriaModal');
                this.limpiarFormulario();
            } catch(error) {
                console.error('Error actualizando categoria:', error);
                alert('Error de comunicacion con el servidor.');
            }
        },
        abrirEditar(categoria) {
            if (categoria.eliminado) {
                return;
            }

            this.modoEdicion = true;
            this.categoriaEditandoId = categoria.idCategoria;
            this.nuevaCategoria = {
                nombreCategoria: categoria.nombreCategoria,
                descripcion: categoria.descripcion || ''
            };

            const modal = new bootstrap.Modal(
                document.getElementById('registrarCategoriaModal')
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
                const response = await fetch(
                    `/api/categorias/${categoria.idCategoria}/estado`,
                    {
                        method: 'PATCH'
                    }
                );

                if (!response.ok) {
                    const mensaje = await this.obtenerMensajeError(response);
                    alert(mensaje || 'No se pudo cambiar el estado.');
                    return;
                }

                await this.cargarCategorias();
            } catch(error) {
                console.error('Error cambiando estado de categoria:', error);
                alert('Error de comunicacion con el servidor.');
            }
        },
        limpiarFormulario() {
            this.reiniciarModal();
        },
        cerrarModal(idModal) {
            const modalElement = document.getElementById(idModal);
            const modalInstance = bootstrap.Modal.getInstance(modalElement)
                || new bootstrap.Modal(modalElement);

            modalInstance.hide();
        },
        async obtenerMensajeError(response) {
            const errorData = await response.json().catch(() => null);

            if (!errorData) {
                return null;
            }

            return errorData.mensaje || errorData.message || null;
        }
    },
    mounted() {
        this.cargarCategorias();
        this.cargarAlimentos();

        const modalCategoria = document.getElementById('registrarCategoriaModal');
        const modalAlimento = document.getElementById('registrarAlimentoModal');

        if (modalCategoria) {
            modalCategoria.addEventListener(
                'hidden.bs.modal',
                () => this.reiniciarModal()
            );
        }

        if (modalAlimento) {
            modalAlimento.addEventListener(
                'hidden.bs.modal',
                () => this.reiniciarModalAlimento()
            );
        }
    }
}).mount('#app');
