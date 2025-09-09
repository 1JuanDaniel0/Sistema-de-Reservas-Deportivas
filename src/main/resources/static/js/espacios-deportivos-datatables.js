$(document).ready(function () {
    $('.datatables-servicios').DataTable({
        processing: true,
        serverSide: true,
        responsive: true,
        ajax: {
            url: '/admin/espacios-deportivos-datatable',
            type: 'GET'
        },
        columns: [
            {
                data: null,
                searchable: false,
                orderable: false,
                render: function (data, type, row, meta) {
                    return meta.row + 1;
                }
            },
            { data: 'nombre' },
            { data: 'nombreTipo' },
            { data: 'nombreLugar' },
            {
                data: 'costo',
                render: function (data) {
                    return `S/ ${parseFloat(data || 0).toFixed(2)}`;
                }
            },
            {
                data: 'estadoEspacio',
                render: function (data) {
                    return `<span class="badge bg-${data === 'Disponible' ? 'success' : 'secondary'}">${data}</span>`;
                }
            },
            {
                data: 'idEspacio',
                render: function (id) {
                    return `
                        <a href="/admin/detalles-espacio?id=${id}" class="btn btn-sm btn-primary me-1" title="Ver Detalles"><i class="bx bx-show"></i></a>
                        <a href="/admin/editar-espacio/${id}" class="btn btn-sm btn-warning" title="Editar"><i class="bx bx-edit-alt"></i></a>
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
