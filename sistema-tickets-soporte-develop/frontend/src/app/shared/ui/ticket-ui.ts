import { EstadoTicket, PrioridadTicket } from '../models/ticket.model';

export const ESTADO_LABELS: Record<EstadoTicket, string> = {
  ABIERTO: 'Abierto',
  EN_PROGRESO: 'En proceso',
  RESUELTO: 'Resuelto',
  CERRADO: 'Cerrado'
};

export const PRIORIDAD_LABELS: Record<PrioridadTicket, string> = {
  BAJA: 'Baja',
  MEDIA: 'Media',
  ALTA: 'Alta',
  URGENTE: 'Crítica'
};

const ESTADO_CLASSES: Record<EstadoTicket, string> = {
  ABIERTO: 'bg-sky-100 text-sky-700',
  EN_PROGRESO: 'bg-amber-100 text-amber-700',
  RESUELTO: 'bg-emerald-100 text-emerald-700',
  CERRADO: 'bg-slate-200 text-slate-600'
};

const PRIORIDAD_CLASSES: Record<PrioridadTicket, string> = {
  BAJA: 'bg-slate-100 text-slate-600',
  MEDIA: 'bg-blue-100 text-blue-700',
  ALTA: 'bg-orange-100 text-orange-700',
  URGENTE: 'bg-rose-100 text-rose-700'
};

export function estadoLabel(estado: EstadoTicket): string {
  return ESTADO_LABELS[estado];
}

export function prioridadLabel(prioridad: PrioridadTicket): string {
  return PRIORIDAD_LABELS[prioridad];
}

export function estadoBadgeClass(estado: EstadoTicket): string {
  return ESTADO_CLASSES[estado];
}

export function prioridadBadgeClass(prioridad: PrioridadTicket): string {
  return PRIORIDAD_CLASSES[prioridad];
}

export function initials(name: string | null | undefined): string {
  return (name || '?')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map(part => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

export function firstName(name: string | null | undefined): string {
  return (name || 'Usuario').trim().split(/\s+/)[0] || 'Usuario';
}

function parse(value: string | Date | null | undefined): Date | null {
  if (!value) {
    return null;
  }
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function formatDate(value: string | Date | null | undefined): string {
  const date = parse(value);
  if (!date) {
    return '—';
  }
  return new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  })
    .format(date)
    .replace('.', '');
}

export function formatDateTime(value: string | Date | null | undefined): string {
  const date = parse(value);
  if (!date) {
    return '—';
  }
  const day = new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: 'short'
  })
    .format(date)
    .replace('.', '');
  const time = new Intl.DateTimeFormat('es-PE', {
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23'
  }).format(date);
  return `${day}, ${time}`;
}
