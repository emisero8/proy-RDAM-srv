import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import SolicitudCard from '../../components/SolicitudCard';
import LoadingSpinner from '../../components/LoadingSpinner';
import Modal from '../../components/Modal';
import { ESTADOS } from '../../utils/constants';
import lupaIcon from '../../assets/lupa.png';

export default function Bandeja() {
    const [solicitudes, setSolicitudes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filtroEstado, setFiltroEstado] = useState('PAGADA');
    const [searchInput, setSearchInput] = useState('');
    const [searchQuery, setSearchQuery] = useState('');
    const navigate = useNavigate();
    const debounceRef = useRef(null);

    /* ── Búsqueda avanzada ───────────────────────────────────── */
    const [showAdvanced, setShowAdvanced] = useState(false);
    const [advDni, setAdvDni] = useState('');
    const [advNombre, setAdvNombre] = useState('');
    const [advRef, setAdvRef] = useState('');
    const [advDesde, setAdvDesde] = useState('');
    const [advHasta, setAdvHasta] = useState('');
    // Filtros activos que se aplicaron (tras clickear Buscar)
    const [activeAdvanced, setActiveAdvanced] = useState(null);

    const handleSearchChange = useCallback((e) => {
        const value = e.target.value;
        setSearchInput(value);
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => setSearchQuery(value.trim()), 400);
    }, []);

    useEffect(() => () => clearTimeout(debounceRef.current), []);

    const fetchSolicitudes = async () => {
        setLoading(true);
        try {
            const params = {};
            if (filtroEstado) params.estado = filtroEstado;

            // Si hay búsqueda avanzada activa, usar esos filtros
            if (activeAdvanced) {
                // Enviar el término más específico al backend
                if (activeAdvanced.ref) params.search = activeAdvanced.ref;
                else if (activeAdvanced.dni) params.search = activeAdvanced.dni;
                else if (activeAdvanced.nombre) params.search = activeAdvanced.nombre;
                if (activeAdvanced.fechaDesde) params.fechaDesde = activeAdvanced.fechaDesde;
                if (activeAdvanced.fechaHasta) params.fechaHasta = activeAdvanced.fechaHasta;
            } else if (searchQuery) {
                params.search = searchQuery;
            }

            const { data } = await api.get('/solicitudes', { params });
            let results = Array.isArray(data) ? data : data.content || [];

            // Filtrar en el cliente los criterios extra que no se enviaron al backend
            if (activeAdvanced) {
                const { ref, dni, nombre } = activeAdvanced;
                if (ref) {
                    // ref se envió al backend; filtrar por dni y nombre localmente
                    if (dni) results = results.filter(s => (s.ciudadanoDni || '').includes(dni));
                    if (nombre) results = results.filter(s =>
                        (s.ciudadanoNombre || '').toLowerCase().includes(nombre.toLowerCase()));
                } else if (dni) {
                    // dni se envió al backend; filtrar por nombre localmente
                    if (nombre) results = results.filter(s =>
                        (s.ciudadanoNombre || '').toLowerCase().includes(nombre.toLowerCase()));
                }
            }

            setSolicitudes(results);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchSolicitudes(); }, [filtroEstado, searchQuery, activeAdvanced]);

    const estadoOptions = Object.entries(ESTADOS).map(([key, val]) => ({ value: key, label: val.label }));

    /* ── Handlers búsqueda avanzada ──────────────────────────── */
    const openAdvanced = () => {
        // Pre-rellenar con filtros activos si los hay
        if (activeAdvanced) {
            setAdvDni(activeAdvanced.dni || '');
            setAdvNombre(activeAdvanced.nombre || '');
            setAdvRef(activeAdvanced.ref || '');
            setAdvDesde(activeAdvanced.desdeRaw || '');
            setAdvHasta(activeAdvanced.hastaRaw || '');
        }
        setShowAdvanced(true);
    };

    const handleAdvancedSearch = () => {
        const dniVal = advDni.trim();
        const nombreVal = advNombre.trim();
        const refVal = advRef.trim();

        const filters = {
            dni: dniVal,
            nombre: nombreVal,
            ref: refVal,
            desdeRaw: advDesde,
            hastaRaw: advHasta,
            fechaDesde: advDesde ? `${advDesde}T00:00:00` : '',
            fechaHasta: advHasta ? `${advHasta}T23:59:59` : '',
        };

        // Solo activar si al menos hay un filtro
        const hasAny = dniVal || nombreVal || refVal || filters.fechaDesde || filters.fechaHasta;
        setActiveAdvanced(hasAny ? filters : null);
        // Limpiar búsqueda simple para evitar conflicto
        setSearchInput('');
        setSearchQuery('');
        setShowAdvanced(false);
    };

    const clearAdvanced = () => {
        setActiveAdvanced(null);
        setAdvDni('');
        setAdvNombre('');
        setAdvRef('');
        setAdvDesde('');
        setAdvHasta('');
    };

    const hasActiveFilters = !!activeAdvanced;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">Bandeja de Solicitudes</h1>
                    <p className="page-subtitle">Solicitudes pagadas para emitir certificado</p>
                </div>
            </div>

            <div className="filter-bar">
                <select
                    className="form-select"
                    value={filtroEstado}
                    onChange={(e) => setFiltroEstado(e.target.value)}
                    id="filtro-estado"
                >
                    <option value="">Todos los estados</option>
                    {estadoOptions.map((opt) => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                </select>

                <div className="search-input-wrapper">
                    <img src={lupaIcon} alt="" className="search-icon" />
                    <input
                        type="text"
                        className="form-input"
                        placeholder="Nº Ref. (ej SOL-2026-003)"
                        value={searchInput}
                        onChange={handleSearchChange}
                        id="buscar-referencia"
                        disabled={hasActiveFilters}
                        style={{ paddingLeft: '2.25rem' }}
                    />
                </div>

                <span
                    className="advanced-search-link"
                    onClick={openAdvanced}
                    id="btn-busqueda-avanzada"
                >
                    Búsqueda avanzada
                </span>
            </div>

            {/* Tags de filtros avanzados activos */}
            {hasActiveFilters && (
                <div className="advanced-filters-active">
                    <span className="advanced-filters-label">Filtros activos:</span>
                    {activeAdvanced.dni && (
                        <span className="advanced-filter-tag">DNI: {activeAdvanced.dni}</span>
                    )}
                    {activeAdvanced.nombre && (
                        <span className="advanced-filter-tag">Nombre: {activeAdvanced.nombre}</span>
                    )}
                    {activeAdvanced.ref && (
                        <span className="advanced-filter-tag">Ref: {activeAdvanced.ref}</span>
                    )}
                    {activeAdvanced.desdeRaw && (
                        <span className="advanced-filter-tag">Desde: {activeAdvanced.desdeRaw}</span>
                    )}
                    {activeAdvanced.hastaRaw && (
                        <span className="advanced-filter-tag">Hasta: {activeAdvanced.hastaRaw}</span>
                    )}
                    <button className="btn btn-outline" onClick={clearAdvanced} style={{ padding: '0.2rem 0.6rem', fontSize: 'var(--font-xs)' }}>
                        ✕ Limpiar
                    </button>
                </div>
            )}

            {loading ? (
                <LoadingSpinner />
            ) : solicitudes.length === 0 ? (
                <div className="empty-state">
                    <div className="empty-state-icon">📭</div>
                    <p>
                        {filtroEstado === 'PAGADA' && !searchQuery && !hasActiveFilters
                            ? 'No hay solicitudes pagadas pendientes de emisión.'
                            : 'No hay solicitudes con el filtro seleccionado.'}
                    </p>
                </div>
            ) : (
                <div style={{ display: 'grid', gap: 'var(--space-lg)', maxWidth: '90%', margin: '0 auto' }}>
                    {solicitudes.map((sol) => (
                        <SolicitudCard
                            key={sol.id}
                            solicitud={sol}
                            onClick={() => navigate(`/solicitudes/${sol.id}`)}
                        />
                    ))}
                </div>
            )}

            {/* ── Modal Búsqueda Avanzada ────── */}
            <Modal
                isOpen={showAdvanced}
                onClose={() => setShowAdvanced(false)}
                title="Búsqueda avanzada"
                footer={
                    <>
                        <button className="btn btn-outline" onClick={() => setShowAdvanced(false)}>Cancelar</button>
                        <button className="btn btn-primary" onClick={handleAdvancedSearch}>Buscar</button>
                    </>
                }
            >
                <div className="advanced-search-form">
                    <div className="form-group">
                        <label className="form-label" htmlFor="adv-ref">Nº de Referencia</label>
                        <input
                            id="adv-ref"
                            type="text"
                            className="form-input"
                            placeholder="Ej: SOL-2026-003"
                            value={advRef}
                            onChange={(e) => setAdvRef(e.target.value)}
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="adv-dni">DNI del ciudadano</label>
                        <input
                            id="adv-dni"
                            type="text"
                            className="form-input"
                            placeholder="7 u 8 dígitos"
                            value={advDni}
                            maxLength={8}
                            inputMode="numeric"
                            onChange={(e) => setAdvDni(e.target.value.replace(/\D/g, '').slice(0, 8))}
                        />
                        {advDni && (advDni.length < 7 || advDni.length > 8) && (
                            <small style={{ color: 'var(--color-warning)', fontSize: 'var(--font-xs)', marginTop: '0.25rem' }}>
                                El DNI debe tener 7 u 8 dígitos
                            </small>
                        )}
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="adv-nombre">Nombre del ciudadano</label>
                        <input
                            id="adv-nombre"
                            type="text"
                            className="form-input"
                            placeholder="Solo letras"
                            value={advNombre}
                            onChange={(e) => setAdvNombre(e.target.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]/g, ''))}
                        />
                    </div>

                    <div className="advanced-search-dates">
                        <div className="form-group">
                            <label className="form-label" htmlFor="adv-desde">Fecha desde</label>
                            <input
                                id="adv-desde"
                                type="date"
                                className="form-input"
                                value={advDesde}
                                onChange={(e) => setAdvDesde(e.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label" htmlFor="adv-hasta">Fecha hasta</label>
                            <input
                                id="adv-hasta"
                                type="date"
                                className="form-input"
                                value={advHasta}
                                onChange={(e) => setAdvHasta(e.target.value)}
                            />
                        </div>
                    </div>
                </div>
            </Modal>
        </div>
    );
}


