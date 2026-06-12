const {createApp} = Vue;
createApp({

    data() {
        return {

            entidades: [],

            entidad: {
                idMesa: 0,
                nombre: "",
                descripcion: "",
                activo: true
            },

            mesaSeleccionada: null,

            busqueda: "",
            pisoFiltro: "",
            estadoFiltro: "",

            cantidadPersonas: 1,

            mesasParaUnir: [],
            mesasSeleccionadasUnir: [],

            resumen: {
                libres: 0,
                ocupadas: 0,
                reservadas: 0,
                cobradas: 0
            }

        };
    },

    computed: {

        mesasFiltradas() {

            return this.entidades.filter(mesa => {

                const coincideBusqueda =
                        !this.busqueda ||
                        mesa.numeroMesa
                        .toString()
                        .includes(this.busqueda);

                const coincideEstado =
                        !this.estadoFiltro ||
                        mesa.estadoMesa.toLowerCase() === this.estadoFiltro;

                const coincidePiso =
                        !this.pisoFiltro ||
                        mesa.piso == this.pisoFiltro;



                return coincideBusqueda && coincideEstado && coincidePiso;

            });

        },
        pisosDisponibles() {

            const pisos =
                    this.entidades.map(mesa => mesa.piso);

            return [...new Set(pisos)]
                    .sort((a, b) => a - b);

        }


    },

    methods: {

        async listar() {

            try {

                const response = await fetch('/api/mesas');

                if (!response.ok) {
                    throw new Error('Error al listar mesas');
                }

                this.entidades = await response.json();
                console.log(this.entidades);

                for (const mesa of this.entidades) {

                    const response =
                            await fetch(`/api/pedidos/mesa/${mesa.idMesa}`);
                    const texto =
                            await response.text();

                    mesa.pedido =
                            texto ? JSON.parse(texto) : null;

                }

            } catch (error) {

                console.error(error);

            }

        },

        nuevo() {

            this.entidad = {
                idMesa: 0,
                numeroMesa: "",
                capacidad: "",
                piso: "",
                estadoMesa: "libre",
                eliminado: false
            };

            const modal = new bootstrap.Modal(
                    document.getElementById('mdlEntidad')
                    );

            modal.show();

        },

        async editar(idMesa) {

            try {

                const response =
                        await fetch(`/api/mesas/${idMesa}`);

                if (!response.ok) {
                    throw new Error('Error al obtener mesa');
                }

                this.entidad = await response.json();

                const modal = new bootstrap.Modal(
                        document.getElementById('mdlEntidad')
                        );

                modal.show();

            } catch (error) {

                console.error(error);

            }

        },

        async guardar() {

            try {

                const response = await fetch('/api/mesas', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(this.entidad)
                });

                if (!response.ok) {
                    throw new Error('Error al guardar');
                }

                await this.listar();
                await this.cargarResumenEstados();

                bootstrap.Modal
                        .getInstance(
                                document.getElementById('mdlEntidad')
                                )
                        ?.hide();

            } catch (error) {

                console.error(error);

            }

        },

        async actualizar() {

            try {

                const response = await fetch(
                        `/api/mesas/${this.entidad.idMesa}`,
                        {
                            method: 'PUT',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(this.entidad)
                        }
                );

                if (!response.ok) {
                    throw new Error('Error al actualizar');
                }

                await this.listar();
                await this.cargarResumenEstados();

                bootstrap.Modal
                        .getInstance(
                                document.getElementById('mdlEntidad')
                                )
                        ?.hide();

            } catch (error) {

                console.error(error);

            }

        },

        eliminar(idMesa) {

            this.entidad.idMesa = idMesa;

            const modal = new bootstrap.Modal(
                    document.getElementById('mdlEliminar')
                    );

            modal.show();

        },

        async confimareliminacion() {

            try {

                const response = await fetch(
                        `/api/mesas/${this.entidad.idMesa}`,
                        {
                            method: 'DELETE'
                        }
                );

                if (!response.ok) {
                    throw new Error('Error al eliminar');
                }

                await this.listar();
                await this.cargarResumenEstados();

                bootstrap.Modal
                        .getInstance(
                                document.getElementById('mdlEliminar')
                                )
                        ?.hide();

            } catch (error) {

                console.error(error);

            }

        },

        async cargarResumenEstados() {

            try {

                const response =
                        await fetch('/api/mesas/resumen');

                if (!response.ok) {
                    throw new Error('Error al cargar resumen');
                }

                this.resumen = await response.json();

            } catch (error) {

                console.error(error);

            }

        },

        limpiarFiltros() {

            this.busqueda = "";
            this.estadoFiltro = "";
            this.pisoFiltro = "";
        },

        seleccionarMesa(item) {

            this.mesaSeleccionada = item;

        },

        abrirModalApertura() {

            this.cantidadPersonas = 1;

            const modal = new bootstrap.Modal(
                    document.getElementById('mdlAperturaMesa')
                    );

            modal.show();

        },

        confirmarAperturaMesa() {

            const idMesa = this.mesaSeleccionada.idMesa;
            const personas = this.cantidadPersonas;

            window.location.href =
                    `/pedidos?idMesa=${idMesa}&personas=${personas}`;

        },

        abrirModalUnion() {

            this.mesasSeleccionadasUnir = [];

            this.mesasParaUnir =
                    this.entidades.filter(mesa =>
                        mesa.estadoMesa === 'libre' &&
                                mesa.idMesa !== this.mesaSeleccionada?.idMesa
                    );

            const modal = new bootstrap.Modal(
                    document.getElementById('mdlUnirMesas')
                    );

            modal.show();

        },

        confirmarUnionMesas() {

            console.log(
                    'Mesa principal:',
                    this.mesaSeleccionada?.idMesa
                    );

            console.log(
                    'Mesas unidas:',
                    this.mesasSeleccionadasUnir
                    );

        }

    },

    async mounted() {

        await this.listar();

        await this.cargarResumenEstados();

    }

}).mount('#app');