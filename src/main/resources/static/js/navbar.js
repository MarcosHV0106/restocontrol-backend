document.addEventListener("DOMContentLoaded", () => {

    const fechaActualElement = document.getElementById("fechaActual");
    const horaLoginElement = document.getElementById("horaLogin");
    const btnCerrarSesion = document.getElementById("btnCerrarSesion");

    let timer = null;

    function actualizarFecha() {

        const ahora = new Date();

        const opciones = {
            weekday: "long",
            year: "numeric",
            month: "long",
            day: "numeric"
        };

        const fechaFormateada = ahora.toLocaleDateString(
            "es-ES",
            opciones
        );

        if (fechaActualElement) {
            fechaActualElement.textContent =
                fechaFormateada.charAt(0).toUpperCase() +
                fechaFormateada.slice(1);
        }
    }

    function inicializarHoraLogin() {

        let loginDateTime =
            sessionStorage.getItem("restoControl_loginTime");

        if (!loginDateTime) {

            loginDateTime = new Date().toISOString();

            sessionStorage.setItem(
                "restoControl_loginTime",
                loginDateTime
            );
        }

        const fechaLogin = new Date(loginDateTime);

        const hora = fechaLogin.toLocaleTimeString(
            "es-ES",
            {
                hour: "2-digit",
                minute: "2-digit",
                hour12: true
            }
        );

        if (horaLoginElement) {
            horaLoginElement.textContent = hora;
        }
    }

    async function cerrarSesion() {

        if (!confirm("¿Desea cerrar sesión?")) {
            return;
        }

        try {

            await fetch("/logout", {
                method: "POST"
            });

        } catch (error) {

            console.warn("Error en logout:", error);

        } finally {

            sessionStorage.removeItem(
                "restoControl_loginTime"
            );

            localStorage.clear();

            window.location.href =
                "/login?logout";
        }
    }

    actualizarFecha();

    timer = setInterval(
        actualizarFecha,
        60000
    );

    inicializarHoraLogin();

    if (btnCerrarSesion) {
        btnCerrarSesion.addEventListener(
            "click",
            cerrarSesion
        );
    }

    window.addEventListener("beforeunload", () => {

        if (timer) {
            clearInterval(timer);
        }

    });

});