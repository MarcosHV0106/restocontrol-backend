/* global Vue, axios, JSON */

const { createApp } = Vue;

createApp({

    data() {

        return {

            correo: '',
            clave: '',
            mensajeError: '',
            showPassword: false,
            isLoading: false,
            emailHistory: [],
            showEmailSuggestions: false,
            emailSuggestions: [],
            windowWidth: typeof window !== 'undefined' ? window.innerWidth : 1024

        };

    },

    mounted() {

        // Cargar histórico de emails desde localStorage
        this.loadEmailHistory();

        // Detectar cambios en el tamaño de la ventana
        window.addEventListener('resize', () => {
            this.windowWidth = window.innerWidth;
        });

        // Auto-enfoque en el primer input
        setTimeout(() => {
            const emailInput = document.getElementById('correoInput');
            if (emailInput) emailInput.focus();
        }, 100);

    },

    methods: {

        loadEmailHistory() {

            try {
                const history = localStorage.getItem('loginEmailHistory');
                this.emailHistory = history ? JSON.parse(history) : [];
            } catch(e) {
                this.emailHistory = [];
            }

        },

        saveEmailToHistory(email) {

            if (!email || !email.includes('@')) return;

            // Remover duplicado si existe
            this.emailHistory = this.emailHistory.filter(e => e !== email);

            // Agregar al inicio
            this.emailHistory.unshift(email);

            // Mantener solo los últimos 10 emails
            this.emailHistory = this.emailHistory.slice(0, 10);

            // Guardar en localStorage
            localStorage.setItem('loginEmailHistory', JSON.stringify(this.emailHistory));

        },

        handleEmailInput() {

            if (!this.correo) {
                this.emailSuggestions = [];
                return;
            }

            const input = this.correo.toLowerCase();
            this.emailSuggestions = this.emailHistory.filter(email =>
                email.toLowerCase().includes(input)
            );

        },

        selectEmail(email) {

            this.correo = email;
            this.showEmailSuggestions = false;
            this.emailSuggestions = [];
            document.getElementById('passwordInput')?.focus();

        },

        async iniciarSesion() {

            // Validaciones
            if (!this.correo || !this.clave) {
                this.mensajeError = 'Por favor completa todos los campos';
                return;
            }

            if (!this.correo.includes('@')) {
                this.mensajeError = 'Por favor ingresa un correo válido';
                return;
            }

            this.isLoading = true;
            this.mensajeError = '';

            try {

                const response = await fetch(
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

                // Guardar en localStorage
                localStorage.setItem('usuario', JSON.stringify(data.usuario));

                // Guardar email en histórico
                this.saveEmailToHistory(this.correo);

                // Guardar fecha y hora de login
                localStorage.setItem('loginDateTime', new Date().toISOString());

                // Esperar un poco antes de redirigir (para mejorar UX)
                setTimeout(() => {
                    window.location.href = '/dashboard';
                }, 500);

            } catch(error) {

                this.mensajeError = 'Credenciales inválidas. Intenta de nuevo.';
                this.isLoading = false;

            }

        }

    }

}).mount('#app');