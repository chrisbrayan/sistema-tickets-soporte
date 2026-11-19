import { ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { TicketService } from '../../core/services/ticket.service';
import { EstadoTicket, PrioridadTicket, Ticket, TicketHistory } from '../../shared/models/ticket.model';
import { estadoLabel, firstName, formatDateTime, prioridadLabel } from '../../shared/ui/ticket-ui';

interface ActivityItem extends TicketHistory {
  ticket: Ticket;
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  template: `
    <section>
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-navy-900">Hola, {{ firstName(displayName) }}</h1>
        <p class="mt-1 text-sm text-slate-500">Resumen de {{ scopeLabel }}.</p>
      </div>

      @if (isLoading && !isBackgroundRefreshing) {
        <div class="mb-6 h-1 overflow-hidden rounded-full bg-blue-100"><div class="h-full w-1/2 animate-pulse rounded-full bg-sky-brand"></div></div>
      }

      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        @for (card of cards; track card.state) {
          <article class="card p-5">
            <div class="flex items-center justify-between">
              <p class="text-sm font-medium text-slate-500">{{ card.label }}</p>
              <span class="h-2.5 w-2.5 rounded-full" [style.background]="card.color"></span>
            </div>
            <p class="mt-3 text-3xl font-bold text-navy-900">{{ count(card.state) }}</p>
            <p class="mt-1 text-xs text-slate-400">de {{ totalTickets }} tickets</p>
          </article>
        }
      </div>

      <div class="mt-6 grid gap-6 lg:grid-cols-3">
        <article class="card p-5">
          <h2 class="mb-4 font-semibold text-navy-900">Por prioridad</h2>
          <div class="space-y-3">
            @for (priority of priorities; track priority.value) {
              <div>
                <div class="mb-1 flex justify-between text-sm">
                  <span class="text-slate-600">{{ priority.label }}</span>
                  <span class="font-semibold text-navy-900">{{ countPriority(priority.value) }}</span>
                </div>
                <div class="h-2 rounded-full bg-slate-100">
                  <div class="h-2 rounded-full transition-all" [style.width.%]="barWidth(countPriority(priority.value), maxPriority)" [style.background]="priority.color"></div>
                </div>
              </div>
            }
          </div>
        </article>

        <article class="card p-5">
          <h2 class="mb-4 font-semibold text-navy-900">Por categoría</h2>
          <div class="space-y-3">
            @for (category of categoryRows; track category.name) {
              <div>
                <div class="mb-1 flex justify-between text-sm">
                  <span class="text-slate-600">{{ category.name }}</span>
                  <span class="font-semibold text-navy-900">{{ category.total }}</span>
                </div>
                <div class="h-2 rounded-full bg-slate-100">
                  <div class="h-2 rounded-full bg-navy-700 transition-all" [style.width.%]="barWidth(category.total, maxCategory)"></div>
                </div>
              </div>
            }
          </div>
        </article>

        <article class="card p-5">
          <h2 class="mb-4 font-semibold text-navy-900">Tickets creados (7 días)</h2>
          <div class="flex h-36 items-end gap-2">
            @for (day of recentDays; track day.iso) {
              <div class="flex flex-1 flex-col items-center justify-end gap-1">
                <span class="text-xs font-semibold text-navy-900">{{ day.total }}</span>
                <div class="w-full rounded-t-md bg-sky-brand transition-all"
                     [style.height.%]="dayHeight(day.total, maxDay)"
                     [style.opacity]="day.total ? 1 : 0.2"></div>
                <span class="text-xs capitalize text-slate-400">{{ day.label }}</span>
              </div>
            }
          </div>
        </article>
      </div>

      <article class="card mt-6 p-5">
        <h2 class="mb-4 font-semibold text-navy-900">Actividad reciente</h2>
        @if (!isLoading && activity.length === 0) {
          <p class="py-4 text-sm text-slate-500">Sin actividad todavía.</p>
        } @else {
          <ul class="divide-y divide-slate-100">
            @for (item of activity; track item.id) {
              <li>
                <a [routerLink]="['/tickets', item.ticket.id]" class="flex items-center justify-between gap-4 py-3 transition hover:bg-slate-50">
                  <span class="min-w-0 text-sm">
                    <span class="block truncate font-medium text-navy-900">#{{ item.ticket.id }} {{ item.ticket.titulo }}</span>
                    <span class="mt-0.5 block truncate text-slate-500">
                      {{ activityText(item) }}@if (item.ticket.usuarioId !== auth.userId()) { · {{ item.ticket.usuarioNombre }} }
                    </span>
                  </span>
                  <span class="shrink-0 text-xs text-slate-400">{{ formatDateTime(item.fecha) }}</span>
                </a>
              </li>
            }
          </ul>
        }
      </article>
    </section>
  `
})
export class DashboardComponent implements OnInit, OnDestroy {
  readonly auth = inject(AuthService);
  readonly cards = [
    { label: 'Abierto', state: 'ABIERTO' as EstadoTicket, color: '#3b9cf5' },
    { label: 'En proceso', state: 'EN_PROGRESO' as EstadoTicket, color: '#f59e0b' },
    { label: 'Resuelto', state: 'RESUELTO' as EstadoTicket, color: '#10b981' },
    { label: 'Cerrado', state: 'CERRADO' as EstadoTicket, color: '#94a3b8' }
  ];
  readonly priorities = [
    { value: 'BAJA' as PrioridadTicket, label: 'Baja', color: '#94a3b8' },
    { value: 'MEDIA' as PrioridadTicket, label: 'Media', color: '#3b82f6' },
    { value: 'ALTA' as PrioridadTicket, label: 'Alta', color: '#f97316' },
    { value: 'URGENTE' as PrioridadTicket, label: 'Crítica', color: '#e11d48' }
  ];
  readonly categoryNames = ['Hardware', 'Software', 'Redes', 'Accesos', 'Otros'];
  readonly formatDateTime = formatDateTime;
  readonly firstName = firstName;

  tickets: Ticket[] = [];
  activity: ActivityItem[] = [];
  isLoading = false;
  isBackgroundRefreshing = false;
  private refreshTimer: number | undefined;

  private readonly refreshOnFocus = (): void => {
    this.load(true);
  };

  private readonly ticketService = inject(TicketService);
  private readonly notifications = inject(NotificationService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);

  get displayName(): string {
    return this.auth.currentLogin()?.userName ?? this.auth.subject()?.split('@')[0] ?? 'Usuario';
  }

  get scopeLabel(): string {
    if (this.auth.role() === 'ADMIN') {
      return 'todos los tickets';
    }
    return this.auth.role() === 'TECNICO' ? 'tus tickets asignados' : 'tus tickets';
  }

  get totalTickets(): number {
    return this.tickets.length;
  }

  get maxPriority(): number {
    return Math.max(1, ...this.priorities.map(priority => this.countPriority(priority.value)));
  }

  get categoryRows(): { name: string; total: number }[] {
    return this.categoryNames.map(name => ({
      name,
      total: this.tickets.filter(ticket => (ticket.categoriaNombre ?? '').toLowerCase() === name.toLowerCase()).length
    }));
  }

  get maxCategory(): number {
    return Math.max(1, ...this.categoryRows.map(category => category.total));
  }

  get recentDays(): { iso: string; label: string; total: number }[] {
    const days: { iso: string; label: string; total: number }[] = [];
    for (let offset = 6; offset >= 0; offset--) {
      const date = new Date();
      date.setHours(0, 0, 0, 0);
      date.setDate(date.getDate() - offset);
      const iso = this.toLocalIso(date);
      days.push({
        iso,
        label: date.toLocaleDateString('es-PE', { weekday: 'short' }).replace('.', ''),
        total: this.tickets.filter(ticket => ticket.fechaCreacion.slice(0, 10) === iso).length
      });
    }
    return days;
  }

  get maxDay(): number {
    return Math.max(1, ...this.recentDays.map(day => day.total));
  }

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('forbidden') === '1') {
      this.notifications.warning('Acceso restringido', 'Tu rol no tiene permisos para abrir esa pantalla.');
    }
    this.load();
    this.refreshTimer = window.setInterval(() => this.load(true), 30000);
    window.addEventListener('focus', this.refreshOnFocus);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer !== undefined) {
      window.clearInterval(this.refreshTimer);
    }
    window.removeEventListener('focus', this.refreshOnFocus);
  }

  @HostListener('document:visibilitychange')
  onVisibilityChange(): void {
    if (document.visibilityState === 'visible') {
      this.load(true);
    }
  }

  count(state: EstadoTicket): number {
    return this.tickets.filter(ticket => ticket.estado === state).length;
  }

  countPriority(priority: PrioridadTicket): number {
    return this.tickets.filter(ticket => ticket.prioridad === priority).length;
  }

  estadoLabel(estado: EstadoTicket): string {
    return estadoLabel(estado);
  }

  activityText(item: ActivityItem): string {
    const isAssignment = item.estadoAnterior != null && item.estadoAnterior === item.estadoNuevo;
    if (isAssignment && item.ticket.tecnicoNombre) {
      return `Asignado a ${item.ticket.tecnicoNombre}`;
    }
    return item.texto;
  }

  prioridadLabel(prioridad: PrioridadTicket): string {
    return prioridadLabel(prioridad);
  }

  barWidth(value: number, max: number): number {
    return Math.max(0, Math.min(100, (value / Math.max(1, max)) * 100));
  }

  dayHeight(value: number, max: number): number {
    return value === 0 ? 0 : Math.max(4, (value / Math.max(1, max)) * 100);
  }

  private load(silent = false): void {
    if (this.isLoading) {
      return;
    }
    this.isLoading = true;
    this.isBackgroundRefreshing = silent;

    this.ticketService.listar().pipe(
      switchMap(tickets => {
        const visible = this.visibleTickets(tickets);
        const requests = visible.map(ticket => this.ticketService.historial(ticket.id).pipe(
          map(events => events.map(event => ({ ...event, ticket }) as ActivityItem)),
          catchError(() => of([] as ActivityItem[]))
        ));
        return forkJoin({
          tickets: of(visible),
          activity: requests.length
            ? forkJoin(requests).pipe(map(groups => groups.flat()))
            : of([] as ActivityItem[])
        });
      }),
      finalize(() => {
        this.isLoading = false;
        this.isBackgroundRefreshing = false;
        this.changeDetector.markForCheck();
      })
    ).subscribe({
      next: result => {
        this.tickets = result.tickets;
        const latestByTicket = new Map<number, ActivityItem>();
        for (const item of result.activity) {
          const current = latestByTicket.get(item.ticket.id);
          if (!current || item.fecha.localeCompare(current.fecha) > 0) {
            latestByTicket.set(item.ticket.id, item);
          }
        }
        this.activity = [...latestByTicket.values()]
          .sort((a, b) => b.fecha.localeCompare(a.fecha) || b.ticket.id - a.ticket.id)
          .slice(0, 7);
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.changeDetector.markForCheck();
      }
    });
  }

  private visibleTickets(tickets: Ticket[]): Ticket[] {
    const role = this.auth.role();
    if (role === 'TECNICO') {
      return tickets.filter(ticket => ticket.tecnicoId === this.auth.userId());
    }
    if (role === 'USUARIO') {
      return tickets.filter(ticket => ticket.usuarioId === this.auth.userId());
    }
    return tickets;
  }

  private toLocalIso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
