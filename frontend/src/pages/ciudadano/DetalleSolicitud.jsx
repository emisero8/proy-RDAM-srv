import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import { useAuth } from '../../hooks/useAuth';
import EstadoBadge from '../../components/EstadoBadge';
import LoadingSpinner from '../../components/LoadingSpinner';
import Modal from '../../components/Modal';
import { formatDate, formatCurrency, TIPOS_CERT, formatSolicitudNombre } from '../../utils/constants';

export default function DetalleSolicitud() {
    const { id } = useParams();
    const { user } = useAuth();
    const navigate = useNavigate();
    const [solicitud, setSolicitud] = useState(null);
    const [historial, setHistorial] = useState([]);
    const [certificado, setCertificado] = useState(null);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);
    const [pagoLoading, setPagoLoading] = useState(false);
    const [error, setError] = useState('');

    // Modal para rechazar
    const [showReject, setShowReject] = useState(false);
    const [motivoRechazo, setMotivoRechazo] = useState('');

    // Modal para emitir certificado (upload PDF)
    const [showEmitir, setShowEmitir] = useState(false);
    const [archivoPdf, setArchivoPdf] = useState(null);

    const fetchData = async () => {
        try {
            const [solRes, histRes] = await Promise.all([
                api.get(`/solicitudes/${id}`),
                api.get(`/solicitudes/${id}/historial`).catch(() => ({ data: [] })),
            ]);
            setSolicitud(solRes.data);
            setHistorial(Array.isArray(histRes.data) ? histRes.data : []);

            // Si está emitida, intentar obtener el certificado
            if (solRes.data.estado === 'EMITIDA') {
                try {
                    const certRes = await api.get(`/certificados/solicitud/${id}`);
                    setCertificado(certRes.data);
                } catch { /* no cert yet */ }
            }
        } catch (err) {
            setError('No se pudo cargar la solicitud');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchData(); }, [id]);

    const handleAction = async (action, body = {}) => {
        setActionLoading(true);
        setError('');
        try {
            if (action === 'tomar') {
                await api.patch(`/solicitudes/${id}/tomar`);
            } else if (action === 'aprobar') {
                await api.patch(`/solicitudes/${id}/aprobar`, { comentario: 'Documentación válida' });
            } else if (action === 'rechazar') {
                await api.patch(`/solicitudes/${id}/rechazar`, { motivoRechazo, comentario: 'Solicitud rechazada' });
                setShowReject(false);
            } else if (action === 'cancelar') {
                await api.patch(`/solicitudes/${id}/cancelar`);
            } else if (action === 'reasignar') {
                await api.patch(`/solicitudes/${id}/reasignar`);
            } else if (action === 'emitir') {
                if (!archivoPdf) {
                    setError('Debe seleccionar un archivo PDF');
                    setActionLoading(false);
                    return;
                }
                const formData = new FormData();
                formData.append('solicitudId', id);
                formData.append('archivo', archivoPdf);
                await api.post('/certificados/emitir', formData, {
                    headers: { 'Content-Type': 'multipart/form-data' },
                });
                setShowEmitir(false);
                setArchivoPdf(null);
            }
            fetchData();
        } catch (err) {
            setError(err.response?.data?.error?.message || 'Error al ejecutar la acción');
        } finally {
            setActionLoading(false);
        }
    };

    /** Inicia el pago real con PlusPagos: llama al backend y redirige via formulario POST */
    const handlePagar = async () => {
        setPagoLoading(true);
        setError('');
        try {
            const res = await api.post('/pagos/iniciar', { solicitudId: parseInt(id) });
            const d = res.data;

            // Construir formulario POST automático hacia PlusPagos (igual que test-pasarela-simple)
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = d.plusPagosUrl;
            form.style.display = 'none';

            const fields = {
                Comercio: d.comercio,
                TransaccionComercioId: d.transaccionComercioId,
                Monto: d.montoEnc,
                CallbackSuccess: d.callbackSuccessEnc,
                CallbackCancel: d.callbackCancelEnc,
                UrlSuccess: d.urlSuccessEnc,
                UrlError: d.urlErrorEnc,
                Informacion: d.informacionEnc,
            };

            Object.entries(fields).forEach(([name, value]) => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = name;
                input.value = value;
                form.appendChild(input);
            });

            document.body.appendChild(form);
            form.submit();
            // El navegador redirigirá — no hay vuelta acá
        } catch (err) {
            setError(err.response?.data?.error?.message || 'Error al iniciar el pago');
            setPagoLoading(false);
        }
    };

    const handleDescargar = async () => {
        if (!certificado) return;
        try {
            const response = await api.get(`/certificados/${certificado.id}/descargar`, {
                responseType: 'blob',
            });
            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `CERT-${solicitud.numero}.pdf`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch {
            setError('Error al descargar el certificado');
        }
    };

    if (loading) return <LoadingSpinner />;
    if (!solicitud) return <div className="empty-state"><p>Solicitud no encontrada</p></div>;

    const isGestor = user?.rol === 'GESTOR' || user?.rol === 'ADMIN';
    const isCiudadano = user?.rol === 'CIUDADANO';
    const isAdmin = user?.rol === 'ADMIN';

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">{formatSolicitudNombre(solicitud)}</h1>
                    {solicitud.numero && (
                        <span style={{ fontSize: 'var(--font-sm)', color: 'var(--color-text-muted)' }}>
                            Ref: {solicitud.numero}
                        </span>
                    )}
                    <EstadoBadge estado={solicitud.estado} />
                </div>
                <button className="btn btn-secondary" onClick={() => isCiudadano ? navigate('/solicitudes/mis') : navigate(-1)}>← Volver</button>
            </div>

            {error && <div className="toast toast-error" style={{ marginBottom: '1rem' }}>{error}</div>}

            {/* === Detail Card === */}
            <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
                <div className="detail-grid">
                    <div className="detail-field">
                        <span className="detail-label">Tipo</span>
                        <span className="detail-value">{TIPOS_CERT.find(t => t.value === solicitud.tipoCert)?.label || solicitud.tipoCert}</span>
                    </div>
                    <div className="detail-field">
                        <span className="detail-label">Urgencia</span>
                        <span className="detail-value">{solicitud.urgencia}</span>
                    </div>
                    <div className="detail-field">
                        <span className="detail-label">Arancel</span>
                        <span className="detail-value">{formatCurrency(solicitud.arancel)}</span>
                    </div>
                    <div className="detail-field">
                        <span className="detail-label">Fecha Creación</span>
                        <span className="detail-value">{formatDate(solicitud.createdAt)}</span>
                    </div>
                    {solicitud.motivoRechazo && (
                        <div className="detail-field" style={{ gridColumn: '1 / -1' }}>
                            <span className="detail-label">Motivo de Rechazo</span>
                            <span className="detail-value" style={{ color: 'var(--color-error)' }}>{solicitud.motivoRechazo}</span>
                        </div>
                    )}
                    {solicitud.observaciones && (
                        <div className="detail-field" style={{ gridColumn: '1 / -1' }}>
                            <span className="detail-label">Observaciones</span>
                            <span className="detail-value">{solicitud.observaciones}</span>
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

            {/* === Actions === */}
            <div className="actions-bar" style={{ marginBottom: 'var(--space-lg)' }}>
                {isGestor && solicitud.estado === 'PENDIENTE_REVISION' && (
                    <button className="btn btn-primary" onClick={() => handleAction('tomar')} disabled={actionLoading} id="btn-tomar">
                        📋 Tomar Solicitud
                    </button>
                )}
                {isGestor && solicitud.estado === 'EN_REVISION' && (
                    <>
                        <button className="btn btn-success" onClick={() => handleAction('aprobar')} disabled={actionLoading} id="btn-aprobar">
                            ✅ Aprobar
                        </button>
                        <button className="btn btn-danger" onClick={() => setShowReject(true)} disabled={actionLoading} id="btn-rechazar">
                            ❌ Rechazar
                        </button>
                    </>
                )}
                {isCiudadano && solicitud.estado === 'PENDIENTE_PAGO' && (
                    <button
                        className="btn btn-primary"
                        onClick={handlePagar}
                        disabled={pagoLoading}
                        id="btn-pagar"
                    >
                        {pagoLoading ? '⏳ Conectando con pasarela...' : '💳 Pagar con PlusPagos'}
                    </button>
                )}
                {isGestor && solicitud.estado === 'PAGADA' && (
                    <button className="btn btn-success" onClick={() => setShowEmitir(true)} disabled={actionLoading} id="btn-emitir">
                        📜 Emitir Certificado
                    </button>
                )}
                {/* Descargar certificado — visible para todos cuando EMITIDA */}
                {solicitud.estado === 'EMITIDA' && certificado && (
                    <button className="btn btn-primary" onClick={handleDescargar} id="btn-descargar">
                        📥 Descargar Certificado PDF
                    </button>
                )}
                {/* Cancelar — ciudadano en cualquier estado no-terminal */}
                {isCiudadano && !['CANCELADA', 'RECHAZADA', 'EMITIDA', 'EXPIRADA'].includes(solicitud.estado) && (
                    <button className="btn btn-danger" onClick={() => handleAction('cancelar')} disabled={actionLoading} id="btn-cancelar">
                        🚫 Cancelar Solicitud
                    </button>
                )}
                {/* Reasignar — sólo Admin cuando está EN_REVISION */}
                {isAdmin && solicitud.estado === 'EN_REVISION' && solicitud.revisorId !== user?.id && (
                    <button className="btn btn-secondary" onClick={() => handleAction('reasignar')} disabled={actionLoading} id="btn-reasignar">
                        🔄 Tomar gestión
                    </button>
                )}
            </div>

            {/* === Historial === */}
            {historial.length > 0 && (
                <div className="card">
                    <h3 className="card-title" style={{ marginBottom: 'var(--space-lg)' }}>Historial de Estados</h3>
                    <div className="timeline">
                        {historial.map((item, i) => (
                            <div key={i} className="timeline-item">
                                <div className="timeline-label">
                                    <EstadoBadge estado={item.estadoNuevo} />
                                </div>
                                <div className="timeline-date">{formatDate(item.createdAt)}</div>
                                {item.comentario && <p style={{ fontSize: 'var(--font-sm)', color: 'var(--color-text-muted)' }}>{item.comentario}</p>}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* === Reject Modal === */}
            <Modal
                isOpen={showReject}
                onClose={() => setShowReject(false)}
                title="Rechazar Solicitud"
                footer={
                    <>
                        <button className="btn btn-secondary" onClick={() => setShowReject(false)}>Cancelar</button>
                        <button
                            className="btn btn-danger"
                            onClick={() => handleAction('rechazar')}
                            disabled={!motivoRechazo.trim() || actionLoading}
                            id="btn-confirmar-rechazo"
                        >
                            Confirmar Rechazo
                        </button>
                    </>
                }
            >
                <div className="form-group">
                    <label className="form-label">Motivo del rechazo *</label>
                    <textarea
                        className="form-textarea"
                        value={motivoRechazo}
                        onChange={(e) => setMotivoRechazo(e.target.value)}
                        placeholder="Indicá el motivo del rechazo..."
                        required
                    />
                </div>
            </Modal>

            {/* === Emitir Certificado Modal (Upload PDF) === */}
            <Modal
                isOpen={showEmitir}
                onClose={() => { setShowEmitir(false); setArchivoPdf(null); }}
                title="Emitir Certificado"
                footer={
                    <>
                        <button className="btn btn-secondary" onClick={() => { setShowEmitir(false); setArchivoPdf(null); }}>Cancelar</button>
                        <button
                            className="btn btn-success"
                            onClick={() => handleAction('emitir')}
                            disabled={!archivoPdf || actionLoading}
                            id="btn-confirmar-emitir"
                        >
                            {actionLoading ? 'Subiendo...' : 'Emitir Certificado'}
                        </button>
                    </>
                }
            >
                <div className="form-group">
                    <label className="form-label">Archivo PDF del certificado *</label>
                    <input
                        type="file"
                        accept="application/pdf"
                        className="form-input"
                        onChange={(e) => setArchivoPdf(e.target.files[0])}
                        id="input-archivo-pdf"
                    />
                    {archivoPdf && (
                        <p style={{ fontSize: 'var(--font-sm)', color: 'var(--color-text-muted)', marginTop: 'var(--space-xs)' }}>
                            📎 {archivoPdf.name} ({(archivoPdf.size / 1024).toFixed(1)} KB)
                        </p>
                    )}
                </div>
            </Modal>
        </div>
    );
}
