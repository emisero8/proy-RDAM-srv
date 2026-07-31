import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import SolicitudCard from '../../components/SolicitudCard';
import LoadingSpinner from '../../components/LoadingSpinner';

export default function MisSolicitudes() {
    const [solicitudes, setSolicitudes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [sortOrder, setSortOrder] = useState('desc'); // 'desc' = más nuevas, 'asc' = más antiguas
    const navigate = useNavigate();

    useEffect(() => {
        api.get('/solicitudes/mis')
            .then(({ data }) => setSolicitudes(Array.isArray(data) ? data : data.content || []))
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);


    const solicitudesOrdenadas = [...solicitudes].sort((a, b) => {
        const dateA = new Date(a.createdAt).getTime();
        const dateB = new Date(b.createdAt).getTime();
        return sortOrder === 'desc' ? dateB - dateA : dateA - dateB;
    });

    if (loading) return <LoadingSpinner />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">Mis Solicitudes</h1>
                    <p className="page-subtitle">Seguimiento de tus trámites de libre deuda</p>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <select 
                        className="btn" 
                        value={sortOrder}
                        onChange={(e) => setSortOrder(e.target.value)}
                        style={{
                            backgroundColor: 'var(--color-bg-card)',
                            border: '1px solid var(--color-primary-500)',
                            color: 'white',
                            width: '160px',
                            cursor: 'pointer'
                        }}
                    >
                        <option value="desc">Más nuevas</option>
                        <option value="asc">Más antiguas</option>
                    </select>
                    <button className="btn btn-primary" onClick={() => navigate('/solicitudes/nueva')} id="btn-nueva-solicitud">
                        + Nueva Solicitud
                    </button>
                </div>
            </div>

            {solicitudes.length === 0 ? (
                <div className="empty-state">
                    <div className="empty-state-icon">📋</div>
                    <p>No tenés solicitudes todavía.</p>
                    <button className="btn btn-primary" style={{ marginTop: '1rem' }} onClick={() => navigate('/solicitudes/nueva')}>
                        Crear tu primera solicitud
                    </button>
                </div>
            ) : (
                <div style={{ display: 'grid', gap: 'var(--space-lg)', maxWidth: '90%', margin: '0 auto' }}>
                    {solicitudesOrdenadas.map((sol) => (
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
