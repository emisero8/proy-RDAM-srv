import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function CompletarPerfil() {
    const { user, completarPerfil, logout, loading, isAuthenticated } = useAuth();
    const navigate = useNavigate();

    const [form, setForm] = useState({
        nombre: '',
        apellido: '',
        dniCuil: '',
        fechaNacimiento: '',
    });
    const [error, setError] = useState('');
    const [ageError, setAgeError] = useState('');

    // Si el usuario ya tiene el perfil completo, redirigir
    useEffect(() => {
        if (!isAuthenticated) {
            navigate('/login', { replace: true });
        } else if (user?.perfilCompleto === true) {
            navigate('/solicitudes/mis', { replace: true });
        }
    }, [isAuthenticated, user, navigate]);

    const calcularEdad = (fechaNac) => {
        if (!fechaNac) return null;
        const hoy = new Date();
        const nac = new Date(fechaNac);
        let edad = hoy.getFullYear() - nac.getFullYear();
        const m = hoy.getMonth() - nac.getMonth();
        if (m < 0 || (m === 0 && hoy.getDate() < nac.getDate())) {
            edad--;
        }
        return edad;
    };

    const handleFechaChange = (e) => {
        const val = e.target.value;
        setForm(f => ({ ...f, fechaNacimiento: val }));
        if (val) {
            const edad = calcularEdad(val);
            if (edad !== null && edad < 18) {
                setAgeError('Debés ser mayor de 18 años para utilizar este servicio.');
            } else {
                setAgeError('');
            }
        } else {
            setAgeError('');
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm(f => ({ ...f, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        // Validación client-side
        const edad = calcularEdad(form.fechaNacimiento);
        if (edad === null || edad < 18) {
            setAgeError('Debés ser mayor de 18 años para utilizar este servicio.');
            return;
        }

        try {
            await completarPerfil({
                nombre: form.nombre,
                apellido: form.apellido,
                dniCuil: form.dniCuil,
                fechaNacimiento: form.fechaNacimiento,
            });
            navigate('/solicitudes/mis', { replace: true });
        } catch (err) {
            const code = err.response?.data?.error?.code;
            const msg = err.response?.data?.error?.message;

            if (code === 'MENOR_DE_EDAD') {
                // La cuenta fue eliminada por el backend — limpiar sesión
                await logout();
                navigate('/login', { replace: true, state: { menorRechazado: true } });
            } else {
                setError(msg || 'Error al guardar los datos. Verificá que el DNI no esté ya registrado.');
            }
        }
    };

    const isFormValid = form.nombre.trim() &&
        form.apellido.trim() &&
        form.dniCuil.trim() &&
        form.fechaNacimiento &&
        !ageError;

    return (
        <div className="auth-container">
            <div className="auth-card" style={{ maxWidth: '500px' }}>
                <div className="auth-logo">
                    <h1>RDAM</h1>
                    <p>Completá tu perfil para continuar</p>
                </div>

                <div style={{
                    background: 'rgba(201, 162, 86, 0.08)',
                    border: '1px solid rgba(201, 162, 86, 0.25)',
                    borderRadius: 'var(--radius-md)',
                    padding: '0.875rem 1rem',
                    marginBottom: '1.5rem',
                    fontSize: '0.875rem',
                    color: 'var(--color-text-muted)',
                    display: 'flex',
                    gap: '0.625rem',
                    alignItems: 'flex-start',
                }}>
                    <span style={{ fontSize: '1rem', flexShrink: 0 }}>ℹ️</span>
                    <span>
                        Es la primera vez que ingresás. Completá tus datos para usar el portal.
                        Esta información es necesaria para gestionar tus trámites.
                    </span>
                </div>

                {error && (
                    <div className="toast toast-error" style={{ marginBottom: '1rem' }}>{error}</div>
                )}

                <form onSubmit={handleSubmit} id="completar-perfil-form">
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 1rem' }}>
                        <div className="form-group">
                            <label className="form-label" htmlFor="nombre">Nombre *</label>
                            <input
                                id="nombre"
                                name="nombre"
                                type="text"
                                className="form-input"
                                placeholder="Juan"
                                value={form.nombre}
                                onChange={handleChange}
                                required
                                autoFocus
                                autoComplete="given-name"
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label" htmlFor="apellido">Apellido *</label>
                            <input
                                id="apellido"
                                name="apellido"
                                type="text"
                                className="form-input"
                                placeholder="Pérez"
                                value={form.apellido}
                                onChange={handleChange}
                                required
                                autoComplete="family-name"
                            />
                        </div>
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="dniCuil">DNI / CUIL *</label>
                        <input
                            id="dniCuil"
                            name="dniCuil"
                            type="text"
                            className="form-input"
                            placeholder="20-12345678-9 o 12345678"
                            value={form.dniCuil}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="fechaNacimiento">Fecha de nacimiento *</label>
                        <input
                            id="fechaNacimiento"
                            name="fechaNacimiento"
                            type="date"
                            className="form-input"
                            value={form.fechaNacimiento}
                            onChange={handleFechaChange}
                            max={new Date(new Date().setFullYear(new Date().getFullYear() - 18))
                                .toISOString().split('T')[0]}
                            required
                        />
                        {ageError && (
                            <span className="form-error" style={{ marginTop: '0.25rem' }}>
                                ⚠️ {ageError}
                            </span>
                        )}
                    </div>

                    <button
                        type="submit"
                        className="btn btn-primary btn-lg"
                        style={{ width: '100%', marginTop: '0.5rem' }}
                        disabled={loading || !isFormValid}
                        id="btn-completar-perfil"
                    >
                        {loading ? 'Guardando...' : 'Completar perfil y continuar →'}
                    </button>
                </form>

                <p style={{
                    marginTop: '1.25rem',
                    fontSize: '0.8rem',
                    color: 'var(--color-text-dim)',
                    textAlign: 'center',
                    lineHeight: 1.5,
                }}>
                    Tus datos son utilizados exclusivamente para la gestión de trámites municipales.<br />
                    Al continuar aceptás los términos de uso del sistema RDAM.
                </p>
            </div>
        </div>
    );
}
