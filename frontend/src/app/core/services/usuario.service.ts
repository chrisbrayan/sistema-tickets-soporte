import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserRole, Usuario } from '../../shared/models/usuario.model';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly resourceUrl = `${environment.apiUrl}/usuarios`;

  constructor(private readonly http: HttpClient) {}

  listarCatalogo(rol?: UserRole): Observable<Usuario[]> {
    const params = rol ? new HttpParams().set('rol', rol) : undefined;
    return this.http.get<Usuario[]>(`${this.resourceUrl}/catalogo`, { params });
  }

  listarTodos(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.resourceUrl);
  }

  crear(request: { nombre: string; email: string; password: string; rol: UserRole }): Observable<Usuario> {
    return this.http.post<Usuario>(this.resourceUrl, request);
  }
}
