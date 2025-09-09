"use strict";

$(function () {
    const estrellasActivas = $(".half-star-ratings");
    const estrellasReadOnly = $(".rateyo-readonly");
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    // Modo interactivo
    estrellasActivas.each(function () {
        const $el = $(this);
        const rating = parseFloat($el.data("rating")) || 0;
        const idReserva = $el.data("reserva-id");
        $el.rateYo({
            rtl: false,
            rating: rating,
            fullStar: false,
            halfStar: true,
            readOnly: rating > 0,
            starWidth: "24px",
            spacing: "4px",
            onSet: function (rating, rateYoInstance) {
                if (rating > 0 && idReserva) {
                    fetch('/calificacion/guardar', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded',
                            [header]: token
                        },
                        body: new URLSearchParams({
                            idReserva: idReserva,
                            puntaje: rating
                        })
                    })
                        .then(res => res.text())
                        .then(msg => {
                            if (msg === 'ok') {
                                Swal.fire("¡Gracias!", "Tu calificación fue registrada con éxito.", "success");
                                $el.rateYo("option", "readOnly", true);
                            } else {
                                Swal.fire("Error", msg, "error");
                            }
                        })
                        .catch(() => {
                            Swal.fire("Error", "No se pudo guardar la calificación.", "error");
                        });
                }
            }
        });
    });

    // Modo solo lectura
    estrellasReadOnly.each(function () {
        const $el = $(this);
        const rating = parseFloat($el.data("rating")) || 0;

        $el.rateYo({
            rtl: false,
            rating: rating,
            fullStar: false,
            halfStar: true,
            readOnly: true,
            starWidth: "24px",
            spacing: "4px"
        });
    });
});
