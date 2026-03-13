import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import SolicitudCard from '../../components/SolicitudCard';
import LoadingSpinner from '../../components/LoadingSpinner';
import { ESTADOS } from '../../utils/constants';

export default function Bandeja() {
    const [solicitudes, setSolicitudes] = useState([]);
    const [loading, setLoading] = useState(true);
    // Mostrar PAGADAS por defecto: son las que el gestor debe procesar
    const [filtroEstado, setFiltroEstado] = useState('PAGADA');
    const navigate = useNavigate();

    const fetchSolicitudes = async () => {
        setLoading(true);
        try {
            const params = {};
            if (filtroEstado) params.estado = filtroEstado;
            const { data } = await api.get('/solicitudes', { params });
            setSolicitudes(Array.isArray(data) ? data : data.content || []);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchSolicitudes(); }, [filtroEstado]);

    const estadoOptions = Object.entries(ESTADOS).map(([key, val]) => ({ value: key, label: val.label }));

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
            </div>

            {loading ? (
                <LoadingSpinner />
            ) : solicitudes.length === 0 ? (
                <div className="empty-state">
                    <div className="empty-state-icon">📭</div>
                    <p>
                        {filtroEstado === 'PAGADA'
                            ? 'No hay solicitudes pagadas pendientes de emisión.'
                            : 'No hay solicitudes con el filtro seleccionado.'}
                    </p>
                </div>
            ) : (
                <div style={{ display: 'grid', gap: 'var(--space-lg)' }}>
                    {solicitudes.map((sol) => (
                        <SolicitudCard
                            key={sol.id}
                            solicitud={sol}
                            onClick={() => navigate(`/solicitudes/${sol.id}`)}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}
