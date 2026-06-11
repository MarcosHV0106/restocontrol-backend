const {createApp} = Vue;

createApp({

    data() {
        return {

            alimentos: [],
            categorias: [],

            categoriaSeleccionada: null,

            pedido: [],
            fechaActual: ""

        };
    },

    computed: {

        alimentosFiltrados() {

            if (!this.categoriaSeleccionada) {
                return this.alimentos;
            }

            return this.alimentos.filter(a =>
                a.categoria &&
                        a.categoria.idCategoria === this.categoriaSeleccionada
            );
        },
        subtotal() {

            return this.pedido.reduce(
                    (total, item) =>
                total + (item.precio * item.cantidad),
                    0
                    );

        },
        igv() {

            return this.subtotal * 0.18;

        },
        total() {

            return this.subtotal + this.igv;

        }


    },

    methods: {

        async cargarAlimentos() {

            try {

                const response = await fetch('/api/alimentos');

                if (!response.ok) {
                    throw new Error();
                }

                this.alimentos = await response.json();

            } catch (error) {

                console.error(error);

            }

        },

        async cargarCategorias() {

            try {

                const response = await fetch('/api/categorias');

                if (!response.ok) {
                    throw new Error();
                }

                this.categorias = await response.json();

            } catch (error) {

                console.error(error);

            }

        },

        seleccionarCategoria(idCategoria) {

            this.categoriaSeleccionada = idCategoria;

        },
        agregarProducto(alimento) {

            const existente = this.pedido.find(
                    p => p.idAlimento === alimento.idAlimento
            );

            if (existente) {

                existente.cantidad++;

            } else {

                this.pedido.push({
                    idAlimento: alimento.idAlimento,
                    nombreAlimento: alimento.nombreAlimento,
                    precio: alimento.precio,
                    cantidad: 1
                });

            }

        },
        aumentarCantidad(item) {

            item.cantidad++;

        },
        disminuirCantidad(item) {

            item.cantidad--;

            if (item.cantidad <= 0) {

                this.pedido =
                        this.pedido.filter(
                                p => p.idAlimento !== item.idAlimento
                        );

            }

        },
        obtenerFechaActual() {

            const hoy = new Date();

            const dia = String(hoy.getDate()).padStart(2, '0');
            const mes = String(hoy.getMonth() + 1).padStart(2, '0');
            const anio = hoy.getFullYear();

            this.fechaActual = `${dia}/${mes}/${anio}`;

        }

    },

    mounted() {
        const app = document.getElementById("app");

        this.idMesa =
                app.dataset.idMesa;

        this.personas =
                app.dataset.personas;

        this.obtenerFechaActual();
        this.cargarCategorias();
        this.cargarAlimentos();

    }

}).mount("#app");