import { useState, useEffect } from 'react';
import api from '../../api/axios';
import LoadingSpinner from '../../components/LoadingSpinner';
import { formatCurrency } from '../../utils/constants';

function MetricasTab() {
    const [resumen, setResumen] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        api.get('/reportes/resumen')
            .then(({ data }) => setResumen(data))
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    if (loading) return <LoadingSpinner />;
    if (!resumen) return <div className="empty-state"><p>No se pudo cargar el resumen</p></div>;

    const stats = resumen.solicitudes || {};

    return (
        <>
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-value">{stats.total || 0}</div>
                    <div className="stat-label">Solicitudes Totales</div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{stats.pendiente_revision || 0}</div>
                    <div className="stat-label">Pendientes Revisión</div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{stats.en_revision || 0}</div>
                    <div className="stat-label">En Revisión</div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{stats.emitidas || 0}</div>
                    <div className="stat-label">Emitidas</div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{stats.rechazadas || 0}</div>
                    <div className="stat-label">Rechazadas</div>
                </div>
            </div>

            <div className="card">
                <h3 className="card-title" style={{ marginBottom: 'var(--space-lg)' }}>Desglose por Estado</h3>
                <div className="table-container">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>Estado</th>
                                <th>Cantidad</th>
                            </tr>
                        </thead>
                        <tbody>
                            {Object.entries(stats)
                                .filter(([key]) => key !== 'total')
                                .map(([key, value]) => (
                                    <tr key={key}>
                                        <td style={{ textTransform: 'capitalize' }}>{key.replace(/_/g, ' ')}</td>
                                        <td><strong>{value}</strong></td>
                                    </tr>
                                ))}
                        </tbody>
                    </table>
                </div>
                {resumen.ingresos_mes != null && (
                    <div style={{ marginTop: 'var(--space-lg)', textAlign: 'right' }}>
                        <span className="detail-label">Ingresos del Período: </span>
                        <span style={{ fontSize: 'var(--font-xl)', fontWeight: 700, color: 'var(--color-success)' }}>
                            {formatCurrency(resumen.ingresos_mes)}
                        </span>
                    </div>
                )}
            </div>
        </>
    );
}

function CentroAyudaTab() {
    const [tickets, setTickets] = useState([]);
    const [loading, setLoading] = useState(true);
    const [expandedId, setExpandedId] = useState(null);

    const fetchTickets = () => {
        setLoading(true);
        api.get('/help-tickets')
            .then(({ data }) => setTickets(data))
            .catch(console.error)
            .finally(() => setLoading(false));
    };

    useEffect(() => { fetchTickets(); }, []);

    const handleResolver = async (id) => {
        if (!window.confirm('¿Desea cerrar y eliminar este ticket?')) return;
        try {
            await api.delete(`/help-tickets/${id}`);
            setTickets(prev => prev.filter(t => t.id !== id));
            setExpandedId(null);
        } catch (err) {
            console.error(err);
            alert('Error al eliminar el ticket.');
        }
    };

    const formatDate = (dateStr) => {
        const d = new Date(dateStr);
        return d.toLocaleString('es-AR', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    if (loading) return <LoadingSpinner />;

    if (tickets.length === 0) {
        return (
            <div className="empty-state">
                <div className="empty-state-icon">📭</div>
                <p>No hay tickets de ayuda pendientes.</p>
            </div>
        );
    }

    return (
        <div className="tickets-list">
            {tickets.map((ticket) => (
                <div
                    key={ticket.id}
                    className={`ticket-card ${expandedId === ticket.id ? 'ticket-expanded' : ''}`}
                >
                    <div
                        className="ticket-header"
                        onClick={() => setExpandedId(expandedId === ticket.id ? null : ticket.id)}
                    >
                        <div className="ticket-info">
                            <span className="ticket-email">{ticket.email}</span>
                            <span className="ticket-date">{formatDate(ticket.createdAt)}</span>
                        </div>
                        <div className="ticket-preview">
                            {expandedId === ticket.id ? '' : (
                                ticket.mensaje.length > 80
                                    ? ticket.mensaje.substring(0, 80) + '...'
                                    : ticket.mensaje
                            )}
                        </div>
                        <span className="ticket-toggle">
                            {expandedId === ticket.id ? '▲' : '▼'}
                        </span>
                    </div>

                    {expandedId === ticket.id && (
                        <div className="ticket-body">
                            <p className="ticket-mensaje">{ticket.mensaje}</p>
                            <div className="ticket-actions">
                                <button
                                    className="btn btn-danger btn-sm"
                                    onClick={() => handleResolver(ticket.id)}
                                >
                                    ✓ Cerrar ticket
                                </button>
                                <button
                                    className="btn btn-secondary btn-sm"
                                    onClick={() => setExpandedId(null)}
                                >
                                    ← Volver
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            ))}
        </div>
    );
}

export default function Reportes() {
    const [tab, setTab] = useState('metricas');

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">Reportes</h1>
                    <p className="page-subtitle">Panel de indicadores y centro de ayuda</p>
                </div>
            </div>

            <div className="tabs">
                <button
                    className={`tab-btn ${tab === 'metricas' ? 'tab-active' : ''}`}
                    onClick={() => setTab('metricas')}
                    id="tab-metricas"
                >
                    📊 Métricas
                </button>
                <button
                    className={`tab-btn ${tab === 'ayuda' ? 'tab-active' : ''}`}
                    onClick={() => setTab('ayuda')}
                    id="tab-ayuda"
                >
                    📢 Tickets de Ayuda
                </button>
            </div>

            {tab === 'metricas' ? <MetricasTab /> : <CentroAyudaTab />}
        </div>
    );
}
