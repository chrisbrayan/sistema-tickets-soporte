import { ChangeDetectorRef, Component, OnInit, inject } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { finalize } from "rxjs";
import { AuthService } from "../../core/auth/auth.service";
import { CategoriaService } from "../../core/services/categoria.service";
import { TicketService } from "../../core/services/ticket.service";
import { Categoria } from "../../shared/models/categoria.model";
import {
  PrioridadTicket,
  Ticket,
  TicketRequest,
} from "../../shared/models/ticket.model";
import { prioridadLabel } from "../../shared/ui/ticket-ui";

const PRIORITY_PATTERN = /^(BAJA|MEDIA|ALTA|URGENTE)$/;

@Component({
  selector: "app-ticket-form",
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-4xl">
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-navy-900">
          {{ isEdit ? "Editar ticket" : "Nuevo ticket" }}
        </h1>
        <p class="mt-1 text-sm text-slate-500">
          {{
            isEdit
              ? "Actualiza la información de la solicitud."
              : "Describe el problema para que el equipo técnico pueda atenderlo."
          }}
        </p>
      </div>

      @if (isLoadingCatalogs) {
        <div class="mb-4 h-1 overflow-hidden rounded-full bg-blue-100">
          <div
            class="h-full w-1/2 animate-pulse rounded-full bg-sky-brand"
          ></div>
        </div>
      }

      <form
        class="card space-y-5 p-6"
        [formGroup]="form"
        (ngSubmit)="submit()"
        novalidate
      >
        <div>
          <label class="label" for="ticket-title">Título</label>
          <input
            id="ticket-title"
            formControlName="titulo"
            class="input"
            placeholder="Ej. No puedo acceder a la VPN"
          />
          @if (showError("titulo", "required")) {
            <span class="mt-1 block text-xs text-rose-600"
              >El título es obligatorio.</span
            >
          } @else if (showError("titulo", "minlength")) {
            <span class="mt-1 block text-xs text-rose-600"
              >El título debe tener al menos 5 caracteres.</span
            >
          } @else if (showError("titulo", "maxlength")) {
            <span class="mt-1 block text-xs text-rose-600"
              >El título no puede superar 120 caracteres.</span
            >
          }
        </div>

        <div>
          <label class="label" for="ticket-description">Descripción</label>
          <textarea
            id="ticket-description"
            formControlName="descripcion"
            class="input min-h-40 resize-y"
            placeholder="Detalla qué ocurre, desde cuándo y qué has intentado."
          ></textarea>
          @if (showError("descripcion", "required")) {
            <span class="mt-1 block text-xs text-rose-600"
              >La descripción es obligatoria.</span
            >
          } @else if (showError("descripcion", "minlength")) {
            <span class="mt-1 block text-xs text-rose-600"
              >La descripción debe tener al menos 10 caracteres.</span
            >
          } @else if (showError("descripcion", "maxlength")) {
            <span class="mt-1 block text-xs text-rose-600"
              >La descripción no puede superar 2000 caracteres.</span
            >
          }
        </div>

        <div class="grid gap-5 sm:grid-cols-2">
          <div>
            <label class="label" for="ticket-priority">Prioridad</label>
            <select
              id="ticket-priority"
              formControlName="prioridad"
              class="input"
            >
              @for (priority of priorities; track priority) {
                <option [value]="priority">
                  {{ prioridadLabel(priority) }}
                </option>
              }
            </select>
            @if (showError("prioridad", "pattern")) {
              <span class="mt-1 block text-xs text-rose-600"
                >Selecciona una prioridad válida.</span
              >
            }
          </div>
          <div>
            <label class="label" for="ticket-category">Categoría</label>
            <select
              id="ticket-category"
              formControlName="categoriaId"
              class="input"
            >
              <option [ngValue]="null" disabled>
                Selecciona una categoría
              </option>
              @for (categoria of categorias; track categoria.id) {
                <option [ngValue]="categoria.id">{{ categoria.nombre }}</option>
              }
            </select>
            @if (showError("categoriaId", "required")) {
              <span class="mt-1 block text-xs text-rose-600"
                >La categoría es obligatoria.</span
              >
            }
          </div>
        </div>

        <div class="flex justify-end gap-3">
          <a routerLink="/tickets" class="btn-ghost">Cancelar</a>
          <button
            type="submit"
            [disabled]="isSaving || isLoadingCatalogs || isLoadingTicket"
            class="btn-primary"
          >
            {{
              isSaving
                ? "Guardando…"
                : isEdit
                  ? "Guardar cambios"
                  : "Crear ticket"
            }}
          </button>
        </div>
      </form>
    </section>
  `,
})
export class TicketFormComponent implements OnInit {
  readonly priorities: PrioridadTicket[] = ["BAJA", "MEDIA", "ALTA", "URGENTE"];
  readonly prioridadLabel = prioridadLabel;

  isSaving = false;
  isLoadingCatalogs = true;
  isLoadingTicket = false;
  submitted = false;
  categorias: Categoria[] = [];

  readonly form = inject(FormBuilder).nonNullable.group({
    titulo: [
      "",
      [Validators.required, Validators.minLength(5), Validators.maxLength(120)],
    ],
    descripcion: [
      "",
      [
        Validators.required,
        Validators.minLength(10),
        Validators.maxLength(2000),
      ],
    ],
    prioridad: [
      "MEDIA" as PrioridadTicket,
      [Validators.required, Validators.pattern(PRIORITY_PATTERN)],
    ],
    usuarioId: [
      null as number | null,
      [Validators.required, Validators.min(1)],
    ],
    categoriaId: [
      null as number | null,
      [Validators.required, Validators.min(1)],
    ],
    tecnicoId: [null as number | null, [Validators.min(1)]],
  });

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly ticketService = inject(TicketService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  readonly isEdit = Boolean(this.route.snapshot.paramMap.get("id"));

  ngOnInit(): void {
    if (!this.isEdit) {
      const userId = this.auth.userId();
      if (userId !== null) {
        this.form.controls.usuarioId.setValue(userId);
      }
    }

    this.categoriaService
      .listar()
      .pipe(
        finalize(() => {
          this.isLoadingCatalogs = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: (categorias) => {
          this.categorias = categorias;
          if (
            !this.isEdit &&
            this.form.controls.categoriaId.value === null &&
            categorias.length > 0
          ) {
            const defaultCategory =
              categorias.find(
                (categoria) => categoria.nombre.toLowerCase() === "software",
              ) ?? categorias[0];
            this.form.controls.categoriaId.setValue(defaultCategory.id);
          }
          this.changeDetector.markForCheck();
        },
        error: () => {
          this.changeDetector.markForCheck();
        },
      });

    if (this.isEdit) {
      const id = Number(this.route.snapshot.paramMap.get("id"));
      this.isLoadingTicket = true;
      this.ticketService
        .obtener(id)
        .pipe(
          finalize(() => {
            this.isLoadingTicket = false;
            this.changeDetector.markForCheck();
          }),
        )
        .subscribe({
          next: (ticket) => {
            this.form.patchValue(this.toFormValue(ticket));
            this.changeDetector.markForCheck();
          },
          error: () => {
            this.changeDetector.markForCheck();
          },
        });
    }
  }

  submit(): void {
    this.submitted = true;
    this.form.markAllAsTouched();
    if (this.form.invalid || this.isLoadingCatalogs || this.isSaving) {
      return;
    }
    this.isSaving = true;
    const id = Number(this.route.snapshot.paramMap.get("id"));
    const request = this.toRequest();
    const request$ = this.isEdit
      ? this.ticketService.actualizar(id, request)
      : this.ticketService.crear(request);
    request$
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: (ticket) =>
          this.router.navigate(["/tickets", ticket.id], {
            queryParams: { saved: "1" },
          }),
        error: () => {
          this.changeDetector.markForCheck();
        },
      });
  }

  showError(
    controlName: keyof typeof this.form.controls,
    errorName: string,
  ): boolean {
    const control = this.form.controls[controlName];
    return this.submitted && control.hasError(errorName);
  }

  private toRequest(): TicketRequest {
    const value = this.form.getRawValue();
    return {
      titulo: value.titulo.trim(),
      descripcion: value.descripcion.trim(),
      prioridad: value.prioridad,
      usuarioId: Number(value.usuarioId),
      categoriaId: Number(value.categoriaId),
      tecnicoId: value.tecnicoId ? Number(value.tecnicoId) : null,
    };
  }

  private toFormValue(ticket: Ticket) {
    return {
      titulo: ticket.titulo,
      descripcion: ticket.descripcion,
      prioridad: ticket.prioridad,
      usuarioId: ticket.usuarioId,
      categoriaId: ticket.categoriaId,
      tecnicoId: ticket.tecnicoId,
    };
  }
}
