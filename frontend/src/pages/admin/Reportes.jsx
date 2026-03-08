import { useState, useEffect } from 'react';
import api from '../../api/axios';
import LoadingSpinner from '../../components/LoadingSpinner';
import { formatCurrency } from '../../utils/constants';

export default function Reportes() {
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
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">Reportes & Métricas</h1>
                    <p className="page-subtitle">Panel de indicadores del sistema RDAM</p>
                </div>
            </div>

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
        </div>
    );
}
