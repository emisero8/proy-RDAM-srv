import { ESTADOS } from '../utils/constants';

export default function EstadoBadge({ estado }) {
    const config = ESTADOS[estado] || { label: estado, className: '' };
    return <span className={`badge ${config.className}`}>{config.label}</span>;
}
