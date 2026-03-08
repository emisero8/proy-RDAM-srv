import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import { URGENCIAS } from '../../utils/constants';

export default function NuevaSolicitud() {
    const [form, setForm] = useState({ tipoCert: 'LIBRE_DEUDA', urgencia: 'NORMAL', observaciones: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        try {
            await api.post('/solicitudes', form);
            navigate('/solicitudes/mis');
        } catch (err) {
            setError(err.response?.data?.error?.message || 'Error al crear la solicitud');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">Nueva Solicitud</h1>
                    <p className="page-subtitle">Solicitud de Certificado Libre Deuda Alimenticio</p>
                </div>
            </div>

            <div className="card" style={{ maxWidth: '600px' }}>
                <form onSubmit={handleSubmit} id="nueva-solicitud-form">
                    {error && <div className="toast toast-error" style={{ marginBottom: '1rem' }}>{error}</div>}

                    <div className="form-group">
                        <label className="form-label">Tipo de Certificado</label>
                        <div className="form-input" style={{ background: 'var(--color-bg)', cursor: 'default', color: 'var(--color-text)' }}>
                            Certificado Libre Deuda Alimenticio
                        </div>
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="urgencia">Urgencia</label>
                        <select
                            id="urgencia"
                            className="form-select"
                            value={form.urgencia}
                            onChange={(e) => setForm({ ...form, urgencia: e.target.value })}
                            disabled
                        >
                            {URGENCIAS.map((u) => (
                                <option key={u.value} value={u.value}>{u.label}</option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="observaciones">Observaciones</label>
                        <textarea
                            id="observaciones"
                            className="form-textarea"
                            value={form.observaciones}
                            onChange={(e) => setForm({ ...form, observaciones: e.target.value })}
                            placeholder="Indicá para qué necesitás el certificado..."
                        />
                    </div>

                    <div style={{ display: 'flex', gap: 'var(--space-sm)' }}>
                        <button type="submit" className="btn btn-primary" disabled={loading} id="btn-crear-solicitud">
                            {loading ? 'Enviando...' : 'Enviar Solicitud'}
                        </button>
                        <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>
                            Cancelar
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
