import { useState } from 'react';
import api from '../api/axios';
import atencionIcon from '../assets/atencion.png';

export default function HelpWidget() {
    const [open, setOpen] = useState(false);
    const [mensaje, setMensaje] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [enviado, setEnviado] = useState(false);

    const maxChars = 256;
    const remaining = maxChars - mensaje.length;

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!mensaje.trim() || enviando) return;
        setEnviando(true);
        try {
            await api.post('/help-tickets', { mensaje: mensaje.trim() });
            setEnviado(true);
            setMensaje('');
            setTimeout(() => {
                setEnviado(false);
                setOpen(false);
            }, 2500);
        } catch (err) {
            console.error(err);
            alert('Error al enviar el ticket. Intente nuevamente.');
        } finally {
            setEnviando(false);
        }
    };

    return (
        <>
            {/* Botón flotante */}
            <button
                className="help-fab"
                onClick={() => { setOpen(!open); setEnviado(false); }}
                title="Centro de ayuda"
                id="help-fab-btn"
            >
                <img src={atencionIcon} alt="Ayuda" className="help-fab-icon" />
                <span className="help-fab-label">Centro de ayuda</span>
            </button>

            {/* Panel lateral */}
            {open && (
                <div className="help-panel-overlay" onClick={() => setOpen(false)}>
                    <div className="help-panel" onClick={(e) => e.stopPropagation()}>
                        <div className="help-panel-header">
                            <h3>Centro de Ayuda</h3>
                            <button className="modal-close" onClick={() => setOpen(false)}>✕</button>
                        </div>

                        <p className="help-panel-desc">
                            ¿Necesita ayuda? Ingrese aquí su problema e intentaremos resolverlo lo más pronto posible.
                        </p>

                        {enviado ? (
                            <div className="help-success">
                                <div className="help-success-icon">✅</div>
                                <p>¡Su consulta fue enviada con éxito!</p>
                                <p style={{ fontSize: 'var(--font-xs)', color: 'var(--color-text-dim)' }}>
                                    Nos pondremos en contacto a la brevedad.
                                </p>
                            </div>
                        ) : (
                            <form onSubmit={handleSubmit}>
                                <div className="help-textarea-wrapper">
                                    <textarea
                                        className="form-textarea help-textarea"
                                        placeholder="Describa su problema..."
                                        value={mensaje}
                                        onChange={(e) => {
                                            if (e.target.value.length <= maxChars) {
                                                setMensaje(e.target.value);
                                            }
                                        }}
                                        maxLength={maxChars}
                                        rows={5}
                                        id="help-mensaje"
                                    />
                                    <span className={`help-char-counter ${remaining <= 30 ? 'help-char-warn' : ''}`}>
                                        {remaining}/{maxChars} caracteres restantes
                                    </span>
                                </div>
                                <button
                                    type="submit"
                                    className="btn btn-primary btn-lg"
                                    disabled={!mensaje.trim() || enviando}
                                    style={{ width: '100%', marginTop: 'var(--space-md)' }}
                                >
                                    {enviando ? 'Enviando...' : 'Enviar consulta'}
                                </button>
                            </form>
                        )}
                    </div>
                </div>
            )}
        </>
    );
}
