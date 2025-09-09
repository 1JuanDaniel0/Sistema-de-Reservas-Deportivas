"use strict"
let direction = "ltr";
if (isRtl) { direction = "rtl";}
let calendar; // Variable global para el calendario
function initializeCalendar() {
    console.log("Inicializando calendario...");
    console.log("Eventos disponibles:", window.reservasEvents);
    const calendarEl = document.getElementById("calendar");
    const sidebar = document.querySelector(".app-calendar-sidebar");
    const eventSidebar = document.getElementById("addEventSidebar");
    const overlay = document.querySelector(".app-overlay");
    // Colores para diferentes estados de reserva
    const estadoColors = {'confirmada': 'success', 'no_confirmada': 'warning', 'cancelada': 'danger', 'pasada': 'secondary'};
    const sidebarTitle = document.querySelector(".offcanvas-title");
    const toggleSidebarBtn = document.querySelector(".btn-toggle-sidebar");
    // Elementos del formulario (solo para visualización)
    const eventTitle = document.querySelector("#eventTitle");
    const eventStartDate = document.querySelector("#eventStartDate");
    const eventEndDate = document.querySelector("#eventEndDate");
    const eventLocation = document.querySelector("#eventLocation");
    const eventDescription = document.querySelector("#eventDescription");
    const offcanvas = new bootstrap.Offcanvas(eventSidebar);
    let selectedEvent;
    // Función para configurar el botón toggle del sidebar
    function setupSidebarToggle() {
        const toggleBtn = document.querySelector(".fc-sidebarToggle-button");
        if (toggleBtn) {
            toggleBtn.classList.remove("fc-button-primary");
            toggleBtn.classList.add("d-lg-none", "d-inline-block", "ps-0");
            // Limpiar contenido existente
            while (toggleBtn.firstChild) { toggleBtn.firstChild.remove();}
            toggleBtn.setAttribute("data-bs-toggle", "sidebar");
            toggleBtn.setAttribute("data-overlay", "");
            toggleBtn.setAttribute("data-target", "#app-calendar-sidebar");
            toggleBtn.insertAdjacentHTML("beforeend", '<i class="bx bx-menu bx-sm text-heading"></i>');
        }
    }
    // Función para obtener filtros activos
    function getActiveFilters() {
        const activeFilters = [];
        const filterInputs = document.querySelectorAll(".input-filter:checked");
        filterInputs.forEach(input => {
            activeFilters.push(input.getAttribute("data-value"));
        });
        return activeFilters;
    }
    // Inicializar el calendario
    calendar = new Calendar(calendarEl, {
        initialView: "timeGridWeek", // dayGridMonth para mostrar el mes completo
        events: function(fetchInfo, successCallback) {
            console.log("Función events llamada");
            // Asegurarse de que la variable existe, aunque esté vacía
            if (!window.reservasEvents) {
                console.log("No hay eventos disponibles, inicializando array vacío");
                window.reservasEvents = [];
            }
            console.log("Eventos disponibles:", window.reservasEvents);
            // Si no hay eventos, devolver array vacío
            if (window.reservasEvents.length === 0) {
                console.log("No hay eventos para mostrar");
                successCallback([]);
                return;
            }
            // Filtrar eventos según los filtros seleccionados
            const activeFilters = getActiveFilters();
            console.log("Filtros activos:", activeFilters);
            // Si no hay filtros activos, mostrar todos los eventos
            if (activeFilters.length === 0) {
                console.log("No hay filtros activos, mostrando todos los eventos");
                successCallback(window.reservasEvents);
                return;
            }
            const filteredEvents = window.reservasEvents.filter(function(event) {
                const isIncluded = activeFilters.includes(event.extendedProps.calendar);
                console.log(`Evento ${event.title} - calendario: ${event.extendedProps.calendar} - incluido: ${isIncluded}`);
                return isIncluded;
            });
            console.log("Eventos filtrados:", filteredEvents);
            successCallback(filteredEvents);
        },
        locale: "es",
        slotMinTime: "08:00:00",
        slotMaxTime: "23:00:00",
        slotDuration: "00:30:00",
        slotLabelInterval: "01:00:00",
        slotLabelFormat: {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
        },
        plugins: [dayGridPlugin, interactionPlugin, listPlugin, timegridPlugin],
        editable: false,
        dragScroll: true,
        dayMaxEvents: 3,
        customButtons: {
            sidebarToggle: {
                text: "Sidebar"
            }
        },
        headerToolbar: {
            start: "sidebarToggle, prev,next, title",
            end: "dayGridMonth,timeGridWeek,timeGridDay,listMonth"
        },
        direction: direction,
        initialDate: new Date(),
        navLinks: true,
        eventClassNames: function({event}) {
            const estado = event._def.extendedProps.calendar;
            return ["fc-event-" + (estadoColors[estado] || 'info')];
        },
        eventClick: function(info) {
            const props = info.event.extendedProps;
            if (!props.esPropia) {
                console.log("Reserva ajena seleccionada. No se abrirá el offcanvas.");
                return; // ❌ No hacer nada si no es del usuario
            }
            selectedEvent = info.event;
            console.log("Evento propio clickeado:", selectedEvent);
            // Mostrar información de la reserva en el sidebar
            offcanvas.show();

            if (sidebarTitle) {
                sidebarTitle.innerHTML = "Información de Reserva";
            }
            // Llenar los campos con información de la reserva
            if (eventTitle) eventTitle.value = `Reserva #${selectedEvent.extendedProps.reservaId}`;
            if (eventStartDate) eventStartDate.value = moment(selectedEvent.start).format('YYYY-MM-DD HH:mm');
            if (eventEndDate) eventEndDate.value = moment(selectedEvent.end).format('YYYY-MM-DD HH:mm');
            if (eventLocation) eventLocation.value = selectedEvent.extendedProps.vecino;
            if (eventDescription) eventDescription.value = `Estado: ${selectedEvent.extendedProps.estado}\nCosto: $${selectedEvent.extendedProps.costo}\nTipo de Pago: ${selectedEvent.extendedProps.tipoPago}\nMomento de Reserva: ${moment(selectedEvent.extendedProps.momentoReserva).format('DD/MM/YYYY HH:mm')}`;
            // Deshabilitar todos los campos
            if (eventTitle) eventTitle.disabled = true;
            if (eventStartDate) eventStartDate.disabled = true;
            if (eventEndDate) eventEndDate.disabled = true;
            if (eventLocation) eventLocation.disabled = true;
            if (eventDescription) eventDescription.disabled = true;
            // Ocultar botones de acción
            const btnAdd = document.querySelector(".btn-add-event");
            const btnDelete = document.querySelector(".btn-delete-event");
            if (btnAdd) btnAdd.style.display = "none";
            if (btnDelete) btnDelete.style.display = "none";
            const btnCancel = document.querySelector(".btn-cancel");
            if (btnCancel) btnCancel.textContent = "Cerrar";
        },
        datesSet: function() {
            setupSidebarToggle();
        },
        viewDidMount: function() {
            setupSidebarToggle();
        },
        eventDidMount: function(info) {
            const props = info.event.extendedProps;
            if (props.esPropia) {
                const horaInicio = info.event.start.toLocaleTimeString('es-PE', { hour: 'numeric', hour12: true });
                const horaFin = info.event.end.toLocaleTimeString('es-PE', { hour: 'numeric', hour12: true });
                const detalle = `
                    ${props.estado} | ${props.vecino}
                    ${horaInicio} a ${horaFin}
                    Costo: S/. ${props.costo}
                    Pago: ${props.tipoPago}
                `.trim();
                new bootstrap.Tooltip(info.el, {
                    title: detalle,
                    placement: "top",
                    trigger: "hover",
                    container: "body",
                    animation: true,
                    customClass: "tooltip-fade-reserva"
                });
            }
        },
        dayCellDidMount: function(info) {
            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0); // Normalizar
            const fechaCelda = new Date(info.date);
            fechaCelda.setHours(0, 0, 0, 0);
            if (fechaCelda < hoy) {
                info.el.classList.add("fc-day-disabled");
            }
        },
        selectAllow: function(selectInfo) {
            const ahora = new Date();
            const hoy = ahora.toISOString().split("T")[0]; // Formato YYYY-MM-DD
            const inicio = selectInfo.start;
            const esHoy = inicio.toISOString().startsWith(hoy);
            // Si es de hoy y la hora seleccionada es anterior a la actual
            if (esHoy && inicio < ahora) {
                console.log("Bloqueado: intervalo pasado");
                return false;
            }
            // Si es día pasado (por seguridad)
            if (selectInfo.start < ahora.setHours(0, 0, 0, 0)) {
                console.log("Bloqueado: fecha pasada");
                return false;
            }
            return true;
        },
        slotLabelDidMount: function(info) {
            const slotTime = info.date;
            const ahora = new Date();
            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);
            if (
                slotTime.getDate() === ahora.getDate() &&
                slotTime.getMonth() === ahora.getMonth() &&
                slotTime.getFullYear() === ahora.getFullYear() &&
                slotTime < ahora
            ) {
                info.el.classList.add("fc-slot-disabled");
            }
        },
        // Agregar evento para manejar clics en fechas vacías
        dateClick: function(info) {
            console.log("Fecha clickeada:", info.dateStr);
            const fechaSeleccionada = info.date;
            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);
            // ✅ No permitir seleccionar días pasados
            if (fechaSeleccionada < hoy) {
                console.log("Fecha pasada, ignorando.");
                return;
            }
            // 📌 Abrir el offcanvas
            const crearReservaOffcanvas = new bootstrap.Offcanvas(document.getElementById('crearReservaOffcanvas'));
            crearReservaOffcanvas.show();
            // 🗓️ Mostrar la fecha
            const fechaStr = moment(fechaSeleccionada).format("YYYY-MM-DD");
            document.getElementById("reservaFecha").value = fechaStr;
            // 👤 Mostrar vecino desde variable JS global
            document.getElementById("reservaVecino").value = window.usuarioSesionNombre || "Desconocido";
            // 📍 Mostrar espacio desde variable JS global
            document.getElementById("reservaEspacio").value = window.espacioSeleccionadoNombre || "Espacio actual";
            // ⏰ Llenar horas de inicio y fin (enteras)
            const horaInicioSelect = document.getElementById("reservaHoraInicio");
            const horaFinSelect = document.getElementById("reservaHoraFin");
            // Limpiar selects
            horaInicioSelect.innerHTML = "";
            horaFinSelect.innerHTML = "";
            for (let h = 8; h < 23; h++) {
                const horaTexto = `${h.toString().padStart(2, '0')}:00`;
                const optionInicio = new Option(horaTexto, horaTexto);
                const optionFin = new Option(horaTexto, horaTexto);
                horaInicioSelect.appendChild(optionInicio);
                horaFinSelect.appendChild(optionFin);
            }
            // 💰 Costo dinámico según horas
            function calcularCosto() {
                const inicio = horaInicioSelect.value;
                const fin = horaFinSelect.value;
                if (inicio && fin) {
                    const hi = parseInt(inicio.split(":")[0]);
                    const hf = parseInt(fin.split(":")[0]);
                    const duracion = hf - hi;
                    const costoHora = parseFloat(window.espacioSeleccionadoCostoHora || 0);
                    const total = duracion > 0 ? duracion * costoHora : 0;
                    document.getElementById("reservaCosto").value = `S/. ${total.toFixed(2)}`;
                }
            }
            horaInicioSelect.addEventListener("change", calcularCosto);
            horaFinSelect.addEventListener("change", calcularCosto);
            // 💳 Cambiar texto del botón según tipo de pago
            const tipoPagoSelect = document.getElementById("reservaTipoPago");
            const btnCrearReserva = document.getElementById("btnCrearReserva");
            function actualizarTextoBoton() {
                if (tipoPagoSelect.value === "linea") {
                    btnCrearReserva.textContent = "Pagar y reservar";
                } else {
                    btnCrearReserva.textContent = "Crear reserva";
                }
            }
            tipoPagoSelect.addEventListener("change", actualizarTextoBoton);
            actualizarTextoBoton(); // Inicial
            horaInicioSelect.value = "08:00";
            horaFinSelect.value = "09:00";
            calcularCosto();
        }
    });
    console.log("Renderizando calendario...");
    calendar.render();
    setupSidebarToggle();
    // Configurar filtros
    const selectAllCheckbox = document.querySelector(".select-all");
    const filterInputs = document.querySelectorAll(".input-filter");
    // Marcar todos los filtros como activos por defecto
    if (selectAllCheckbox) {
        selectAllCheckbox.checked = true;
    }
    filterInputs.forEach(input => {
        input.checked = true;
    });
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener("click", function() {
            const isChecked = this.checked;
            filterInputs.forEach(input => {
                input.checked = isChecked;
            });
            if (calendar) {
                calendar.refetchEvents();
            }
        });
    }
    filterInputs.forEach(input => {
        input.addEventListener("click", function() {
            const checkedInputs = document.querySelectorAll(".input-filter:checked");
            if (selectAllCheckbox) {
                selectAllCheckbox.checked = checkedInputs.length === filterInputs.length;
            }
            if (calendar) {
                calendar.refetchEvents();
            }
        });
    });
    // Configurar el botón toggle del sidebar
    if (toggleSidebarBtn) {
        toggleSidebarBtn.addEventListener("click", function() {
            if (sidebarTitle) {
                sidebarTitle.innerHTML = "Información de Reservas";
            }
            if (sidebar) sidebar.classList.remove("show");
            if (overlay) overlay.classList.remove("show");
        });
    }
    // Limpiar campos cuando se cierra el sidebar
    if (eventSidebar) {
        eventSidebar.addEventListener("hidden.bs.offcanvas", function() {
            if (eventTitle) {
                eventTitle.disabled = false;
                eventTitle.value = "";
            }
            if (eventStartDate) {
                eventStartDate.disabled = false;
                eventStartDate.value = "";
            }
            if (eventEndDate) {
                eventEndDate.disabled = false;
                eventEndDate.value = "";
            }
            if (eventLocation) {
                eventLocation.disabled = false;
                eventLocation.value = "";
            }
            if (eventDescription) {
                eventDescription.disabled = false;
                eventDescription.value = "";
            }
            const btnAdd = document.querySelector(".btn-add-event");
            const btnCancel = document.querySelector(".btn-cancel");
            if (btnAdd) btnAdd.style.display = "inline-block";
            if (btnCancel) btnCancel.textContent = "Cancel";
        });
    }
}
// Inicializar el calendario cuando los datos estén listos
document.addEventListener("DOMContentLoaded", function() {
    console.log("DOM cargado para calendario");
    // Escuchar el evento de datos listos
    window.addEventListener('reservasDataReady', function() {
        console.log("Datos de reservas listos, inicializando calendario");
        initializeCalendar();
    });
    // Fallback: si los datos ya están disponibles o no hay datos
    setTimeout(function() {
        if (typeof window.reservasEvents !== 'undefined') {
            console.log("Datos ya disponibles, inicializando calendario inmediatamente");
            initializeCalendar();
        } else {
            console.log("No hay datos disponibles, inicializando calendario con array vacío");
            window.reservasEvents = [];
            initializeCalendar();
        }
    }, 1000); // Esperar 1 segundo para asegurar que todo esté cargado
});