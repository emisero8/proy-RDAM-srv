import { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function LoginCiudadano() {
    const [step, setStep] = useState(1);
    const [email, setEmail] = useState('');
    const [codigo, setCodigo] = useState('');
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const { solicitarCodigo, validarCodigo, loading, isAuthenticated, user } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    // Mostrar aviso si volvemos del rechazo de menores
    useEffect(() => {
        if (location.state?.menorRechazado) {
            setError('Acceso denegado: debés ser mayor de 18 años. Tu cuenta fue eliminada del sistema.');
        }
    }, [location.state]);

    // Redirigir automáticamente si ya está autenticado
    useEffect(() => {
        if (isAuthenticated && user?.portal === 'CIUDADANO') {
            if (user?.perfilCompleto === false) {
                navigate('/completar-perfil', { replace: true });
            } else {
                navigate('/solicitudes/mis', { replace: true });
            }
        }
    }, [isAuthenticated, user, navigate]);

    // ─── Paso 1: Solicitar código ───────────────────────────────────────
    const handleSolicitarCodigo = async (e) => {
        e.preventDefault();
        setError('');
        setMessage('');
        try {
            const data = await solicitarCodigo(email);
            setMessage(data.message || 'Código enviado. Revisá tu email.');
            setStep(2);
        } catch (err) {
            const msg = err.response?.data?.error?.message || 'Error al solicitar el código';
            setError(msg);
        }
    };

    // ─── Paso 2: Validar código ─────────────────────────────────────────
    const handleValidarCodigo = async (e) => {
        e.preventDefault();
        setError('');
        try {
            const userData = await validarCodigo(email, codigo);
            // Redirigir según si el perfil ya fue completado
            if (userData?.perfilCompleto === false) {
                navigate('/completar-perfil', { replace: true });
            } else {
                navigate('/solicitudes/mis', { replace: true });
            }
        } catch (err) {
            const msg = err.response?.data?.error?.message || 'Código inválido o expirado';
            setError(msg);
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-logo">
                    <h1>RDAM</h1>
                    <p>ID Ciudadano — Portal de Servicios</p>
                </div>

                {step === 1 ? (
                    /* ─── Paso 1: Email ─────────────────────────────────── */
                    <form onSubmit={handleSolicitarCodigo} id="solicitar-codigo-form">
                        {error && <div className="toast toast-error" style={{ marginBottom: '1rem' }}>{error}</div>}

                        <div className="form-group">
                            <label className="form-label" htmlFor="email">Email</label>
                            <input
                                id="email"
                                type="email"
                                className="form-input"
                                placeholder="tucorreo@email.com"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                autoFocus
                            />
                        </div>

                        <p style={{ fontSize: '0.85rem', color: 'var(--color-text-dim)', marginBottom: '1rem' }}>
                            Te enviaremos un código de 6 dígitos a tu email para verificar tu identidad. No necesitás contraseña.
                        </p>

                        <button type="submit" className="btn btn-primary btn-lg" style={{ width: '100%' }} disabled={loading} id="btn-solicitar-codigo">
                            {loading ? 'Enviando...' : 'Enviar código'}
                        </button>
                    </form>
                ) : (
                    /* ─── Paso 2: Código ────────────────────────────────── */
                    <form onSubmit={handleValidarCodigo} id="validar-codigo-form">
                        {error && <div className="toast toast-error" style={{ marginBottom: '1rem' }}>{error}</div>}
                        {message && <div className="toast toast-success" style={{ marginBottom: '1rem' }}>{message}</div>}

                        <p style={{ fontSize: '0.9rem', marginBottom: '1rem' }}>
                            Ingresá el código de 6 dígitos enviado a <strong>{email}</strong>
                        </p>

                        <div className="form-group">
                            <label className="form-label" htmlFor="codigo">Código de verificación</label>
                            <input
                                id="codigo"
                                type="text"
                                className="form-input"
                                placeholder="123456"
                                value={codigo}
                                onChange={(e) => {
                                    // Solo permitir dígitos, máx 6
                                    const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                                    setCodigo(val);
                                }}
                                maxLength={6}
                                required
                                autoFocus
                                style={{ textAlign: 'center', fontSize: '1.5rem', letterSpacing: '0.5em' }}
                            />
                        </div>

                        <button type="submit" className="btn btn-primary btn-lg" style={{ width: '100%' }} disabled={loading || codigo.length !== 6} id="btn-validar-codigo">
                            {loading ? 'Verificando...' : 'Verificar código'}
                        </button>

                        <button type="button"
                            className="btn btn-secondary"
                            style={{ width: '100%', marginTop: '0.75rem' }}
                            onClick={() => { setStep(1); setCodigo(''); setError(''); setMessage(''); }}
                        >
                            ← Volver a ingresar email
                        </button>
                    </form>
                )}

                <div className="auth-footer">
                    <p style={{ marginTop: '0.5rem' }}>
                        <Link to="/admin/login" style={{ color: 'var(--color-text-dim)' }}>Portal interno →</Link>
                    </p>
                </div>
            </div>
        </div>
    );
}
