$(document).ready(function () {
    $('.export-opcion').on('click', function () {
        const formato = $(this).data('formato');
        const tabla = $('.dataTable').DataTable();
        const filtros = tabla.ajax.params();

        const queryParams = new URLSearchParams({
            formato: formato,
            search: filtros.search.value || ''
        });

        window.location.href = `/admin/exportar-datatable?${queryParams.toString()}`;
    });
});
