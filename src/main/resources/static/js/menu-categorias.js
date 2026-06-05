/* global Vue, bootstrap, JSON */

const { createApp } = Vue;

createApp({
    data() {
        return {
            
            categorias: [],
            
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
                    data.filter(c => !c.eliminado);

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
        
        limpiarFormulario() {
            this.nuevaCategoria.nombreCategoria = '';
            this.nuevaCategoria.descripcion = '';
        }
    },
    mounted() {
        this.cargarCategorias();
    }
}).mount('#app');