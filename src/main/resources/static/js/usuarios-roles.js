/* global Vue, bootstrap, JSON, confirm, alert */

const { createApp } = Vue;

createApp({
    data() {
        return {
            tabActiva: 'usuarios',
            usuarios: [],
            roles: [],
            busquedaUsuario: '',
            busquedaRol: '',
            modoEdicionUsuario: false,
            usuarioEditandoId: null,
            modoEdicionRol: false,
            rolEditandoId: null,
            nuevoUsuario: {
                nombre: '',
                apellido: '',
                correo: '',
                clave: '',
                idRol: ''
            },
            nuevoRol: {
                nombreRol: '',
                descripcion: ''
            }
        };
    },
    computed: {
        usuariosFiltrados() {
            const termino = this.busquedaUsuario.trim().toLowerCase();

            if (!termino) {
                return this.usuarios;
            }

            return this.usuarios.filter(usuario => {
                const rol = usuario.rol ? usuario.rol.nombreRol : '';

                return [
                    usuario.nombre,
                    usuario.apellido,
                    usuario.correo,
                    rol
                ].some(valor => (valor || '').toLowerCase().includes(termino));
            });
        },
        rolesFiltrados() {
            const termino = this.busquedaRol.trim().toLowerCase();

            if (!termino) {
                return this.roles;
            }

            return this.roles.filter(rol => {
                return [
                    rol.nombreRol,
                    rol.descripcion
                ].some(valor => (valor || '').toLowerCase().includes(termino));
            });
        }
    },
    methods: {
        async cargarUsuarios() {
            try {
                const response = await fetch('/api/usuarios');

                if (!response.ok) {
                    throw new Error('Error al obtener usuarios');
                }

                this.usuarios = await response.json();
            } catch (error) {
                console.error('Error cargando usuarios:', error);
                alert('No se pudieron cargar los usuarios.');
            }
        },
        async cargarRoles() {
            try {
                const response = await fetch('/api/roles');

                if (!response.ok) {
                    throw new Error('Error al obtener roles');
                }

                this.roles = await response.json();
            } catch (error) {
                console.error('Error cargando roles:', error);
                alert('No se pudieron cargar los roles.');
            }
        },
        async guardarUsuario() {
            if (!this.validarUsuario()) {
                return;
            }

            const payload = {
                nombre: this.nuevoUsuario.nombre.trim(),
                apellido: this.nuevoUsuario.apellido.trim(),
                correo: this.nuevoUsuario.correo.trim(),
                idRol: Number(this.nuevoUsuario.idRol)
            };

            const clave = this.nuevoUsuario.clave.trim();

            if (!this.modoEdicionUsuario || clave) {
                payload.clave = clave;
            }

            try {
                const url = this.modoEdicionUsuario
                    ? `/api/usuarios/${this.usuarioEditandoId}`
                    : '/api/usuarios';
                const method = this.modoEdicionUsuario ? 'PUT' : 'POST';

                const response = await fetch(url, {
                    method,
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                if (!response.ok) {
                    const mensaje = await this.obtenerMensajeError(response);
                    alert(mensaje || 'No se pudo guardar el usuario.');
                    return;
                }

                await this.cargarUsuarios();
                this.cerrarModal('usuarioModal');
                this.reiniciarModalUsuario();
            } catch (error) {
                console.error('Error guardando usuario:', error);
                alert('Error de comunicacion con el servidor.');
            }
        },
        abrirEditarUsuario(usuario) {
            this.modoEdicionUsuario = true;
            this.usuarioEditandoId = usuario.idUsuario;
            this.nuevoUsuario = {
                nombre: usuario.nombre || '',
                apellido: usuario.apellido || '',
                correo: usuario.correo || '',
                clave: '',
                idRol: usuario.rol ? usuario.rol.idRol : ''
            };

            const modal = new bootstrap.Modal(
                document.getElementById('usuarioModal')
            );
            modal.show();
        },
        async eliminarUsuario(usuario) {
            const nombreCompleto = `${usuario.nombre} ${usuario.apellido}`.trim();

            if (!confirm(`Deseas eliminar a "${nombreCompleto}"?`)) {
                return;
            }

            try {
                const response = await fetch(
                    `/api/usuarios/${usuario.idUsuario}`,
                    {
                        method: 'DELETE'
                    }
                );

                if (!response.ok) {
                    const mensaje = await this.obtenerMensajeError(response);
                    alert(mensaje || 'No se pudo eliminar el usuario.');
                    return;
                }

                await this.cargarUsuarios();
            } catch (error) {
                console.error('Error eliminando usuario:', error);
                alert('Error de comunicacion con el servidor.');
            }
        },
        validarUsuario() {
            if (!this.nuevoUsuario.nombre.trim()) {
                alert('El nombre del usuario es obligatorio.');
                return false;
            }

            if (!this.nuevoUsuario.apellido.trim()) {
                alert('El apellido del usuario es obligatorio.');
                return false;
            }

            if (!this.nuevoUsuario.correo.trim()) {
                alert('El correo es obligatorio.');
                return false;
            }

            if (!this.nuevoUsuario.correo.includes('@')) {
                alert('El correo no tiene un formato valido.');
                return false;
            }

            if (!this.modoEdicionUsuario && !this.nuevoUsuario.clave.trim()) {
                alert('La clave del usuario es obligatoria.');
                return false;
            }

            if (!this.nuevoUsuario.idRol) {
                alert('El rol del usuario es obligatorio.');
                return false;
            }

            return true;
        },
        reiniciarModalUsuario() {
            this.modoEdicionUsuario = false;
            this.usuarioEditandoId = null;
            this.nuevoUsuario = {
                nombre: '',
                apellido: '',
                correo: '',
                clave: '',
                idRol: ''
            };
        },
        async guardarRol() {
            if (!this.validarRol()) {
                return;
            }

            const payload = {
                nombreRol: this.nuevoRol.nombreRol.trim(),
                descripcion: this.nuevoRol.descripcion
                    ? this.nuevoRol.descripcion.trim()
                    : ''
            };

            try {
                const url = this.modoEdicionRol
                    ? `/api/roles/${this.rolEditandoId}`
                    : '/api/roles';
                const method = this.modoEdicionRol ? 'PUT' : 'POST';

                const response = await fetch(url, {
                    method,
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                if (!response.ok) {
                    const mensaje = await this.obtenerMensajeError(response);
                    alert(mensaje || 'No se pudo guardar el rol.');
                    return;
                }

                await this.cargarRoles();
                await this.cargarUsuarios();
                this.cerrarModal('rolModal');
                this.reiniciarModalRol();
            } catch (error) {
                console.error('Error guardando rol:', error);
                alert('Error de comunicacion con el servidor.');
            }
        },
        abrirEditarRol(rol) {
            this.modoEdicionRol = true;
            this.rolEditandoId = rol.idRol;
            this.nuevoRol = {
                nombreRol: rol.nombreRol || '',
                descripcion: rol.descripcion || ''
            };

            const modal = new bootstrap.Modal(
                document.getElementById('rolModal')
            );
            modal.show();
        },
        async eliminarRol(rol) {
            const usuariosAsignados = this.contarUsuariosPorRol(rol.idRol);

            if (usuariosAsignados > 0) {
                alert('No puedes eliminar un rol con usuarios asignados. Reasigna esos usuarios primero.');
                return;
            }

            if (!confirm(`Deseas eliminar el rol "${rol.nombreRol}"?`)) {
                return;
            }

            try {
                const response = await fetch(
                    `/api/roles/${rol.idRol}`,
                    {
                        method: 'DELETE'
                    }
                );

                if (!response.ok) {
                    const mensaje = await this.obtenerMensajeError(response);
                    alert(mensaje || 'No se pudo eliminar el rol.');
                    return;
                }

                await this.cargarRoles();
            } catch (error) {
                console.error('Error eliminando rol:', error);
                alert('Error de comunicacion con el servidor.');
            }
        },
        validarRol() {
            if (!this.nuevoRol.nombreRol.trim()) {
                alert('El nombre del rol es obligatorio.');
                return false;
            }

            return true;
        },
        reiniciarModalRol() {
            this.modoEdicionRol = false;
            this.rolEditandoId = null;
            this.nuevoRol = {
                nombreRol: '',
                descripcion: ''
            };
        },
        contarUsuariosPorRol(idRol) {
            return this.usuarios.filter(usuario => {
                return usuario.rol && usuario.rol.idRol === idRol;
            }).length;
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
        this.cargarRoles();
        this.cargarUsuarios();

        const modalUsuario = document.getElementById('usuarioModal');
        const modalRol = document.getElementById('rolModal');

        if (modalUsuario) {
            modalUsuario.addEventListener(
                'hidden.bs.modal',
                () => this.reiniciarModalUsuario()
            );
        }

        if (modalRol) {
            modalRol.addEventListener(
                'hidden.bs.modal',
                () => this.reiniciarModalRol()
            );
        }
    }
}).mount('#app');
