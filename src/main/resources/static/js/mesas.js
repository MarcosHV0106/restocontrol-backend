new Vue({
    el: "#app",
    data: {
        entidades: [],
        entidad: []
    },
    methods: {
        listar: function () {
            this.$http.get('/api/mesas').then(response => {
                this.entidades = response.data;
            });
        },
        nuevo: function () {
            this.entidad = {
                id: 0,
                nombre: "",
                descripcion: "",
                activo: true
            };
            $("#mdlEntidad").modal("show");
        },
        editar: function (id) {
            this.$http.get('/api/mesas/' + id).then(response => {
                this.entidad = response.data;
                $("#mdlEntidad").modal("show");
            });
        },
        guardar: function () {
            //this.entidad.activo = 1;
            this.$http.post('/api/mesas', this.entidad).then(response => {
                this.listar();
                $("#mdlEntidad").modal("hide");
            });
        },
        actualizar: function () {
            this.$http.put('/api/mesas/' + this.entidad.id, this.entidad).then(response => {
                this.listar();
                $("#mdlEntidad").modal("hide");
            });
        },
        eliminar: function (id) {
            this.entidad.id = id;
            $("#mdlEliminar").modal("show");
        },
        confimareliminacion: function () {
            this.$http.delete('/api/mesas/' + this.entidad.id).then(response => {
                this.listar();
                $("#mdlEliminar").modal("hide");
            });
        },
    },
    mounted: function () {
        this.listar();
    }
});