import EstadoBadge from './EstadoBadge';
import { formatCurrency, formatSolicitudNombre } from '../utils/constants';
import { useAuth } from '../hooks/useAuth';

function SlaBadge({ solicitud }) {
    if (solicitud.estado !== 'PAGADA') return null;

    const updated = new Date(solicitud.updatedAt);
    const now = new Date();
    const diffMs = Math.max(0, now - updated);
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));

    let level, label;
    if (diffDays < 3) {
        level = 'green';
        label = diffDays === 0 ? `${diffHours}h` : `${diffDays}d`;
    } else if (diffDays < 7) {
        level = 'yellow';
        label = `${diffDays}d`;
    } else {
        level = 'red';
        label = `${diffDays}d`;
    }

    return (
        <span className={`sla-badge sla-${level}`} title={`Hace ${diffDays} día(s) en estado Pagada`}>
            ⏱ {label}
        </span>
    );
}

export default function SolicitudCard({ solicitud, onClick }) {
    const { user } = useAuth();
    const isGestor = user?.rol === 'GESTOR' || user?.rol === 'ADMIN';

    return (
        <div className="card" onClick={onClick} style={{ cursor: onClick ? 'pointer' : 'default', padding: 'var(--space-md)' }}>
            <div className="card-header" style={{ marginBottom: 'var(--space-md)' }}>
                <div>
                    <span className="card-title" style={{ fontSize: 'var(--font-lg)' }}>{formatSolicitudNombre(solicitud)}</span>
                    {solicitud.numero && (
                        <span style={{ fontSize: 'var(--font-xs)', color: 'var(--color-text-muted)', display: 'block', marginTop: '2px' }}>
                            Ref: {solicitud.numero}
                        </span>
                    )}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {isGestor && <SlaBadge solicitud={solicitud} />}
                    <EstadoBadge estado={solicitud.estado} />
                </div>
            </div>
            <div className="detail-grid" style={{ gap: 'var(--space-md)' }}>
                <div className="detail-field">
                    <span className="detail-label" style={{ fontSize: '0.65rem' }}>Tipo</span>
                    <span className="detail-value" style={{ fontSize: 'var(--font-sm)' }}>{solicitud.tipoCert?.replace('_', ' ')}</span>
                </div>
                <div className="detail-field">
                    <span className="detail-label" style={{ fontSize: '0.65rem' }}>Arancel</span>
                    <span className="detail-value" style={{ fontSize: 'var(--font-sm)' }}>{formatCurrency(solicitud.arancel)}</span>
                </div>
                {solicitud.ciudadanoNombre && (
                    <div className="detail-field">
                        <span className="detail-label" style={{ fontSize: '0.65rem' }}>Ciudadano</span>
                        <span className="detail-value" style={{ fontSize: 'var(--font-sm)' }}>{solicitud.ciudadanoNombre}</span>
                    </div>
                )}
                {isGestor && (
                    <div className="detail-field">
                        <span className="detail-label" style={{ fontSize: '0.65rem' }}>Gestor asignado</span>
                        <span className="detail-value" style={{ fontSize: 'var(--font-sm)' }}>
                            {solicitud.revisorNombre ? `👤 ${solicitud.revisorNombre}` : 'Sin asignar'}
                        </span>
                    </div>
                )}
            </div>
        </div>
    );
}
