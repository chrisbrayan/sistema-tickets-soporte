import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { Categoria } from "../../shared/models/categoria.model";

@Injectable({ providedIn: "root" })
export class CategoriaService {
  private readonly resourceUrl = `${environment.apiUrl}/categorias`;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(this.resourceUrl);
  }

  crear(
    request: Pick<Categoria, "nombre" | "descripcion">,
  ): Observable<Categoria> {
    return this.http.post<Categoria>(this.resourceUrl, request);
  }

  actualizar(
    id: number,
    request: Pick<Categoria, "nombre" | "descripcion">,
  ): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.resourceUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`);
  }
}
