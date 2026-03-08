import { useState, useEffect } from 'react';
import api from '../../api/axios';
import LoadingSpinner from '../../components/LoadingSpinner';
import Modal from '../../components/Modal';
import { ROLES, formatDate } from '../../utils/constants';

export default function Usuarios() {
    const [usuarios, setUsuarios] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [actionLoading, setActionLoading] = useState(false);

    // Create modal
    const [showCreate, setShowCreate] = useState(false);
    const [createForm, setCreateForm] = useState({
        nombre: '', apellido: '', email: '', password: '', dniCuil: '', telefono: '', rol: 'GESTOR'
    });

    // Edit modal
    const [showEdit, setShowEdit] = useState(false);
    const [editForm, setEditForm] = useState({
        id: null, nombre: '', apellido: '', email: '', dniCuil: '', telefono: '', rol: 'GESTOR', tipo: ''
    });
    const fetchUsuarios = async () => {
        try {
            const { data } = await api.get('/usuarios');
            setUsuarios(Array.isArray(data) ? data : data.content || []);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchUsuarios(); }, []);

    // ─── Create ──────────────────────────────────────────────────────────────
    const handleCreate = async (e) => {
        e.preventDefault();
        if (!createForm.password || createForm.password.length < 8) {
            setError('La contraseña debe tener al menos 8 caracteres');
            return;
        }
        setActionLoading(true);
        setError('');
        try {
            await api.post('/usuarios', createForm);
            setShowCreate(false);
            setCreateForm({ nombre: '', apellido: '', email: '', password: '', dniCuil: '', telefono: '', rol: 'GESTOR' });
            fetchUsuarios();
        } catch (err) {
            setError(err.response?.data?.error?.message || 'Error al crear usuario');
        } finally {
            setActionLoading(false);
        }
    };

    // ─── Edit ────────────────────────────────────────────────────────────────
    const openEdit = (u) => {
        setEditForm({
            id: u.id,
            nombre: u.nombre,
            apellido: u.apellido,
            email: u.email,
            dniCuil: u.dniCuil || '',
            telefono: u.telefono || '',
            rol: u.rol,
            tipo: u.tipo
        });
        setError('');
        setShowEdit(true);
    };

    const handleEdit = async (e) => {
        e.preventDefault();
        setActionLoading(true);
        setError('');
        try {
            await api.put(`/usuarios/${editForm.id}`, {
                nombre: editForm.nombre,
                apellido: editForm.apellido,
                email: editForm.email,
                dniCuil: editForm.dniCuil,
                telefono: editForm.telefono,
                rol: editForm.rol
            });
            setShowEdit(false);
            fetchUsuarios();
        } catch (err) {
            setError(err.response?.data?.error?.message || 'Error al actualizar usuario');
        } finally {
            setActionLoading(false);
        }
    };

    // ─── Toggle Estado ───────────────────────────────────────────────────────
    const toggleEstado = async (id, activo) => {
        try {
            await api.patch(`/usuarios/${id}/estado`, { activo: !activo });
            fetchUsuarios();
        } catch (err) {
            console.error(err);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">Gestión de Usuarios</h1>
                    <p className="page-subtitle">Administración de usuarios internos del sistema</p>
                </div>
                <button className="btn btn-primary" onClick={() => { setError(''); setShowCreate(true); }} id="btn-crear-usuario">
                    + Nuevo Usuario
                </button>
            </div>

            <div className="table-container">
                <table className="table">
                    <thead>
                        <tr>
                            <th>Nombre</th>
                            <th>Email</th>
                            <th>Rol</th>
                            <th>Estado</th>
                            <th>Fecha Alta</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        {usuarios.map((u) => (
                            <tr key={u.id}>
                                <td>{u.nombre} {u.apellido}</td>
                                <td>{u.email}</td>
                                <td>
                                    <span className={`badge ${ROLES[u.rol]?.className || ''}`}>
                                        {ROLES[u.rol]?.label || u.rol}
                                    </span>
                                </td>
                                <td>
                                    <span className={`badge ${u.activo ? 'badge-aprobada' : 'badge-rechazada'}`}>
                                        {u.activo ? 'Activo' : 'Inactivo'}
                                    </span>
                                </td>
                                <td>{formatDate(u.createdAt)}</td>
                                <td style={{ display: 'flex', gap: 'var(--space-xs)' }}>
                                    <button
                                        className="btn btn-sm btn-secondary"
                                        onClick={() => openEdit(u)}
                                        id={`btn-editar-${u.id}`}
                                    >
                                        ✏️ Editar
                                    </button>
                                    <button
                                        className={`btn btn-sm ${u.activo ? 'btn-danger' : 'btn-success'}`}
                                        onClick={() => toggleEstado(u.id, u.activo)}
                                        id={`btn-toggle-${u.id}`}
                                    >
                                        {u.activo ? 'Desactivar' : 'Activar'}
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* ═══ Create User Modal ═══ */}
            <Modal
                isOpen={showCreate}
                onClose={() => setShowCreate(false)}
                title="Nuevo Usuario Interno"
                footer={
                    <>
                        <button className="btn btn-secondary" onClick={() => setShowCreate(false)}>Cancelar</button>
                        <button className="btn btn-primary" onClick={handleCreate} disabled={actionLoading} id="btn-confirmar-crear-usuario">
                            {actionLoading ? 'Creando...' : 'Crear Usuario'}
                        </button>
                    </>
                }
            >
                {error && <div className="toast toast-error" style={{ marginBottom: '1rem' }}>{error}</div>}
                <div className="form-group">
                    <label className="form-label">Nombre *</label>
                    <input className="form-input" value={createForm.nombre} onChange={(e) => setCreateForm({ ...createForm, nombre: e.target.value })} required />
                </div>
                <div className="form-group">
                    <label className="form-label">Apellido *</label>
                    <input className="form-input" value={createForm.apellido} onChange={(e) => setCreateForm({ ...createForm, apellido: e.target.value })} required />
                </div>
                <div className="form-group">
                    <label className="form-label">Email *</label>
                    <input className="form-input" type="email" value={createForm.email} onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })} required />
                </div>
                <div className="form-group">
                    <label className="form-label">Contraseña *</label>
                    <input className="form-input" type="password" value={createForm.password}
                        onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                        placeholder="Mínimo 8 caracteres"
                        required />
                </div>
                <div className="form-group">
                    <label className="form-label">DNI/CUIL</label>
                    <input className="form-input" value={createForm.dniCuil} onChange={(e) => setCreateForm({ ...createForm, dniCuil: e.target.value })} />
                </div>
                <div className="form-group">
                    <label className="form-label">Teléfono</label>
                    <input className="form-input" value={createForm.telefono} onChange={(e) => setCreateForm({ ...createForm, telefono: e.target.value })} />
                </div>
                <div className="form-group">
                    <label className="form-label">Rol *</label>
                    <select className="form-select" value={createForm.rol} onChange={(e) => setCreateForm({ ...createForm, rol: e.target.value })}>
                        <option value="GESTOR">Gestor</option>
                        <option value="ADMIN">Admin</option>
                    </select>
                </div>
            </Modal>

            {/* ═══ Edit User Modal ═══ */}
            <Modal
                isOpen={showEdit}
                onClose={() => setShowEdit(false)}
                title="Editar Usuario"
                footer={
                    <>
                        <button className="btn btn-secondary" onClick={() => setShowEdit(false)}>Cancelar</button>
                        <button className="btn btn-primary" onClick={handleEdit} disabled={actionLoading} id="btn-confirmar-editar-usuario">
                            {actionLoading ? 'Guardando...' : 'Guardar Cambios'}
                        </button>
                    </>
                }
            >
                {error && <div className="toast toast-error" style={{ marginBottom: '1rem' }}>{error}</div>}
                <div className="form-group">
                    <label className="form-label">Nombre</label>
                    <input className="form-input" value={editForm.nombre} onChange={(e) => setEditForm({ ...editForm, nombre: e.target.value })} />
                </div>
                <div className="form-group">
                    <label className="form-label">Apellido</label>
                    <input className="form-input" value={editForm.apellido} onChange={(e) => setEditForm({ ...editForm, apellido: e.target.value })} />
                </div>
                <div className="form-group">
                    <label className="form-label">Email</label>
                    <input className="form-input" type="email" value={editForm.email} onChange={(e) => setEditForm({ ...editForm, email: e.target.value })} />
                </div>
                <div className="form-group">
                    <label className="form-label">DNI/CUIL</label>
                    <input className="form-input" value={editForm.dniCuil} onChange={(e) => setEditForm({ ...editForm, dniCuil: e.target.value })} />
                </div>
                <div className="form-group">
                    <label className="form-label">Teléfono</label>
                    <input className="form-input" value={editForm.telefono} onChange={(e) => setEditForm({ ...editForm, telefono: e.target.value })} />
                </div>
                {editForm.tipo !== 'CIUDADANO' && (
                    <div className="form-group">
                        <label className="form-label">Rol</label>
                        <select className="form-select" value={editForm.rol || 'GESTOR'} onChange={(e) => setEditForm({ ...editForm, rol: e.target.value })}>
                            <option value="GESTOR">Gestor</option>
                            <option value="ADMIN">Admin</option>
                        </select>
                    </div>
                )}
            </Modal>
        </div>
    );
}
