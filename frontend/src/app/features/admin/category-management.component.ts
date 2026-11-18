import { ChangeDetectorRef, Component, OnInit, inject } from "@angular/core";
import {
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { finalize } from "rxjs";
import { CategoriaService } from "../../core/services/categoria.service";
import { ConfirmationService } from "../../core/services/confirmation.service";
import { NotificationService } from "../../core/services/notification.service";
import { Categoria } from "../../shared/models/categoria.model";
import { IconComponent } from "../../shared/ui/icon.component";

@Component({
  selector: "app-category-management",
  imports: [FormsModule, ReactiveFormsModule, IconComponent],
  template: `
    <section>
      <div class="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 class="text-2xl font-bold text-navy-900">Categorías</h1>
          <p class="mt-1 text-sm text-slate-500">
            Organiza las solicitudes por tipo de soporte.
          </p>
        </div>
        <button type="button" (click)="openCreate()" class="btn-primary">
          <app-icon name="plus" class="h-4 w-4" />
          Nueva categoría
        </button>
      </div>

      <div class="card mb-4 p-4">
        <div class="relative">
          <app-icon
            name="search"
            class="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400"
          />
          <input
            [(ngModel)]="searchTerm"
            class="input pl-9"
            placeholder="Buscar por nombre o descripción"
            aria-label="Buscar categorías"
          />
        </div>
      </div>

      <div class="card overflow-hidden">
        @if (isLoading) {
          <div class="flex justify-center py-16">
            <div
              class="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-navy-800"
            ></div>
          </div>
        } @else if (filteredCategorias.length === 0) {
          <div class="p-10 text-center">
            <p class="font-medium text-slate-700">
              No se encontraron categorías con esos criterios.
            </p>
            <p class="mt-2 text-sm text-slate-500">
              Prueba con otra búsqueda o crea una nueva categoría.
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
                  <th class="px-4 py-3 font-semibold">Categoría</th>
                  <th class="px-4 py-3 font-semibold">Descripción</th>
                  <th class="w-28 px-4 py-3 text-right font-semibold">
                    Acciones
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (categoria of filteredCategorias; track categoria.id) {
                  <tr class="group transition hover:bg-sky-50/60">
                    <td class="px-4 py-3 text-slate-400">
                      #{{ categoria.id }}
                    </td>
                    <td class="px-4 py-3">
                      <div class="flex items-center gap-3">
                        <span
                          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-sky-100 text-sky-700"
                        >
                          <app-icon name="category" class="h-4 w-4" />
                        </span>
                        <span class="font-medium text-navy-900">{{
                          categoria.nombre
                        }}</span>
                      </div>
                    </td>
                    <td class="px-4 py-3 text-slate-600">
                      {{ categoria.descripcion || "Sin descripción" }}
                    </td>
                    <td class="px-4 py-3">
                      <div class="flex justify-end gap-1">
                        <button
                          type="button"
                          (click)="openEdit(categoria)"
                          class="rounded-lg p-2 text-slate-500 transition hover:bg-sky-50 hover:text-sky-700"
                          [attr.aria-label]="
                            'Editar categoría ' + categoria.nombre
                          "
                          [title]="'Editar ' + categoria.nombre"
                        >
                          <app-icon name="edit" class="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          [disabled]="deletingId === categoria.id"
                          (click)="remove(categoria)"
                          class="rounded-lg p-2 text-slate-500 transition hover:bg-rose-50 hover:text-rose-600 disabled:cursor-not-allowed disabled:opacity-50"
                          [attr.aria-label]="
                            'Eliminar categoría ' + categoria.nombre
                          "
                          [title]="'Eliminar ' + categoria.nombre"
                        >
                          @if (deletingId === categoria.id) {
                            <span
                              class="block h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-rose-600"
                            ></span>
                          } @else {
                            <app-icon name="trash" class="h-4 w-4" />
                          }
                        </button>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <div class="divide-y divide-slate-100 md:hidden">
            @for (categoria of filteredCategorias; track categoria.id) {
              <div class="flex items-start gap-3 p-4">
                <span
                  class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-sky-100 text-sky-700"
                >
                  <app-icon name="category" class="h-4 w-4" />
                </span>
                <div class="min-w-0 flex-1">
                  <p class="text-xs text-slate-400">#{{ categoria.id }}</p>
                  <p class="mt-0.5 font-semibold text-navy-900">
                    {{ categoria.nombre }}
                  </p>
                  <p class="mt-1 text-xs text-slate-500">
                    {{ categoria.descripcion || "Sin descripción" }}
                  </p>
                  <div class="mt-3 flex gap-1">
                    <button
                      type="button"
                      (click)="openEdit(categoria)"
                      class="rounded-lg p-2 text-slate-500 transition hover:bg-sky-50 hover:text-sky-700"
                      [attr.aria-label]="'Editar categoría ' + categoria.nombre"
                      [title]="'Editar ' + categoria.nombre"
                    >
                      <app-icon name="edit" class="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      [disabled]="deletingId === categoria.id"
                      (click)="remove(categoria)"
                      class="rounded-lg p-2 text-slate-500 transition hover:bg-rose-50 hover:text-rose-600 disabled:cursor-not-allowed disabled:opacity-50"
                      [attr.aria-label]="
                        'Eliminar categoría ' + categoria.nombre
                      "
                      [title]="'Eliminar ' + categoria.nombre"
                    >
                      @if (deletingId === categoria.id) {
                        <span
                          class="block h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-rose-600"
                        ></span>
                      } @else {
                        <app-icon name="trash" class="h-4 w-4" />
                      }
                    </button>
                  </div>
                </div>
              </div>
            }
          </div>

          <div
            class="border-t border-slate-100 px-4 py-3 text-xs text-slate-500"
          >
            {{ filteredCategorias.length }} resultado(s)
          </div>
        }
      </div>
    </section>

    @if (showForm) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/60 p-4 backdrop-blur-sm"
        (click)="closeForm()"
      >
        <section
          class="card max-h-[90vh] w-full max-w-lg overflow-auto p-6 shadow-xl"
          role="dialog"
          aria-modal="true"
          aria-labelledby="category-dialog-title"
          (click)="$event.stopPropagation()"
        >
          <div class="mb-4 flex items-center justify-between">
            <h2
              id="category-dialog-title"
              class="text-lg font-semibold text-navy-900"
            >
              {{ editingId ? "Editar categoría" : "Registrar categoría" }}
            </h2>
            <button
              type="button"
              (click)="closeForm()"
              class="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100"
              aria-label="Cerrar"
            >
              <app-icon name="close" class="h-5 w-5" />
            </button>
          </div>

          <form
            class="space-y-4"
            [formGroup]="form"
            (ngSubmit)="save()"
            novalidate
          >
            <div>
              <label class="label" for="category-name">Nombre</label>
              <input
                id="category-name"
                formControlName="nombre"
                class="input"
              />
              @if (submitted && form.controls.nombre.hasError("required")) {
                <span class="mt-1 block text-xs text-rose-600"
                  >El nombre es obligatorio.</span
                >
              } @else if (
                submitted && form.controls.nombre.hasError("maxlength")
              ) {
                <span class="mt-1 block text-xs text-rose-600"
                  >El nombre no puede superar 100 caracteres.</span
                >
              }
            </div>
            <div>
              <label class="label" for="category-description"
                >Descripción</label
              >
              <textarea
                id="category-description"
                formControlName="descripcion"
                class="input min-h-28 resize-y"
              ></textarea>
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button type="button" (click)="closeForm()" class="btn-ghost">
                Cancelar
              </button>
              <button type="submit" [disabled]="isSaving" class="btn-primary">
                {{ isSaving ? "Guardando…" : "Guardar" }}
              </button>
            </div>
          </form>
        </section>
      </div>
    }
  `,
})
export class CategoryManagementComponent implements OnInit {
  categorias: Categoria[] = [];
  searchTerm = "";
  showForm = false;
  editingId: number | null = null;
  submitted = false;
  isLoading = false;
  isSaving = false;
  deletingId: number | null = null;

  readonly form = inject(FormBuilder).nonNullable.group({
    nombre: ["", [Validators.required, Validators.maxLength(100)]],
    descripcion: ["", [Validators.maxLength(500)]],
  });

  private readonly categoriaService = inject(CategoriaService);
  private readonly confirmations = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.load();
  }

  get filteredCategorias(): Categoria[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      return this.categorias;
    }
    return this.categorias.filter(
      (categoria) =>
        categoria.nombre.toLowerCase().includes(term) ||
        (categoria.descripcion ?? "").toLowerCase().includes(term),
    );
  }

  load(): void {
    this.isLoading = true;
    this.categoriaService
      .listar()
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.changeDetector.markForCheck();
        }),
      )
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

  openCreate(): void {
    this.editingId = null;
    this.form.reset({ nombre: "", descripcion: "" });
    this.submitted = false;
    this.showForm = true;
  }

  openEdit(categoria: Categoria): void {
    this.editingId = categoria.id;
    this.form.reset({
      nombre: categoria.nombre,
      descripcion: categoria.descripcion ?? "",
    });
    this.submitted = false;
    this.showForm = true;
  }

  closeForm(): void {
    if (!this.isSaving) {
      this.showForm = false;
    }
  }

  save(): void {
    this.submitted = true;
    this.form.markAllAsTouched();
    if (this.form.invalid || this.isSaving) {
      return;
    }
    this.isSaving = true;
    const request = this.form.getRawValue();
    const operation$ =
      this.editingId === null
        ? this.categoriaService.crear(request)
        : this.categoriaService.actualizar(this.editingId, request);
    operation$
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.showForm = false;
          this.notifications.success(
            "Categoría guardada",
            "Los cambios se aplicaron correctamente.",
          );
          this.load();
          this.changeDetector.markForCheck();
        },
        error: () => {
          this.changeDetector.markForCheck();
        },
      });
  }

  async remove(categoria: Categoria): Promise<void> {
    const confirmed = await this.confirmations.confirm({
      title: "Eliminar categoría",
      message: `¿Seguro que deseas eliminar la categoría “${categoria.nombre}”? Esta acción no se puede deshacer.`,
      confirmLabel: "Eliminar",
      cancelLabel: "Cancelar",
      tone: "danger",
    });
    if (!confirmed) {
      return;
    }
    this.deletingId = categoria.id;
    this.categoriaService
      .eliminar(categoria.id)
      .pipe(
        finalize(() => {
          this.deletingId = null;
          this.changeDetector.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.notifications.success(
            "Categoría eliminada",
            `${categoria.nombre} ya no está disponible.`,
          );
          this.load();
          this.changeDetector.markForCheck();
        },
        error: () => {
          this.changeDetector.markForCheck();
        },
      });
  }
}
