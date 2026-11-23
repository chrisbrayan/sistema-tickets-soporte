import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AsignarTecnicoRequest,
  CambiarEstadoRequest,
  Ticket,
  TicketHistory,
  TicketRequest
} from '../../shared/models/ticket.model';
import { TicketStatusSummary } from '../../shared/models/ticket-summary.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly resourceUrl = `${environment.apiUrl}/tickets`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(this.resourceUrl);
  }

  resumenPorEstado(): Observable<TicketStatusSummary[]> {
    return this.http.get<TicketStatusSummary[]>(`${this.resourceUrl}/resumen-por-estado`);
  }

  obtener(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.resourceUrl}/${id}`);
  }

  historial(id: number): Observable<TicketHistory[]> {
    return this.http.get<TicketHistory[]>(`${this.resourceUrl}/${id}/historial`);
  }

  crear(request: TicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(this.resourceUrl, request);
  }

  actualizar(id: number, request: TicketRequest): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.resourceUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`);
  }

  asignarTecnico(id: number, request: AsignarTecnicoRequest): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.resourceUrl}/${id}/asignacion`, request);
  }

  cambiarEstado(id: number, request: CambiarEstadoRequest): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.resourceUrl}/${id}/estado`, request);
  }

  cerrar(id: number, request: CambiarEstadoRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.resourceUrl}/${id}/cerrar`, request);
  }

  buscarPorEstadoYCategoria(estado: string, categoriaId: number): Observable<Ticket[]> {
    const params = new HttpParams().set('estado', estado).set('categoriaId', categoriaId);
    return this.http.get<Ticket[]>(`${this.resourceUrl}/search`, { params });
  }

  filtrar(filters: {
    estado?: string;
    prioridad?: string;
    categoriaId?: number;
    desde?: string;
    hasta?: string;
  }): Observable<Ticket[]> {
    let params = new HttpParams();
    if (filters.estado) params = params.set('estado', filters.estado);
    if (filters.prioridad) params = params.set('prioridad', filters.prioridad);
    if (filters.categoriaId) params = params.set('categoriaId', filters.categoriaId);
    if (filters.desde) params = params.set('desde', `${filters.desde}T00:00:00`);
    if (filters.hasta) params = params.set('hasta', `${filters.hasta}T23:59:59`);
    return this.http.get<Ticket[]>(`${this.resourceUrl}/filtrar`, { params });
  }

  misAsignados(tecnicoId: number): Observable<Ticket[]> {
    const params = new HttpParams().set('tecnicoId', tecnicoId);
    return this.http.get<Ticket[]>(`${this.resourceUrl}/mis-asignados`, { params });
  }
}
