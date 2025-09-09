function getLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            function(position) {
                document.getElementById("latitud").value = position.coords.latitude;
                document.getElementById("longitud").value = position.coords.longitude;
                document.getElementById("attendanceForm").submit();
            },
            function(error) {
                alert("Error al obtener la geolocalización: " + error.message);
            }
        );
    } else {
        alert("La geolocalización no es compatible con este navegador.");
    }
}