/* global Vue, axios, JSON */

const { createApp } = Vue;

createApp({

    data() {

        return {

            correo: '',
            clave: '',
            mensajeError: ''

        };

    },

    methods: {

        async iniciarSesion() {

            try {

                const response =
                    await fetch(
                        '/api/auth/login',
                        {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                correo: this.correo,
                                clave: this.clave
                            })
                        }
                    );

                if (!response.ok) {
                    throw new Error();
                }

                const data = await response.json();

                localStorage.setItem(
                    'usuario',
                    JSON.stringify(data.usuario)
                );

                window.location.href =
                    '/dashboard';

            } catch(error) {

                this.mensajeError =
                    'Credenciales inválidas';

            }
        }

    }

}).mount('#app');