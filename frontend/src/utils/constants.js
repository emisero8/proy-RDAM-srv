// ─── Estado de solicitud → label y clase CSS ─────────────────────────────
//
// Flujo activo: PENDIENTE_PAGO → PAGADA → EMITIDA → EXPIRADA
//               (también CANCELADA desde PENDIENTE_PAGO)
export const ESTADOS = {
    PENDIENTE_PAGO:  { label: 'Pendiente de Pago',  className: 'badge-pendiente-pago' },
    PAGADA:          { label: 'Pagada',              className: 'badge-pagada' },
    EMITIDA:         { label: 'Emitida',             className: 'badge-emitida' },
    EXPIRADA:        { label: 'Expirada',            className: 'badge-expirada' },
    CANCELADA:       { label: 'Cancelada',           className: 'badge-rechazada' },
};

export const ROLES = {
    CIUDADANO: { label: 'Ciudadano', className: 'badge-ciudadano' },
    GESTOR:    { label: 'Gestor',    className: 'badge-gestor' },
    ADMIN:     { label: 'Admin',     className: 'badge-admin' },
};

export const TIPOS_CERT = [
    { value: 'LIBRE_DEUDA', label: 'Certificado Libre Deuda Alimenticio' },
];

export const URGENCIAS = [
    { value: 'NORMAL',  label: 'Normal' },
    { value: 'URGENTE', label: 'Urgente' },
];

export function formatDate(dateString) {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleDateString('es-AR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
    });
}

export function formatCurrency(amount) {
    if (amount == null) return '—';
    return new Intl.NumberFormat('es-AR', {
        style: 'currency', currency: 'ARS',
    }).format(amount);
}

/**
 * Nombre legible para una solicitud.
 * Ejemplo: "Solicitud — 03/03/2026 — N.° 001"
 */
export function formatSolicitudNombre(solicitud) {
    if (!solicitud) return '';
    const fecha = solicitud.createdAt
        ? new Date(solicitud.createdAt).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' })
        : '—';
    const seq = solicitud.numero ? solicitud.numero.split('-').pop() : solicitud.id;
    return `Solicitud — ${fecha} — N.° ${seq}`;
}
