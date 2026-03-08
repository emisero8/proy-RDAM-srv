// ─── Estado de solicitud → label y clase CSS ─────────────────────────────
export const ESTADOS = {
    PENDIENTE_REVISION: { label: 'Pendiente Revisión', className: 'badge-pendiente' },
    EN_REVISION: { label: 'En Revisión', className: 'badge-en-revision' },
    APROBADA: { label: 'Aprobada', className: 'badge-aprobada' },
    RECHAZADA: { label: 'Rechazada', className: 'badge-rechazada' },
    CANCELADA: { label: 'Cancelada', className: 'badge-rechazada' },
    PENDIENTE_PAGO: { label: 'Pendiente Pago', className: 'badge-pendiente-pago' },
    PAGADA: { label: 'Pagada', className: 'badge-pagada' },
    EMITIDA: { label: 'Emitida', className: 'badge-emitida' },
};

export const ROLES = {
    CIUDADANO: { label: 'Ciudadano', className: 'badge-ciudadano' },
    GESTOR: { label: 'Gestor', className: 'badge-gestor' },
    ADMIN: { label: 'Admin', className: 'badge-admin' },
};

export const TIPOS_CERT = [
    { value: 'LIBRE_DEUDA', label: 'Certificado Libre Deuda Alimenticio' },
];

export const URGENCIAS = [
    { value: 'NORMAL', label: 'Normal' },
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
 * El código técnico (SOL-2026-001) queda como referencia secundaria.
 */
export function formatSolicitudNombre(solicitud) {
    if (!solicitud) return '';
    const fecha = solicitud.createdAt
        ? new Date(solicitud.createdAt).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric' })
        : '—';
    // Extrae el número secuencial del código técnico, p.ej. "SOL-2026-001" → "001"
    const seq = solicitud.numero ? solicitud.numero.split('-').pop() : solicitud.id;
    return `Solicitud — ${fecha} — N.° ${seq}`;
}

