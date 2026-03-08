import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import SolicitudCard from '../../components/SolicitudCard';
import LoadingSpinner from '../../components/LoadingSpinner';

export default function MisSolicitudes() {
    const [solicitudes, setSolicitudes] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        api.get('/solicitudes/mis')
            .then(({ data }) => setSolicitudes(Array.isArray(data) ? data : data.content || []))
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    if (loading) return <LoadingSpinner />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">Mis Solicitudes</h1>
                    <p className="page-subtitle">Seguimiento de tus trámites de libre deuda</p>
                </div>
                <button className="btn btn-primary" onClick={() => navigate('/solicitudes/nueva')} id="btn-nueva-solicitud">
                    + Nueva Solicitud
                </button>
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
