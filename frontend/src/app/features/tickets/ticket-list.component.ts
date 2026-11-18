import { ChangeDetectorRef, Component, OnInit, inject } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { finalize, map } from "rxjs";
import { AuthService } from "../../core/auth/auth.service";
import { CategoriaService } from "../../core/services/categoria.service";
import { ConfirmationService } from "../../core/services/confirmation.service";
import { NotificationService } from "../../core/services/notification.service";
import { TicketService } from "../../core/services/ticket.service";
import { Categoria } from "../../shared/models/categoria.model";
import {
  EstadoTicket,
  PrioridadTicket,
  Ticket,
} from "../../shared/models/ticket.model";
import { IconComponent } from "../../shared/ui/icon.component";
import {
  estadoBadgeClass,
  estadoLabel,
  formatDate,
  prioridadBadgeClass,
  prioridadLabel,
} from "../../shared/ui/ticket-ui";

@Component({
  selector: "app-ticket-list",
  imports: [RouterLink, FormsModule, IconComponent],
  template: `
    <section>
      <div class="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 class="text-2xl font-bold text-navy-900">{{ pageTitle }}</h1>
          <p class="mt-1 text-sm text-slate-500">
            Busca y filtra las solicitudes de soporte.
          </p>
        </div>
        @if (canCreate) {
          <a routerLink="/tickets/nuevo" class="btn-primary">
            <app-icon name="plus" class="h-4 w-4" />
            Nuevo ticket
          </a>
        }
      </div>

      <div class="card mb-4 p-4">
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-[2fr_1fr_1fr_1fr]">
          <div class="relative sm:col-span-2 lg:col-span-1">
            <app-icon
              name="search"
              class="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400"
            />
            <input
              [(ngModel)]="searchTerm"
              class="input pl-9"
              placeholder="Buscar por título, #id o solicitante"
              aria-label="Buscar tickets"
            />
          </div>
          <select
            [(ngModel)]="selectedEstado"
            (ngModelChange)="onFilterChange()"
            class="input"
            aria-label="Filtrar por estado"
          >
            <option value="">Todos los estados</option>
            @for (state of states; track state) {
              <option [value]="state">{{ estadoLabel(state) }}</option>
            }
          </select>
          <select
            [(ngModel)]="selectedPrioridad"
            (ngModelChange)="onFilterChange()"
            class="input"
            aria-label="Filtrar por prioridad"
          >
            <option value="">Toda prioridad</option>
            @for (priority of priorities; track priority) {
              <option [value]="priority">{{ prioridadLabel(priority) }}</option>
            }
          </select>
          <select
            [(ngModel)]="selectedCategoriaId"
            (ngModelChange)="onFilterChange()"
            class="input"
            aria-label="Filtrar por categoría"
          >
            <option value="">Toda categoría</option>
            @for (categoria of categorias; track categoria.id) {
              <option [value]="categoria.id">{{ categoria.nombre }}</option>
            }
          </select>
        </div>
        <div class="mt-3 flex flex-col gap-3 sm:flex-row sm:items-center">
          <div class="flex flex-1 items-center gap-2">
            <input
              type="date"
              [(ngModel)]="dateFrom"
              (ngModelChange)="onFilterChange()"
              class="input"
              aria-label="Desde"
            />
            <span class="text-slate-400">–</span>
            <input
              type="date"
              [(ngModel)]="dateTo"
              (ngModelChange)="onFilterChange()"
              class="input"
              aria-label="Hasta"
            />
          </div>
          <button
            type="button"
            (click)="clearFilters()"
            class="btn-ghost sm:w-auto"
          >
            Limpiar filtros
          </button>
        </div>
      </div>

      <div class="card overflow-hidden">
        @if (isLoading) {
          <div class="flex justify-center py-16">
            <div
              class="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-navy-800"
            ></div>
          </div>
        } @else if (filteredTickets.length === 0) {
          <div class="p-10 text-center">
            <p class="font-medium text-slate-700">
              No se encontraron tickets con esos criterios.
            </p>
            <p class="mt-2 text-sm text-slate-500">
              Prueba con otros filtros o crea una nueva solicitud.
            </p>
          </div>
        } @else {
          <div class="hidden overflow-x-auto md:block">
            <table class="w-full text-sm">
              <thead
                class="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500"
              >
                <tr>
                  <th class="px-4 py-3 font-semibold">#</th>
                  <th class="px-4 py-3 font-semibold">Ticket</th>
                  <th class="px-4 py-3 font-semibold">Solicitante</th>
                  <th class="px-4 py-3 font-semibold">Técnico</th>
                  <th class="px-4 py-3 font-semibold">Categoría</th>
                  <th class="px-4 py-3 font-semibold">Prioridad</th>
                  <th class="px-4 py-3 font-semibold">Estado</th>
                  <th class="px-4 py-3 font-semibold">Creado</th>
                  @if (canDelete) {
                    <th class="w-10 px-2 py-3">
                      <span class="sr-only">Acciones</span>
                    </th>
                  }
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (ticket of filteredTickets; track ticket.id) {
                  <tr class="group transition hover:bg-sky-50/60">
                    <td class="px-4 py-3 text-slate-400">#{{ ticket.id }}</td>
                    <td class="px-4 py-3">
                      <a
                        [routerLink]="['/tickets', ticket.id]"
                        class="font-medium text-navy-900 hover:text-sky-700"
                        >{{ ticket.titulo }}</a
                      >
                    </td>
                    <td class="whitespace-nowrap px-4 py-3 text-slate-600">
                      {{ ticket.usuarioNombre || "—" }}
                    </td>
                    <td class="whitespace-nowrap px-4 py-3 text-slate-600">
                      @if (ticket.tecnicoNombre) {
                        {{ ticket.tecnicoNombre }}
                      } @else {
                        <span class="text-slate-400">Sin asignar</span>
                      }
                    </td>
                    <td class="whitespace-nowrap px-4 py-3 text-slate-600">
                      {{ ticket.categoriaNombre || "—" }}
                    </td>
                    <td class="px-4 py-3">
                      <span
                        class="inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold"
                        [class]="prioridadBadgeClass(ticket.prioridad)"
                        >{{ prioridadLabel(ticket.prioridad) }}</span
                      >
                    </td>
                    <td class="px-4 py-3">
                      <span
                        class="inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold"
                        [class]="estadoBadgeClass(ticket.estado)"
                        >{{ estadoLabel(ticket.estado) }}</span
                      >
                    </td>
                    <td class="whitespace-nowrap px-4 py-3 text-slate-500">
                      {{ formatDate(ticket.fechaCreacion) }}
                    </td>
                    @if (canDelete) {
                      <td class="px-2 py-3 text-right">
                        <button
                          type="button"
                          (click)="remove(ticket)"
                          class="rounded-lg p-1.5 text-slate-300 opacity-0 transition hover:bg-rose-50 hover:text-rose-600 focus:opacity-100 group-hover:opacity-100"
                          [attr.aria-label]="'Eliminar ticket #' + ticket.id"
                        >
                          <app-icon name="trash" class="h-4 w-4" />
                        </button>
                      </td>
                    }
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <div class="divide-y divide-slate-100 md:hidden">
            @for (ticket of filteredTickets; track ticket.id) {
              <div class="flex items-start gap-3 p-4">
                <a
                  [routerLink]="['/tickets', ticket.id]"
                  class="min-w-0 flex-1"
                >
                  <p class="text-xs text-slate-400">#{{ ticket.id }}</p>
                  <p class="mt-0.5 font-semibold text-navy-900">
                    {{ ticket.titulo }}
                  </p>
                  <div
                    class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500"
                  >
                    <span>{{ ticket.categoriaNombre || "—" }}</span>
                    <span>{{ ticket.tecnicoNombre || "Sin asignar" }}</span>
                    <span>{{ formatDate(ticket.fechaCreacion) }}</span>
                  </div>
                  <div class="mt-2 flex flex-wrap gap-1.5">
                    <span
                      class="rounded-full px-2.5 py-0.5 text-xs font-semibold"
                      [class]="prioridadBadgeClass(ticket.prioridad)"
                      >{{ prioridadLabel(ticket.prioridad) }}</span
                    >
                    <span
                      class="rounded-full px-2.5 py-0.5 text-xs font-semibold"
                      [class]="estadoBadgeClass(ticket.estado)"
                      >{{ estadoLabel(ticket.estado) }}</span
                    >
                  </div>
                </a>
                @if (canDelete) {
                  <button
                    type="button"
                    (click)="remove(ticket)"
                    class="rounded-lg p-1.5 text-slate-400 transition hover:bg-rose-50 hover:text-rose-600"
                    [attr.aria-label]="'Eliminar ticket #' + ticket.id"
                  >
                    <app-icon name="trash" class="h-4 w-4" />
                  </button>
                }
              </div>
            }
          </div>

          <div
            class="border-t border-slate-100 px-4 py-3 text-xs text-slate-500"
          >
            {{ filteredTickets.length }} resultado(s)
          </div>
        }
      </div>
    </section>
  `,
})
export class TicketListComponent implements OnInit {
  readonly states: EstadoTicket[] = [
    "ABIERTO",
    "EN_PROGRESO",
    "RESUELTO",
    "CERRADO",
  ];
  readonly priorities: PrioridadTicket[] = ["BAJA", "MEDIA", "ALTA", "URGENTE"];
  readonly estadoLabel = estadoLabel;
  readonly prioridadLabel = prioridadLabel;
  readonly estadoBadgeClass = estadoBadgeClass;
  readonly prioridadBadgeClass = prioridadBadgeClass;
  readonly formatDate = formatDate;

  tickets: Ticket[] = [];
  categorias: Categoria[] = [];
  searchTerm = "";
  selectedEstado = "";
  selectedPrioridad = "";
  selectedCategoriaId = "";
  dateFrom = "";
  dateTo = "";
  isAssignedView = false;
  isLoading = false;
  deletingTicketId: number | null = null;

  private readonly ticketService = inject(TicketService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly confirmations = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);

  get canCreate(): boolean {
    return this.auth.role() !== "TECNICO";
  }

  get canDelete(): boolean {
    return this.auth.role() === "ADMIN";
  }

  get pageTitle(): string {
    if (this.isAssignedView || this.auth.role() === "TECNICO") {
      return "Tickets asignados";
    }
    return this.auth.role() === "USUARIO" ? "Mis tickets" : "Todos los tickets";
  }

  get filteredTickets(): Ticket[] {
    const term = this.searchTerm.trim().toLowerCase();
    return this.tickets.filter((ticket) => {
      const matchesTerm =
        !term ||
        ticket.titulo.toLowerCase().includes(term) ||
        String(ticket.id).includes(term) ||
        (ticket.usuarioNombre ?? "").toLowerCase().includes(term);
      const matchesEstado =
        !this.selectedEstado || ticket.estado === this.selectedEstado;
      const matchesPrioridad =
        !this.selectedPrioridad || ticket.prioridad === this.selectedPrioridad;
      const matchesCategory =
        !this.selectedCategoriaId ||
        ticket.categoriaId === Number(this.selectedCategoriaId);
      const date = ticket.fechaCreacion.slice(0, 10);
      const matchesFrom = !this.dateFrom || date >= this.dateFrom;
      const matchesTo = !this.dateTo || date <= this.dateTo;
      return (
        matchesTerm &&
        matchesEstado &&
        matchesPrioridad &&
        matchesCategory &&
        matchesFrom &&
        matchesTo
      );
    });
  }

  ngOnInit(): void {
    this.isAssignedView = this.route.snapshot.data["assignedOnly"] === true;
    this.loadCategories();
    this.loadTickets();
  }

  loadCategories(): void {
    this.categoriaService
      .listar()
      .pipe(finalize(() => this.changeDetector.markForCheck()))
      .subscribe({
        next: (categorias) => {
          this.categorias = categorias;
          this.changeDetector.markForCheck();
        },
        error: () => {
          this.changeDetector.markForCheck();
        },
      });
  }

  loadTickets(): void {
    this.isLoading = true;
    const hasFilters = Boolean(
      this.selectedEstado ||
      this.selectedPrioridad ||
      this.selectedCategoriaId ||
      this.dateFrom ||
      this.dateTo,
    );
    const request$ =
      this.isAssignedView && this.auth.userId() !== null
        ? this.ticketService.misAsignados(this.auth.userId()!)
        : this.auth.role() === "ADMIN" && hasFilters
          ? this.ticketService.filtrar({
              estado: this.selectedEstado,
              prioridad: this.selectedPrioridad,
              categoriaId: Number(this.selectedCategoriaId) || undefined,
              desde: this.dateFrom,
              hasta: this.dateTo,
            })
          : this.ticketService.listar();

    request$
      .pipe(
        map((tickets) => this.filteredTicketsFrom(tickets)),
        finalize(() => {
          this.isLoading = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: (tickets) => {
          this.tickets = tickets;
          this.changeDetector.markForCheck();
        },
        error: () => {
          this.changeDetector.markForCheck();
        },
      });
  }

  onFilterChange(): void {
    this.loadTickets();
  }

  clearFilters(): void {
    this.searchTerm = "";
    this.selectedEstado = "";
    this.selectedPrioridad = "";
    this.selectedCategoriaId = "";
    this.dateFrom = "";
    this.dateTo = "";
    this.loadTickets();
  }

  async remove(ticket: Ticket): Promise<void> {
    const confirmed = await this.confirmations.confirm({
      title: `Eliminar ticket #${ticket.id}`,
      message:
        "¿Seguro que deseas eliminar este ticket? Esta acción no se puede deshacer.",
      confirmLabel: "Eliminar",
      cancelLabel: "Cancelar",
      tone: "danger",
    });
    if (!confirmed) {
      return;
    }
    this.deletingTicketId = ticket.id;
    this.ticketService
      .eliminar(ticket.id)
      .pipe(
        finalize(() => {
          this.deletingTicketId = null;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.notifications.success(
            "Ticket eliminado",
            `El ticket #${ticket.id} ya no está disponible.`,
          );
          this.loadTickets();
          this.changeDetector.markForCheck();
        },
        error: () => {
          this.changeDetector.markForCheck();
        },
      });
  }

  private filteredTicketsFrom(tickets: Ticket[]): Ticket[] {
    const term = this.searchTerm.trim().toLowerCase();
    return tickets.filter((ticket) => {
      const matchesTerm =
        !term ||
        ticket.titulo.toLowerCase().includes(term) ||
        String(ticket.id).includes(term) ||
        (ticket.usuarioNombre ?? "").toLowerCase().includes(term);
      return matchesTerm;
    });
  }
}
