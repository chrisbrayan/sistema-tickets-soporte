import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { toApiError } from '../../core/http/api-error.util';
import { CategoriaService } from '../../core/services/categoria.service';
import { NotificationService } from '../../core/services/notification.service';
import { TicketService } from '../../core/services/ticket.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { Categoria } from '../../shared/models/categoria.model';
import { EstadoTicket, PrioridadTicket, Ticket, TicketHistory } from '../../shared/models/ticket.model';
import { Usuario } from '../../shared/models/usuario.model';
import { AvatarComponent } from '../../shared/ui/avatar.component';
import { IconComponent } from '../../shared/ui/icon.component';
import {
  estadoBadgeClass,
  estadoLabel,
  formatDate,
  formatDateTime,
  prioridadBadgeClass,
  prioridadLabel
} from '../../shared/ui/ticket-ui';

@Component({
  selector: 'app-ticket-detail',
  imports: [ReactiveFormsModule, RouterLink, AvatarComponent, IconComponent],
  template: `
    <section>
      <a [routerLink]="backLink" class="mb-4 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-navy-900">
        <app-icon name="back" class="h-4 w-4" />
        Volver al listado
      </a>

      @if (isLoading) {
        <div class="card flex justify-center py-20">
          <div class="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-navy-800"></div>
        </div>
      } @else if (!ticket && loadFailed) {
        <div class="card p-10 text-center">
          <p class="font-medium text-slate-700">No se pudo cargar el ticket.</p>
          <p class="mt-2 text-sm text-slate-500">Vuelve al listado e inténtalo de nuevo.</p>
        </div>
      }

      @if (ticket) {
        <div class="grid gap-6 lg:grid-cols-3">
          <div class="space-y-6 lg:col-span-2">
            <article class="card p-6">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p class="text-sm text-slate-400">Ticket #{{ ticket.id }}</p>
                  <h1 class="mt-1 text-2xl font-bold text-navy-900">{{ ticket.titulo }}</h1>
                </div>
                <div class="flex flex-wrap gap-2">
                  <span class="inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold" [class]="prioridadBadgeClass(ticket.prioridad)">{{ prioridadLabel(ticket.prioridad) }}</span>
                  <span class="inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold" [class]="estadoBadgeClass(ticket.estado)">{{ estadoLabel(ticket.estado) }}</span>
                </div>
              </div>

              <p class="mt-5 whitespace-pre-line leading-7 text-slate-600">{{ ticket.descripcion }}</p>

              <dl class="mt-6 grid gap-4 border-t border-slate-100 pt-5 text-sm sm:grid-cols-3">
                <div>
                  <dt class="label">Categoría</dt>
                  <dd class="text-slate-700">{{ ticket.categoriaNombre || 'Sin categoría' }}</dd>
                </div>
                <div>
                  <dt class="label">Creado</dt>
                  <dd class="text-slate-700">{{ formatDate(ticket.fechaCreacion) }}</dd>
                </div>
                <div>
                  <dt class="label">Actualizado</dt>
                  <dd class="text-slate-700">{{ formatDateTime(ticket.fechaActualizacion) }}</dd>
                </div>
              </dl>
            </article>

            <article class="card p-6">
              <h2 class="mb-4 font-semibold text-navy-900">Historial</h2>
              @if (isLoadingHistory) {
                <p class="text-sm text-slate-500">Cargando historial…</p>
              } @else if (historyError) {
                <p class="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ historyError }}</p>
              } @else if (history.length === 0) {
                <p class="rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-500">Todavía no hay eventos registrados.</p>
              } @else {
                <ol class="relative space-y-5 border-l-2 border-slate-100 pl-6">
                  @for (event of reversedHistory; track event.id) {
                    <li class="relative">
                      <span class="absolute -left-[31px] top-1 h-2.5 w-2.5 rounded-full bg-sky-brand ring-4 ring-white"></span>
                      <p class="text-sm font-medium text-navy-900">{{ event.texto }}</p>
                      <p class="mt-0.5 text-xs text-slate-400">{{ formatDateTime(event.fecha) }}</p>
                      @if (showEventComment(event)) {
                        <p class="mt-2 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600">{{ event.comentario }}</p>
                      }
                    </li>
                  }
                </ol>
              }
            </article>
          </div>

          <aside class="space-y-6">
            <div class="card space-y-5 p-5">
              <div>
                <p class="label">Solicitante</p>
                <div class="flex items-center gap-3">
                  <app-avatar [name]="ticket.usuarioNombre" size="sm" />
                  <div class="min-w-0 text-sm">
                    <p class="truncate font-medium text-navy-900">{{ ticket.usuarioNombre || '—' }}</p>
                    <p class="truncate text-slate-500">{{ ticket.usuarioEmail || 'Sin correo' }}</p>
                  </div>
                </div>
              </div>

              <div>
                <p class="label">Técnico asignado</p>
                @if (canAssign && ticket.estado !== 'CERRADO') {
                  <select [formControl]="assignForm.controls.tecnicoId" (change)="onTechnicianSelection()" [disabled]="isSavingAssign || isLoadingTechnicians" class="input">
                    <option [ngValue]="null" [disabled]="ticket.tecnicoId !== null">Sin asignar</option>
                    @for (technician of technicians; track technician.id) {
                      <option [ngValue]="technician.id">{{ technician.nombre }}</option>
                    }
                  </select>
                  @if (showAssignForm) {
                    <textarea [formControl]="assignForm.controls.comentario" rows="2" class="input mt-2" placeholder="Comentario opcional"></textarea>
                    <div class="mt-2 flex gap-2">
                      <button type="button" (click)="assign()" [disabled]="isSavingAssign" class="btn-primary flex-1 px-3 py-2">{{ isSavingAssign ? 'Guardando…' : 'Guardar' }}</button>
                      <button type="button" (click)="cancelAssign()" class="btn-ghost px-3 py-2">Cancelar</button>
                    </div>
                  }
                } @else if (ticket.tecnicoNombre) {
                  <div class="flex items-center gap-3">
                    <app-avatar [name]="ticket.tecnicoNombre" size="sm" />
                    <span class="truncate text-sm font-medium text-navy-900">{{ ticket.tecnicoNombre }}</span>
                  </div>
                } @else {
                  <p class="text-sm text-slate-400">Sin asignar</p>
                }
              </div>

              <div>
                <p class="label">Estado</p>
                @if (canOperate && ticket.estado !== 'CERRADO') {
                  <select [formControl]="stateForm.controls.estado" (change)="onStateSelection()" [disabled]="isSavingState" class="input">
                    @for (state of states; track state) {
                      <option [value]="state">{{ estadoLabel(state) }}</option>
                    }
                  </select>
                  @if (showStateForm) {
                    <textarea [formControl]="stateForm.controls.comentario" rows="2" class="input mt-2" placeholder="Observación de la atención"></textarea>
                    <div class="mt-2 flex gap-2">
                      <button type="button" (click)="changeState()" [disabled]="isSavingState" class="btn-primary flex-1 px-3 py-2">{{ isSavingState ? 'Actualizando…' : 'Guardar' }}</button>
                      <button type="button" (click)="cancelState()" class="btn-ghost px-3 py-2">Cancelar</button>
                    </div>
                  }
                } @else {
                  <span class="inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold" [class]="estadoBadgeClass(ticket.estado)">{{ estadoLabel(ticket.estado) }}</span>
                }
              </div>
            </div>

            @if (canEdit) {
              <div class="card space-y-3 p-5">
                <p class="label">Acciones</p>
                <button type="button" (click)="openEditDialog()" [disabled]="ticket.estado === 'CERRADO'"
                        class="btn-ghost w-full whitespace-nowrap"
                        [title]="ticket.estado === 'CERRADO' ? 'Un ticket cerrado no puede editarse' : 'Editar los datos del ticket'">
                  <app-icon name="edit" class="h-4 w-4" />
                  Editar ticket
                </button>
                @if (ticket.estado !== 'CERRADO') {
                  <button type="button" (click)="showCloseDialog = true" class="btn-primary w-full whitespace-nowrap">
                    <app-icon name="check" class="h-4 w-4" />
                    Cerrar ticket
                  </button>
                }
              </div>
            } @else if (canOperate && ticket.estado !== 'CERRADO') {
              <div class="card space-y-3 p-5">
                <p class="label">Acciones</p>
                <button type="button" (click)="showCloseDialog = true" class="btn-primary w-full whitespace-nowrap">
                  <app-icon name="check" class="h-4 w-4" />
                  Cerrar ticket
                </button>
              </div>
            }

            @if (canCloseAsRequester) {
              <div class="card space-y-3 p-5">
                <p class="label">Acciones</p>
                <button type="button" (click)="showCloseDialog = true" class="btn-primary w-full">
                  <app-icon name="check" class="h-4 w-4" />
                  Cerrar mi solicitud
                </button>
              </div>
            }

            @if (auth.role() === 'USUARIO') {
              <p class="rounded-lg bg-sky-50 p-4 text-sm leading-6 text-sky-800">Recibirás la actualización de estado aquí cuando un técnico atienda tu solicitud.</p>
            }
          </aside>
        </div>
      }
    </section>

    @if (showCloseDialog && ticket) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/60 p-4 backdrop-blur-sm" (click)="showCloseDialog = false">
        <div class="card max-h-[90vh] w-full max-w-lg overflow-auto p-6 shadow-xl" role="dialog" aria-modal="true" aria-labelledby="close-dialog-title" (click)="$event.stopPropagation()">
          <div class="mb-4 flex items-center justify-between">
            <h2 id="close-dialog-title" class="text-lg font-semibold text-navy-900">Cerrar ticket</h2>
            <button type="button" (click)="showCloseDialog = false" class="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100" aria-label="Cerrar">
              <app-icon name="close" class="h-5 w-5" />
            </button>
          </div>
          <p class="text-sm text-slate-600">¿Confirmas que el ticket #{{ ticket.id }} está resuelto? Una vez cerrado no podrá modificarse.</p>
          <textarea [formControl]="closeForm.controls.comentario" rows="3" class="input mt-4" placeholder="Observación final (opcional)"></textarea>
          <div class="mt-6 flex justify-end gap-3">
            <button type="button" (click)="showCloseDialog = false" class="btn-ghost whitespace-nowrap">Cancelar</button>
            <button type="button" (click)="closeTicket()" [disabled]="isClosing" class="btn-danger whitespace-nowrap">{{ isClosing ? 'Cerrando…' : 'Cerrar ticket' }}</button>
          </div>
        </div>
      </div>
    }
    @if (showEditDialog && ticket) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/60 p-4 backdrop-blur-sm" (click)="closeEditDialog()">
        <section class="card max-h-[90vh] w-full max-w-lg overflow-auto p-6 shadow-xl" role="dialog" aria-modal="true" aria-labelledby="edit-dialog-title" (click)="$event.stopPropagation()">
          <div class="mb-4 flex items-center justify-between">
            <h2 id="edit-dialog-title" class="text-lg font-semibold text-navy-900">Editar ticket #{{ ticket.id }}</h2>
            <button type="button" (click)="closeEditDialog()" class="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100" aria-label="Cerrar">
              <app-icon name="close" class="h-5 w-5" />
            </button>
          </div>

          <form class="space-y-4" [formGroup]="editForm" (ngSubmit)="saveEdit()" novalidate>
            <div>
              <label class="label" for="edit-ticket-title">Título</label>
              <input id="edit-ticket-title" formControlName="titulo" class="input" />
              @if (editSubmitted && editForm.controls.titulo.hasError('required')) {
                <span class="mt-1 block text-xs text-rose-600">El título es obligatorio.</span>
              } @else if (editSubmitted && editForm.controls.titulo.hasError('minlength')) {
                <span class="mt-1 block text-xs text-rose-600">El título debe tener al menos 5 caracteres.</span>
              } @else if (editSubmitted && editForm.controls.titulo.hasError('maxlength')) {
                <span class="mt-1 block text-xs text-rose-600">El título no puede superar 120 caracteres.</span>
              }
            </div>

            <div>
              <label class="label" for="edit-ticket-description">Descripción</label>
              <textarea id="edit-ticket-description" formControlName="descripcion" rows="5" class="input resize-y"></textarea>
              @if (editSubmitted && editForm.controls.descripcion.hasError('required')) {
                <span class="mt-1 block text-xs text-rose-600">La descripción es obligatoria.</span>
              } @else if (editSubmitted && editForm.controls.descripcion.hasError('minlength')) {
                <span class="mt-1 block text-xs text-rose-600">La descripción debe tener al menos 10 caracteres.</span>
              } @else if (editSubmitted && editForm.controls.descripcion.hasError('maxlength')) {
                <span class="mt-1 block text-xs text-rose-600">La descripción no puede superar 2000 caracteres.</span>
              }
            </div>

            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label class="label" for="edit-ticket-priority">Prioridad</label>
                <select id="edit-ticket-priority" formControlName="prioridad" class="input">
                  @for (priority of priorities; track priority) {
                    <option [value]="priority">{{ prioridadLabel(priority) }}</option>
                  }
                </select>
              </div>
              <div>
                <label class="label" for="edit-ticket-category">Categoría</label>
                <select id="edit-ticket-category" formControlName="categoriaId" class="input">
                  <option [ngValue]="null" disabled>Selecciona una categoría</option>
                  @for (categoria of categorias; track categoria.id) {
                    <option [ngValue]="categoria.id">{{ categoria.nombre }}</option>
                  }
                </select>
                @if (editSubmitted && editForm.controls.categoriaId.hasError('required')) {
                  <span class="mt-1 block text-xs text-rose-600">La categoría es obligatoria.</span>
                }
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button type="button" (click)="closeEditDialog()" class="btn-ghost whitespace-nowrap">Cancelar</button>
              <button type="submit" [disabled]="isSavingEdit || isLoadingCategories" class="btn-primary whitespace-nowrap">
                {{ isSavingEdit ? 'Guardando…' : 'Guardar cambios' }}
              </button>
            </div>
          </form>
        </section>
      </div>
    }
  `
})
export class TicketDetailComponent implements OnInit {
  ticket: Ticket | null = null;
  history: TicketHistory[] = [];
  technicians: Usuario[] = [];
  categorias: Categoria[] = [];
  isLoading = false;
  isLoadingHistory = false;
  isLoadingTechnicians = false;
  isLoadingCategories = false;
  isSavingAssign = false;
  isSavingState = false;
  isSavingEdit = false;
  isClosing = false;
  showAssignForm = false;
  showStateForm = false;
  showCloseDialog = false;
  showEditDialog = false;
  editSubmitted = false;
  loadFailed = false;
  historyError = '';
  readonly states: EstadoTicket[] = ['ABIERTO', 'EN_PROGRESO', 'RESUELTO'];
  readonly priorities: PrioridadTicket[] = ['BAJA', 'MEDIA', 'ALTA', 'URGENTE'];
  readonly estadoLabel = estadoLabel;
  readonly prioridadLabel = prioridadLabel;
  readonly estadoBadgeClass = estadoBadgeClass;
  readonly prioridadBadgeClass = prioridadBadgeClass;
  readonly formatDate = formatDate;
  readonly formatDateTime = formatDateTime;

  readonly assignForm = inject(FormBuilder).nonNullable.group({
    tecnicoId: [null as number | null],
    comentario: ['']
  });
  readonly stateForm = inject(FormBuilder).nonNullable.group({
    estado: ['ABIERTO' as EstadoTicket],
    comentario: ['']
  });
  readonly closeForm = inject(FormBuilder).nonNullable.group({
    comentario: ['']
  });
  readonly editForm = inject(FormBuilder).nonNullable.group({
    titulo: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(120)]],
    descripcion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
    prioridad: ['MEDIA' as PrioridadTicket, [Validators.required]],
    categoriaId: [null as number | null, [Validators.required, Validators.min(1)]]
  });

  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly ticketService = inject(TicketService);
  private readonly usuarioService = inject(UsuarioService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly notifications = inject(NotificationService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  get canOperate(): boolean {
    return this.auth.role() === 'TECNICO' || this.auth.role() === 'ADMIN';
  }

  get canAssign(): boolean {
    return this.auth.role() === 'ADMIN';
  }

  get canEdit(): boolean {
    return this.auth.role() === 'ADMIN';
  }

  get canCloseAsRequester(): boolean {
    return this.auth.role() === 'USUARIO' && this.ticket?.estado !== 'CERRADO';
  }

  get backLink(): string {
    return this.auth.role() === 'TECNICO' ? '/tickets/mis-asignados' : '/tickets';
  }

  get reversedHistory(): TicketHistory[] {
    return [...this.history].reverse();
  }

  showEventComment(event: TicketHistory): boolean {
    const comentario = event.comentario?.trim();
    if (!comentario || comentario === event.texto) {
      return false;
    }
    const isAssignment = event.estadoAnterior != null && event.estadoAnterior === event.estadoNuevo;
    return !(isAssignment && comentario === 'Técnico asignado');
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (this.route.snapshot.queryParamMap.get('saved') === '1') {
      this.notifications.success('Ticket guardado', 'Los cambios se aplicaron correctamente.');
    }
    if (this.canAssign) {
      this.loadTechnicians();
    }
    if (this.canEdit) {
      this.loadCategories();
    }
    this.loadTicket(id);
    this.loadHistory(id);
  }

  onTechnicianSelection(): void {
    const selected = this.assignForm.controls.tecnicoId.value;
    this.showAssignForm = selected !== null && selected !== this.ticket?.tecnicoId;
    if (!this.showAssignForm) {
      this.assignForm.controls.comentario.setValue('');
    }
  }

  onStateSelection(): void {
    this.showStateForm = this.stateForm.controls.estado.value !== this.ticket?.estado;
    if (!this.showStateForm) {
      this.stateForm.controls.comentario.setValue('');
    }
  }

  assign(): void {
    if (!this.ticket || this.isSavingAssign) {
      return;
    }
    const selected = this.assignForm.controls.tecnicoId.value;
    if (selected === null || selected === this.ticket.tecnicoId) {
      this.showAssignForm = false;
      return;
    }
    this.isSavingAssign = true;
    this.ticketService.asignarTecnico(this.ticket.id, {
      tecnicoId: selected,
      comentario: this.assignForm.controls.comentario.value
    }).pipe(finalize(() => {
      this.isSavingAssign = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: ticket => this.operationCompleted(ticket, 'Técnico asignado correctamente.'),
      error: error => this.showError(error)
    });
  }

  cancelAssign(): void {
    this.showAssignForm = false;
    this.assignForm.controls.comentario.setValue('');
    this.assignForm.controls.tecnicoId.setValue(this.ticket?.tecnicoId ?? null);
  }

  openEditDialog(): void {
    if (!this.ticket || this.ticket.estado === 'CERRADO') {
      return;
    }
    this.editSubmitted = false;
    this.editForm.reset({
      titulo: this.ticket.titulo,
      descripcion: this.ticket.descripcion,
      prioridad: this.ticket.prioridad,
      categoriaId: this.ticket.categoriaId
    });
    this.showEditDialog = true;
    if (this.categorias.length === 0) {
      this.loadCategories();
    }
  }

  closeEditDialog(): void {
    if (!this.isSavingEdit) {
      this.showEditDialog = false;
    }
  }

  saveEdit(): void {
    if (!this.ticket || this.isSavingEdit) {
      return;
    }
    this.editSubmitted = true;
    this.editForm.markAllAsTouched();
    if (this.editForm.invalid) {
      return;
    }
    const value = this.editForm.getRawValue();
    this.isSavingEdit = true;
    this.ticketService.actualizar(this.ticket.id, {
      titulo: value.titulo.trim(),
      descripcion: value.descripcion.trim(),
      prioridad: value.prioridad,
      usuarioId: this.ticket.usuarioId,
      categoriaId: Number(value.categoriaId),
      tecnicoId: this.ticket.tecnicoId
    }).pipe(finalize(() => {
      this.isSavingEdit = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: ticket => {
        this.showEditDialog = false;
        this.operationCompleted(ticket, 'Ticket actualizado correctamente.');
      },
      error: () => {
        this.changeDetector.markForCheck();
      }
    });
  }

  changeState(): void {
    if (!this.ticket || this.isSavingState) {
      return;
    }
    this.isSavingState = true;
    this.ticketService.cambiarEstado(this.ticket.id, {
      estado: this.stateForm.controls.estado.value,
      comentario: this.stateForm.controls.comentario.value
    }).pipe(finalize(() => {
      this.isSavingState = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: ticket => this.operationCompleted(ticket, 'Estado actualizado correctamente.'),
      error: error => this.showError(error)
    });
  }

  cancelState(): void {
    this.showStateForm = false;
    this.stateForm.controls.comentario.setValue('');
    this.stateForm.controls.estado.setValue(this.ticket?.estado === 'CERRADO' ? 'RESUELTO' : (this.ticket?.estado ?? 'ABIERTO'));
  }

  closeTicket(): void {
    if (!this.ticket || this.ticket.estado === 'CERRADO' || this.isClosing) {
      return;
    }
    this.isClosing = true;
    this.ticketService.cerrar(this.ticket.id, {
      estado: 'CERRADO',
      comentario: this.closeForm.controls.comentario.value
    }).pipe(finalize(() => {
      this.isClosing = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: ticket => {
        this.showCloseDialog = false;
        this.operationCompleted(ticket, 'Ticket cerrado correctamente.');
      },
      error: error => this.showError(error)
    });
  }

  private loadTicket(id: number): void {
    this.isLoading = true;
    this.loadFailed = false;
    this.ticketService.obtener(id).pipe(finalize(() => {
      this.isLoading = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: ticket => {
        this.setTicket(ticket);
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.loadFailed = true;
        this.showError(error);
      }
    });
  }

  private loadHistory(id: number): void {
    this.isLoadingHistory = true;
    this.historyError = '';
    this.ticketService.historial(id).pipe(finalize(() => {
      this.isLoadingHistory = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: history => {
        this.history = history;
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.historyError = toApiError(error).message;
        this.changeDetector.markForCheck();
      }
    });
  }

  private loadTechnicians(): void {
    this.isLoadingTechnicians = true;
    this.usuarioService.listarCatalogo('TECNICO').pipe(finalize(() => {
      this.isLoadingTechnicians = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: technicians => {
        this.technicians = technicians;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.changeDetector.markForCheck();
      }
    });
  }

  private loadCategories(): void {
    this.isLoadingCategories = true;
    this.categoriaService.listar().pipe(finalize(() => {
      this.isLoadingCategories = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: categorias => {
        this.categorias = categorias;
        if (this.editForm.controls.categoriaId.value === null && this.ticket) {
          this.editForm.controls.categoriaId.setValue(this.ticket.categoriaId);
        }
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.changeDetector.markForCheck();
      }
    });
  }

  private operationCompleted(ticket: Ticket, message: string): void {
    this.setTicket(ticket);
    this.notifications.success(message);
    this.assignForm.reset({ tecnicoId: ticket.tecnicoId ?? null, comentario: '' });
    this.stateForm.reset({
      estado: ticket.estado === 'CERRADO' ? 'RESUELTO' : ticket.estado,
      comentario: ''
    });
    this.closeForm.reset({ comentario: '' });
    this.showAssignForm = false;
    this.showStateForm = false;
    this.loadHistory(ticket.id);
    this.changeDetector.markForCheck();
  }

  private setTicket(ticket: Ticket): void {
    this.ticket = ticket;
    this.assignForm.patchValue({ tecnicoId: ticket.tecnicoId ?? null });
    this.stateForm.patchValue({
      estado: ticket.estado === 'CERRADO' ? 'RESUELTO' : ticket.estado
    });
  }

  private showError(_error: unknown): void {
    this.showCloseDialog = false;
    this.changeDetector.markForCheck();
  }
}
