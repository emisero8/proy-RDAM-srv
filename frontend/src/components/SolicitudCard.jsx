import EstadoBadge from './EstadoBadge';
import { formatCurrency, formatSolicitudNombre } from '../utils/constants';

export default function SolicitudCard({ solicitud, onClick }) {
    return (
        <div className="card" onClick={onClick} style={{ cursor: onClick ? 'pointer' : 'default' }}>
            <div className="card-header">
                <div>
                    <span className="card-title">{formatSolicitudNombre(solicitud)}</span>
                    {solicitud.numero && (
                        <span style={{ fontSize: 'var(--font-sm)', color: 'var(--color-text-muted)', display: 'block', marginTop: '2px' }}>
                            Ref: {solicitud.numero}
                        </span>
                    )}
                </div>
                <EstadoBadge estado={solicitud.estado} />
            </div>
            <div className="detail-grid">
                <div className="detail-field">
                    <span className="detail-label">Tipo</span>
                    <span className="detail-value">{solicitud.tipoCert?.replace('_', ' ')}</span>
                </div>
                <div className="detail-field">
                    <span className="detail-label">Arancel</span>
                    <span className="detail-value">{formatCurrency(solicitud.arancel)}</span>
                </div>
                {solicitud.ciudadanoNombre && (
                    <div className="detail-field">
                        <span className="detail-label">Ciudadano</span>
                        <span className="detail-value">{solicitud.ciudadanoNombre}</span>
                    </div>
                )}
                {solicitud.revisorNombre && (
                    <div className="detail-field">
                        <span className="detail-label">Gestor asignado</span>
                        <span className="detail-value">👤 {solicitud.revisorNombre}</span>
                    </div>
                )}
            </div>
        </div>
    );
}
