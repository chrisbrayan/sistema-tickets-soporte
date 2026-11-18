export type EstadoTicket = 'ABIERTO' | 'EN_PROGRESO' | 'RESUELTO' | 'CERRADO';
export type PrioridadTicket = 'BAJA' | 'MEDIA' | 'ALTA' | 'URGENTE';

export interface Ticket {
  id: number;
  titulo: string;
  descripcion: string;
  prioridad: PrioridadTicket;
  estado: EstadoTicket;
  usuarioId: number;
  usuarioNombre?: string;
  usuarioEmail?: string;
  categoriaId: number;
  categoriaNombre?: string;
  tecnicoId: number | null;
  tecnicoNombre?: string | null;
  tecnicoEmail?: string | null;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface TicketRequest {
  titulo: string;
  descripcion: string;
  prioridad: PrioridadTicket;
  usuarioId: number;
  categoriaId: number;
  tecnicoId?: number | null;
  estado?: EstadoTicket;
}

export interface AsignarTecnicoRequest {
  tecnicoId: number;
  usuarioEjecutorId?: number;
  comentario?: string;
}

export interface CambiarEstadoRequest {
  estado: EstadoTicket;
  usuarioEjecutorId?: number;
  comentario?: string;
}

export interface TicketHistory {
  id: number;
  fecha: string;
  texto: string;
  comentario?: string | null;
  usuarioNombre?: string;
  estadoAnterior?: EstadoTicket | null;
  estadoNuevo?: EstadoTicket;
}
