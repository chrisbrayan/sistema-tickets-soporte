import { EstadoTicket } from './ticket.model';

export interface TicketStatusSummary {
  estado: EstadoTicket;
  total: number;
}
