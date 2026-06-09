// logout.js

document.addEventListener("DOMContentLoaded", () => {

    const btnLogout =
        document.getElementById("btnLogout");

    if (!btnLogout) {
        return;
    }

    btnLogout.addEventListener("click", () => {

        if (!confirm("¿Desea cerrar sesión?")) {
            return;
        }

        localStorage.removeItem("usuario");

        window.location.href = "/login";

    });

});