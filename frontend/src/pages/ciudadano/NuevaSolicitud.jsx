import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import { URGENCIAS } from '../../utils/constants';

export default function NuevaSolicitud() {
    const [form, setForm] = useState({
        tipoCert: 'LIBRE_DEUDA',
        urgencia: 'NORMAL',
        observaciones: '',
        nombre: '',
        apellido: '',
        dni: '',
        fechaNacimiento: ''
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    // ─── Filtros de entrada en tiempo real ─────────────────────────────────
    const soloLetras = (valor) => valor.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]/g, '');
    const soloDigitos = (valor) => valor.replace(/\D/g, '');

    // Fecha máxima: hoy | Fecha mínima: año 1900
    const hoy = new Date().toISOString().split('T')[0];

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        // Validar nombre y apellido
        if (!form.nombre.trim() || !form.apellido.trim()) {
            setError('Nombre y Apellido son obligatorios.');
            setLoading(false);
            return;
        }

        // Validar DNI: 7-8 dígitos
        if (form.dni.length < 7 || form.dni.length > 8) {
            setError('El DNI debe tener entre 7 y 8 dígitos.');
            setLoading(false);
            return;
        }

        // Validar fecha de nacimiento
        const fn = new Date(form.fechaNacimiento);
        if (fn.getFullYear() < 1900 || fn.getFullYear() > new Date().getFullYear()) {
            setError('El año de nacimiento debe ser un año válido de 4 dígitos.');
            setLoading(false);
            return;
        }

        const hoyDate = new Date();
        let edad = hoyDate.getFullYear() - fn.getFullYear();
        const m = hoyDate.getMonth() - fn.getMonth();
        if (m < 0 || (m === 0 && hoyDate.getDate() < fn.getDate())) {
            edad--;
        }

        if (edad < 18) {
            setError('Debe ser mayor de 18 años para realizar esta solicitud.');
            setLoading(false);
            return;
        }

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

            <div className="card" style={{ maxWidth: '900px' }}>
                <form onSubmit={handleSubmit} id="nueva-solicitud-form">

                    {error && (
                        <div className="toast toast-error" style={{ marginBottom: '1rem' }}>{error}</div>
                    )}

                    {/*
                     * Grid responsivo: 2 columnas en pantallas anchas (>= 640px),
                     * 1 columna en pantallas angostas. Las clases se definen en index.css.
                     */}
                    <div className="solicitud-form-grid">

                        {/* Tipo de certificado — full width */}
                        <div className="form-group solicitud-full">
                            <label className="form-label">Tipo de Certificado</label>
                            <div
                                className="form-input"
                                style={{ background: 'var(--color-bg)', cursor: 'default', color: 'var(--color-text)' }}
                            >
                                Certificado Libre Deuda Alimenticio
                            </div>
                        </div>

                        {/* Nombre | Apellido */}
                        <div className="form-group">
                            <label className="form-label" htmlFor="nombre">
                                Nombre <span style={{ color: 'red' }}>*</span>
                            </label>
                            <input
                                id="nombre"
                                type="text"
                                className="form-input"
                                value={form.nombre}
                                onChange={(e) => setForm({ ...form, nombre: soloLetras(e.target.value) })}
                                placeholder="Solo letras"
                                maxLength={100}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label" htmlFor="apellido">
                                Apellido <span style={{ color: 'red' }}>*</span>
                            </label>
                            <input
                                id="apellido"
                                type="text"
                                className="form-input"
                                value={form.apellido}
                                onChange={(e) => setForm({ ...form, apellido: soloLetras(e.target.value) })}
                                placeholder="Solo letras"
                                maxLength={100}
                                required
                            />
                        </div>

                        {/* DNI | Fecha de Nacimiento */}
                        <div className="form-group">
                            <label className="form-label" htmlFor="dni">
                                DNI <span style={{ color: 'red' }}>*</span>
                            </label>
                            <input
                                id="dni"
                                type="text"
                                className="form-input"
                                inputMode="numeric"
                                value={form.dni}
                                onChange={(e) => setForm({ ...form, dni: soloDigitos(e.target.value) })}
                                placeholder="7-8 dígitos"
                                maxLength={8}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label" htmlFor="fechaNacimiento">
                                Fecha de Nacimiento <span style={{ color: 'red' }}>*</span>
                            </label>
                            <input
                                id="fechaNacimiento"
                                type="date"
                                className="form-input"
                                value={form.fechaNacimiento}
                                onChange={(e) => setForm({ ...form, fechaNacimiento: e.target.value })}
                                max={hoy}
                                min="1900-01-01"
                                required
                            />
                        </div>

                        {/* Urgencia — full width */}
                        <div className="form-group solicitud-full">
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

                        {/* Observaciones — full width */}
                        <div className="form-group solicitud-full">
                            <label className="form-label" htmlFor="observaciones">Observaciones</label>
                            <textarea
                                id="observaciones"
                                className="form-textarea"
                                value={form.observaciones}
                                onChange={(e) => setForm({ ...form, observaciones: e.target.value })}
                                placeholder="Indicá para qué necesitás el certificado..."
                            />
                        </div>

                        {/* Botones — full width */}
                        <div className="solicitud-full" style={{ display: 'flex', gap: 'var(--space-sm)' }}>
                            <button
                                type="submit"
                                className="btn btn-primary"
                                disabled={loading}
                                id="btn-crear-solicitud"
                            >
                                {loading ? 'Enviando...' : 'Enviar Solicitud'}
                            </button>
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => navigate(-1)}
                            >
                                Cancelar
                            </button>
                        </div>

                    </div>
                </form>
            </div>
        </div>
    );
}
