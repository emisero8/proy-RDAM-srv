import { useSearchParams, useNavigate } from 'react-router-dom';

export default function ResultadoPago() {
    const [params] = useSearchParams();
    const navigate = useNavigate();

    const status = params.get('status');   // 'success' | 'error'
    const txn = params.get('txn');      // TXN-SOL-2026-001-timestamp
    const solicitudId = params.get('sol');    // ID numérico de la solicitud

    const isSuccess = status === 'success';

    const handleVolver = () => {
        if (solicitudId) {
            navigate(`/solicitudes/${solicitudId}`);
        } else {
            navigate('/solicitudes/mis');
        }
    };

    return (
        <div style={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 'var(--space-xl)',
            background: 'var(--color-bg)',
        }}>
            <div className="card" style={{
                maxWidth: 480,
                width: '100%',
                textAlign: 'center',
                padding: 'var(--space-2xl)',
            }}>
                <div style={{ fontSize: '4rem', marginBottom: 'var(--space-lg)' }}>
                    {isSuccess ? '✅' : '❌'}
                </div>

                <h1 className="page-title" style={{
                    color: isSuccess ? 'var(--color-success)' : 'var(--color-error)',
                    marginBottom: 'var(--space-md)',
                }}>
                    {isSuccess ? '¡Pago realizado!' : 'Pago no procesado'}
                </h1>

                <p style={{ color: 'var(--color-text-muted)', marginBottom: 'var(--space-lg)' }}>
                    {isSuccess
                        ? 'Tu pago fue aprobado. Tu solicitud pasará al estado PAGADA en breve.'
                        : 'El pago fue rechazado o cancelado. Podés intentarlo nuevamente desde el detalle de tu solicitud.'}
                </p>

                {txn && (
                    <div style={{
                        background: 'var(--color-bg-elevated)',
                        borderRadius: 'var(--radius-md)',
                        padding: 'var(--space-md)',
                        marginBottom: 'var(--space-lg)',
                        fontFamily: 'monospace',
                        fontSize: 'var(--font-sm)',
                        color: 'var(--color-text-muted)',
                        wordBreak: 'break-all',
                    }}>
                        Referencia: {txn}
                    </div>
                )}

                <div style={{ display: 'flex', gap: 'var(--space-md)', justifyContent: 'center', flexWrap: 'wrap' }}>
                    <button className="btn btn-primary" onClick={handleVolver}>
                        ← Volver a la solicitud
                    </button>
                </div>
            </div>
        </div>
    );
}
