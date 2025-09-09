$(document).ready(function () {
    const table = $('.datatables-users').DataTable({
        processing: true,
        serverSide: true,
        responsive: true,
        ajax: {
            url: '/admin/usuarios-datatable',
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
            { data: 'nombreCompleto' },
            { data: 'correo' },
            { data: 'rol' },
            {
                data: 'activo',
                render: function (data) {
                    return data
                        ? '<span class="badge bg-success">Activo</span>'
                        : '<span class="badge bg-secondary">Inactivo</span>';
                }
            },
            {
                data: 'idUsuarios',
                render: function (data, type, row) {
                    return `
                        <button class="btn btn-sm btn-primary btn-editar" data-id="${data}">
                            Editar
                        </button>
                        <button class="btn btn-sm btn-danger btn-eliminar" data-id="${data}">
                            Eliminar
                        </button>
                    `;
                }
            }
        ],
        order: [[1, 'asc']],
        language: {
            url: '/lang/es-ES.json'
        }
    });

    // Modal para editar rol
    $('body').append(`
        <div class="modal fade" id="editarRolModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Editar Rol de Usuario</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <p>Editando rol de: <span id="nombreUsuario"></span></p>
                        <select class="form-select" id="selectRol">
                            <option value="1">Usuario final</option>
                            <option value="2">Coordinador</option>
                        </select>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-primary" id="guardarRol">Guardar</button>
                    </div>
                </div>
            </div>
        </div>
    `);

    let usuarioIdActual;

    // Manejar clic en botón editar
    $(document).on('click', '.btn-editar', function() {
        usuarioIdActual = $(this).data('id');
        $.get(`/admin/usuarios/editar/${usuarioIdActual}`, function(data) {
            $('#nombreUsuario').text(data.nombreCompleto);
            $('#selectRol').val(data.rol);
            $('#editarRolModal').modal('show');
        });
    });

    // Guardar cambios de rol
    $('#guardarRol').click(function() {
        const rolId = $('#selectRol').val();
        $.ajax({
            url: `/admin/usuarios/editar/${usuarioIdActual}`,
            method: 'POST',
            data: {
                rolId: rolId,
                _csrf: $('meta[name="_csrf"]').attr('content')
            },
            success: function() {
                $('#editarRolModal').modal('hide');
                table.ajax.reload();
            }
        });
    });

    // Manejar clic en botón eliminar
    $(document).on('click', '.btn-eliminar', function() {
        const id = $(this).data('id');
        if (confirm('¿Está seguro de que desea eliminar este usuario? Esta acción no se puede deshacer.')) {
            $.ajax({
                url: `/admin/usuarios/eliminar/${id}`,
                method: 'DELETE',
                headers: {
                    'X-CSRF-TOKEN': $('meta[name="_csrf"]').attr('content')
                },
                success: function() {
                    table.ajax.reload();
                }
            });
        }
    });
});
