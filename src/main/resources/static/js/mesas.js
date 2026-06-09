new Vue({
    el: "#app",
    data: {
        entidades: [],
        entidad: [],
        mesaSeleccionada: null,
        busqueda: "",
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
    },

    computed: {

        mesasFiltradas() {

            return this.entidades.filter(mesa => {

                const coincideBusqueda =
                        !this.busqueda ||
                        mesa.numeroMesa.toString().includes(this.busqueda);

                const coincideEstado =
                        !this.estadoFiltro ||
                        mesa.estadoMesa.toLowerCase() === this.estadoFiltro;

                return coincideBusqueda && coincideEstado;

            });

        }

    },
    methods: {
        listar: function () {
            this.$http.get('/api/mesas').then(response => {
                this.entidades = response.data;
            });
        },
        nuevo: function () {
            this.entidad = {
                idMesa: 0,
                nombre: "",
                descripcion: "",
                activo: true
            };
            $("#mdlEntidad").modal("show");
        },
        editar: function (idMesa) {
            this.$http.get('/api/mesas/' + idMesa).then(response => {
                this.entidad = response.data;
                $("#mdlEntidad").modal("show");
            });
        },
        guardar: function () {
            //this.entidad.activo = 1;
            this.$http.post('/api/mesas', this.entidad).then(response => {
                this.listar();
                this.cargarResumenEstados();
                $("#mdlEntidad").modal("hide");
            });
        },
        actualizar: function () {
            console.log(this.entidad);
            this.$http.put('/api/mesas/' + this.entidad.idMesa, this.entidad).then(response => {
                this.listar();
                this.cargarResumenEstados();
                $("#mdlEntidad").modal("hide");
            });
        },
        eliminar: function (idMesa) {
            this.entidad.idMesa = idMesa;
            $("#mdlEliminar").modal("show");
        },
        confimareliminacion: function () {
            this.$http.delete('/api/mesas/' + this.entidad.idMesa).then(response => {
                this.listar();
                this.cargarResumenEstados();
                $("#mdlEliminar").modal("hide");
            });
        },
        cargarResumenEstados: function () {

            this.$http.get('/api/mesas/resumen')
                    .then(response => {

                        this.resumen = response.data;

                    });

        },
        limpiarFiltros: function () {

            this.busqueda = "";
            this.estadoFiltro = "";

        },
        seleccionarMesa: function (item) {

            this.mesaSeleccionada = item;

        },
        abrirModalApertura: function () {

            this.cantidadPersonas = 1;

            const modal = new bootstrap.Modal(
                    document.getElementById('mdlAperturaMesa')
                    );

            modal.show();

        },
        confirmarAperturaMesa: function () {

            console.log(
                    "Mesa:",
                    this.mesaSeleccionada.numeroMesa
                    );

            console.log(
                    "Personas:",
                    this.cantidadPersonas
                    );

            const modal =
                    bootstrap.Modal.getInstance(
                            document.getElementById('mdlAperturaMesa')
                            );

            modal.hide();

        },

        abrirModalUnion: function () {

            this.mesasSeleccionadasUnir = [];

            this.mesasParaUnir = this.entidades.filter(mesa =>
                mesa.estadoMesa === 'libre' &&
                        mesa.idMesa !== this.mesaSeleccionada.idMesa

            );

            const modal = new bootstrap.Modal(
                    document.getElementById('mdlUnirMesas')
                    );

            modal.show();

        },
        
        confirmarUnionMesas: function() {

    console.log(
        "Mesa principal:",
        this.mesaSeleccionada.idMesa
    );

    console.log(
        "Mesas unidas:",
        this.mesasSeleccionadasUnir
    );

}
    },
    mounted: function () {
        this.listar();
        this.cargarResumenEstados();
    }
});
