document.addEventListener('DOMContentLoaded', function() {
    const filterToggleBtn = document.getElementById('filterToggleBtn');
    const filterPanel = document.getElementById('filterPanel');
    const filterOverlay = document.getElementById('filterOverlay');
    const filterCloseBtn = document.getElementById('filterCloseBtn');
    const clearFiltersBtn = document.getElementById('clearFiltersBtn');

    // Función para abrir el panel
    function openFilterPanel() {
        filterToggleBtn.classList.add('active');
        filterOverlay.classList.add('show');

        setTimeout(() => {
            filterPanel.classList.add('show', 'bounce-in');
        }, 50);

        document.body.style.overflow = 'hidden';
    }

    // Función para cerrar el panel
    function closeFilterPanel() {
        filterToggleBtn.classList.remove('active');
        filterPanel.classList.remove('show', 'bounce-in');
        filterOverlay.classList.remove('show');
        document.body.style.overflow = '';
    }

    // Event listeners
    filterToggleBtn.addEventListener('click', function() {
        if (filterPanel.classList.contains('show')) {
            closeFilterPanel();
        } else {
            openFilterPanel();
        }
    });

    filterCloseBtn.addEventListener('click', closeFilterPanel);
    filterOverlay.addEventListener('click', closeFilterPanel);

    // Limpiar filtros
    clearFiltersBtn.addEventListener('click', function() {
        const form = this.closest('form');
        form.reset();
        closeFilterPanel();
        window.location.href = '/vecino/espacios-disponibles';
    });

    // Cerrar con tecla Escape
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && filterPanel.classList.contains('show')) {
            closeFilterPanel();
        }
    });

    // Animación del botón al hacer hover
    filterToggleBtn.addEventListener('mouseenter', function() {
        if (!this.classList.contains('active')) {
            this.style.transform = 'translateY(-2px) scale(1.1)';
        }
    });

    filterToggleBtn.addEventListener('mouseleave', function() {
        if (!this.classList.contains('active')) {
            this.style.transform = '';
        }
    });
});