"use strict";
let direction = "ltr";
isRtl && (direction = "rtl"),
  document.addEventListener("DOMContentLoaded", function () {
    {
      
      const v = document.getElementById("calendar"),
        m = document.querySelector(".app-calendar-sidebar"),
        p = document.getElementById("addEventSidebar"),
        f = document.querySelector(".app-overlay"),
        g = {
          Business: "primary",
          Holiday: "success",
          Personal: "danger",
          Family: "warning",
          ETC: "info",
        },
        b = document.querySelector(".offcanvas-title"),
        h = document.querySelector(".btn-toggle-sidebar"),
        y = document.querySelector('button[type="submit"]'),
        L = document.querySelector(".btn-cancel"),
        k = document.querySelector("#eventStartDate"),
        w = document.querySelector("#eventEndDate"),
        PM = document.querySelector("#paymentMethod"),
        Y = document.querySelector(".inline-calendar");
      let a,
        l = events,
        r = !1,
        e;
      const C = new bootstrap.Offcanvas(p);
      var d, o;
      function s() {
        var e = document.querySelector(".fc-sidebarToggle-button");
        for (
          e.classList.remove("fc-button-primary"),
            e.classList.add("d-lg-none", "d-inline-block", "ps-0");
          e.firstChild;

        )
          e.firstChild.remove();
        e.setAttribute("data-bs-toggle", "sidebar"),
          e.setAttribute("data-overlay", ""),
          e.setAttribute("data-target", "#app-calendar-sidebar"),
          e.insertAdjacentHTML(
            "beforeend",
            '<i class="bx bx-menu bx-sm text-heading"></i>'
          );
      }
        k &&
          (d = k.flatpickr({
            enableTime: !0,
            altFormat: "Y-m-dTH:i:S",
            onReady: function (e, t, n) {
              n.isMobile && n.mobileInput.setAttribute("step", null);
            },
          })),
        w &&
          (o = w.flatpickr({
            enableTime: !0,
            altFormat: "Y-m-dTH:i:S",
            onReady: function (e, t, n) {
              n.isMobile && n.mobileInput.setAttribute("step", null);
            },
          })),
        Y && (e = Y.flatpickr({ monthSelectorType: "static", inline: !0 }));
      let i = new Calendar(v, {
        initialView: "dayGridMonth",
        events: function (e, t) {
          let n = (function () {
            let t = [],
              e = [].slice.call(
                document.querySelectorAll(".input-filter:checked")
              );
            return (
              e.forEach((e) => {
                t.push(e.getAttribute("data-value"));
              }),
              t
            );
          })();
          t(
            l.filter(function (e) {
              return n.includes(e.extendedProps.calendar.toLowerCase());
            })
          );
        },
        plugins: [dayGridPlugin, interactionPlugin, listPlugin, timegridPlugin],
        editable: !0,
        dragScroll: !0,
        dayMaxEvents: 2,
        eventResizableFromStart: !0,
        customButtons: { sidebarToggle: { text: "Barra lateral" } },
        headerToolbar: {
          start: "sidebarToggle, prev,next, title",
          end: "dayGridMonth,timeGridWeek,timeGridDay,listMonth",
        },
        direction: direction,
        initialDate: new Date(),
        navLinks: !0,
        eventClassNames: function ({ event: e }) {
          return ["fc-event-" + g[e._def.extendedProps.calendar]];
        },
        dateClick: function (e) {
          e = moment(e.date).format("YYYY-MM-DD");
          u(),
            C.show(),
            b && (b.innerHTML = "Reservar Espacio"),
            (y.innerHTML = "Agregar"),
            y.classList.remove("btn-update-event"),
            y.classList.add("btn-add-event"),
            S.classList.add("d-none"),
            (k.value = e),
            (w.value = e);
        },
        datesSet: function () {
          s();
        },
        viewDidMount: function () {
          s();
        },
        locale: 'es', // Configura FullCalendar en español
  
      });
      i.render(), s();
      var c = document.getElementById("eventForm");
      function u() {
        w.value = "";
          k.value = "";
          PM.value = "online";
      }
      FormValidation.formValidation(c, {
        fields: {
          eventStartDate: {
            validators: { notEmpty: { message: "Por favor, ingrese la fecha de inicio " } },
          },
          eventEndDate: {
            validators: { notEmpty: { message: "Por favor, ingrese la fecha de fin " } },
          },
        },
        plugins: {
          trigger: new FormValidation.plugins.Trigger(),
          bootstrap5: new FormValidation.plugins.Bootstrap5({
            eleValidClass: "",
            rowSelector: function (e, t) {
              return ".mb-3";
            },
          }),
          submitButton: new FormValidation.plugins.SubmitButton(),
          autoFocus: new FormValidation.plugins.AutoFocus(),
        },
      })
        .on("core.form.valid", function () {
          r = !0;
        })
        .on("core.form.invalid", function () {
          r = !1;
        }),
        h &&
          h.addEventListener("click", (e) => {
            L.classList.remove("d-none");
          }),
        y.addEventListener("click", (e) => {

          if (r) {
            const spaceName = "Espacio 1";
            const startDate = k.value;
            const endDate = w.value;
            const paymentMethod = PM.value === "online" ? "Pago en línea" : "Depósito bancario";
          
            const reservationData = {
              spaceName: spaceName,
              startDate: startDate,
              endDate: endDate,
              paymentMethod: paymentMethod
            };
            localStorage.setItem("reservationData", JSON.stringify(reservationData));
            window.location.href = "";
            C.hide();
          }
        }),
        p.addEventListener("hidden.bs.offcanvas", function () {
          u();
        }),
        h.addEventListener("click", (e) => {
          b && (b.innerHTML = "Reservar Espacio"),
            (y.innerHTML = "Agregar"),
            y.classList.remove("btn-update-event"),
            y.classList.add("btn-add-event"),
            m.classList.remove("show"),
            f.classList.remove("show");
        }),
        e.config.onChange.push(function (e) {
          i.changeView(i.view.type, moment(e[0]).format("YYYY-MM-DD")),
            s(),
            m.classList.remove("show"),
            f.classList.remove("show");
        });
    }
  });
