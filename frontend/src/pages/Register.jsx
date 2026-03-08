import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Register() {
    const [form, setForm] = useState({
        nombre: '', apellido: '', email: '', password: '', dniCuil: '', telefono: ''
    });
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);
    const { register, loading } = useAuth();
    const navigate = useNavigate();

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            await register(form);
            setSuccess(true);
            setTimeout(() => navigate('/login'), 2000);
        } catch (err) {
            const msg = err.response?.data?.error?.message || 'Error en el registro';
            setError(msg);
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-logo">
                    <h1>RDAM</h1>
                    <p>Registro de nuevo ciudadano</p>
                </div>

                {success ? (
                    <div className="toast toast-success" style={{ textAlign: 'center' }}>
                        ✅ Registro exitoso. Redirigiendo al login...
                    </div>
                ) : (
                    <form onSubmit={handleSubmit} id="register-form">
                        {error && <div className="toast toast-error" style={{ marginBottom: '1rem' }}>{error}</div>}

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 1rem' }}>
                            <div className="form-group">
                                <label className="form-label" htmlFor="nombre">Nombre</label>
                                <input id="nombre" name="nombre" className="form-input" value={form.nombre} onChange={handleChange} required />
                            </div>
                            <div className="form-group">
                                <label className="form-label" htmlFor="apellido">Apellido</label>
                                <input id="apellido" name="apellido" className="form-input" value={form.apellido} onChange={handleChange} required />
                            </div>
                        </div>

                        <div className="form-group">
                            <label className="form-label" htmlFor="email">Email</label>
                            <input id="email" name="email" type="email" className="form-input" value={form.email} onChange={handleChange} required />
                        </div>

                        <div className="form-group">
                            <label className="form-label" htmlFor="reg-password">Contraseña</label>
                            <input id="reg-password" name="password" type="password" className="form-input" placeholder="Mínimo 8 caracteres" value={form.password} onChange={handleChange} required minLength={8} />
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 1rem' }}>
                            <div className="form-group">
                                <label className="form-label" htmlFor="dniCuil">DNI / CUIL</label>
                                <input id="dniCuil" name="dniCuil" className="form-input" placeholder="20-12345678-9" value={form.dniCuil} onChange={handleChange} />
                            </div>
                            <div className="form-group">
                                <label className="form-label" htmlFor="telefono">Teléfono</label>
                                <input id="telefono" name="telefono" className="form-input" placeholder="011-1234-5678" value={form.telefono} onChange={handleChange} />
                            </div>
                        </div>

                        <button type="submit" className="btn btn-primary btn-lg" style={{ width: '100%' }} disabled={loading} id="btn-register">
                            {loading ? 'Registrando...' : 'Crear Cuenta'}
                        </button>
                    </form>
                )}

                <div className="auth-footer">
                    <p>¿Ya tenés cuenta? <Link to="/login">Iniciar Sesión</Link></p>
                </div>
            </div>
        </div>
    );
}
