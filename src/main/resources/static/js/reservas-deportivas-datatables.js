$(document).ready(function () {
    $('.datatables-reservas').DataTable({
        processing: true,
        serverSide: true,
        responsive: true,
        ajax: {
            url: '/admin/reservas-deportivas-datatable',
            type: 'GET'
        },
        columns: [
            {
                data: null,
                searchable: false,
                orderable: false,
                render: function (data, type, row, meta) {
                    return meta.row + 1; // numeración #
                }
            },
            { data: 'nombre' },            // Nombre del espacio
            { data: 'tipo' },              // Tipo de espacio
            { data: 'lugar' },             // Lugar
            {
                data: 'costo',
                render: function (data) {
                    return `S/ ${parseFloat(data || 0).toFixed(2)}`;
                }
            },
            {
                data: 'estadoEspacio',
                render: function (data) {
                    let badgeClass = 'bg-secondary';
                    if (data === 'Disponible') badgeClass = 'bg-success';
                    else if (data === 'Ocupado') badgeClass = 'bg-warning';
                    else if (data === 'Cancelada' || data === 'Rechazada') badgeClass = 'bg-danger';

                    return `<span class="badge ${badgeClass}">${data}</span>`;
                }
            },
            {
                data: 'id',
                render: function (id) {
                    return `
            <a href="/admin/detalles-reserva/${id}" class="btn btn-sm btn-primary" title="Ver Detalles">
              <i class="bx bx-show"></i>
            </a>
          `;
                }
            }
        ],
        order: [[1, 'asc']],
        language: {
            url: '/lang/es-ES.json'
        }
    });
});
