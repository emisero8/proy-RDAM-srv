import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function LoginAdmin() {
    const [identificador, setIdentificador] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const { loginAdmin, loading, isAuthenticated, user } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (isAuthenticated && user?.portal === 'ADMIN') {
            navigate(user.rol === 'ADMIN' ? '/reportes' : '/bandeja', { replace: true });
        }
    }, [isAuthenticated, user, navigate]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            await loginAdmin(identificador, password);
            // La redirección ocurrirá por el useEffect cuando el contexto se actualice
        } catch (err) {
            const msg = err.response?.data?.error?.message || 'Credenciales inválidas';
            setError(msg);
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-logo">
                    <h1>RDAM</h1>
                    <p>Login Interno — Gestores y Administradores</p>
                </div>

                <form onSubmit={handleSubmit} id="login-admin-form">
                    {error && <div className="toast toast-error" style={{ marginBottom: '1rem' }}>{error}</div>}

                    <div className="form-group">
                        <label className="form-label" htmlFor="identificador-admin">Email o CUIL</label>
                        <input
                            id="identificador-admin"
                            type="text"
                            className="form-input"
                            placeholder="usuario@rdam.gob.ar"
                            value={identificador}
                            onChange={(e) => setIdentificador(e.target.value)}
                            required
                            autoFocus
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="password-admin">Contraseña</label>
                        <input
                            id="password-admin"
                            type="password"
                            className="form-input"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>

                    <button type="submit" className="btn btn-primary btn-lg" style={{ width: '100%' }} disabled={loading} id="btn-login-admin">
                        {loading ? 'Ingresando...' : 'Ingresar como Interno'}
                    </button>
                </form>

                <div className="auth-footer">
                    <Link to="/login" style={{ color: 'var(--color-text-dim)' }}>← Portal Ciudadano</Link>
                </div>
            </div>
        </div>
    );
}
