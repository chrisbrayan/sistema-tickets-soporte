import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { UsuarioService } from '../../core/services/usuario.service';
import { NotificationService } from '../../core/services/notification.service';
import { UserRole, Usuario } from '../../shared/models/usuario.model';
import { AvatarComponent } from '../../shared/ui/avatar.component';
import { IconComponent } from '../../shared/ui/icon.component';

@Component({
  selector: 'app-user-management',
  imports: [ReactiveFormsModule, AvatarComponent, IconComponent],
  template: `
    <section>
      <div class="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 class="text-2xl font-bold text-navy-900">Usuarios y roles</h1>
          <p class="mt-1 text-sm text-slate-500">Registra usuarios y asígnales un rol.</p>
        </div>
        <button type="button" (click)="openForm()" class="btn-primary">
          <app-icon name="plus" class="h-4 w-4" />
          Nuevo usuario
        </button>
      </div>

      @if (isLoading) {
        <div class="mb-4 h-1 overflow-hidden rounded-full bg-blue-100"><div class="h-full w-1/2 animate-pulse rounded-full bg-sky-brand"></div></div>
      }

      <div class="card overflow-hidden">
        @if (!isLoading && usuarios.length === 0) {
          <div class="p-8 text-center text-slate-500">No hay usuarios registrados.</div>
        } @else {
          <div class="hidden overflow-x-auto md:block">
            <table class="w-full min-w-[560px] text-sm">
              <thead class="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th class="px-5 py-3 font-semibold">Usuario</th>
                  <th class="px-5 py-3 font-semibold">Correo</th>
                  <th class="px-5 py-3 font-semibold">Rol</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (usuario of usuarios; track usuario.id) {
                  <tr class="transition hover:bg-slate-50/60">
                    <td class="px-5 py-3">
                      <div class="flex items-center gap-3">
                        <app-avatar [name]="usuario.nombre" size="sm" />
                        <span class="font-medium text-navy-900">{{ usuario.nombre }}</span>
                      </div>
                    </td>
                    <td class="whitespace-nowrap px-5 py-3 text-slate-600">{{ usuario.email }}</td>
                    <td class="px-5 py-3">
                      <span class="rounded-full px-2.5 py-0.5 text-xs font-semibold" [class]="roleClass(usuario.rol)">{{ usuario.rol }}</span>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <div class="divide-y divide-slate-100 md:hidden">
            @for (usuario of usuarios; track usuario.id) {
              <div class="flex items-center gap-3 p-4">
                <app-avatar [name]="usuario.nombre" size="sm" />
                <div class="min-w-0 flex-1">
                  <p class="truncate font-medium text-navy-900">{{ usuario.nombre }}</p>
                  <p class="truncate text-xs text-slate-500">{{ usuario.email }}</p>
                </div>
                <span class="shrink-0 rounded-full px-2.5 py-0.5 text-xs font-semibold" [class]="roleClass(usuario.rol)">{{ usuario.rol }}</span>
              </div>
            }
          </div>
        }
      </div>
    </section>

    @if (showForm) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/60 p-4 backdrop-blur-sm" (click)="closeForm()">
        <section class="card max-h-[90vh] w-full max-w-lg overflow-auto p-6 shadow-xl" role="dialog" aria-modal="true" aria-labelledby="user-dialog-title" (click)="$event.stopPropagation()">
          <div class="mb-4 flex items-center justify-between">
            <h2 id="user-dialog-title" class="text-lg font-semibold text-navy-900">Registrar usuario</h2>
            <button type="button" (click)="closeForm()" class="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100" aria-label="Cerrar">
              <app-icon name="close" class="h-5 w-5" />
            </button>
          </div>

          <form class="space-y-4" [formGroup]="form" (ngSubmit)="save()" novalidate>
            <div>
              <label class="label" for="user-name">Nombre completo</label>
              <input id="user-name" formControlName="nombre" class="input" />
              @if (submitted && form.controls.nombre.hasError('required')) {
                <span class="mt-1 block text-xs text-rose-600">El nombre es obligatorio.</span>
              } @else if (submitted && form.controls.nombre.hasError('minlength')) {
                <span class="mt-1 block text-xs text-rose-600">El nombre debe tener al menos 2 caracteres.</span>
              }
            </div>
            <div>
              <label class="label" for="user-email">Correo</label>
              <input id="user-email" formControlName="email" type="email" class="input" />
              @if (submitted && form.controls.email.hasError('required')) {
                <span class="mt-1 block text-xs text-rose-600">El correo es obligatorio.</span>
              } @else if (submitted && form.controls.email.hasError('email')) {
                <span class="mt-1 block text-xs text-rose-600">Ingresa un correo válido.</span>
              }
            </div>
            <div>
              <label class="label" for="user-password">Contraseña temporal</label>
              <input id="user-password" formControlName="password" type="password" class="input" />
              @if (submitted && form.controls.password.hasError('required')) {
                <span class="mt-1 block text-xs text-rose-600">La contraseña es obligatoria.</span>
              } @else if (submitted && form.controls.password.hasError('minlength')) {
                <span class="mt-1 block text-xs text-rose-600">Debe tener al menos 8 caracteres.</span>
              }
            </div>
            <div>
              <label class="label" for="user-role">Rol</label>
              <select id="user-role" formControlName="rol" class="input">
                @for (role of roles; track role) {
                  <option [value]="role">{{ role }}</option>
                }
              </select>
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button type="button" (click)="closeForm()" class="btn-ghost">Cancelar</button>
              <button type="submit" [disabled]="isSaving" class="btn-primary">{{ isSaving ? 'Registrando…' : 'Registrar' }}</button>
            </div>
          </form>
        </section>
      </div>
    }
  `
})
export class UserManagementComponent implements OnInit {
  readonly roles: UserRole[] = ['USUARIO', 'TECNICO', 'ADMIN'];
  usuarios: Usuario[] = [];
  showForm = false;
  submitted = false;
  isLoading = false;
  isSaving = false;

  readonly form = inject(FormBuilder).nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    rol: ['USUARIO' as UserRole, [Validators.required]]
  });

  private readonly usuarioService = inject(UsuarioService);
  private readonly notifications = inject(NotificationService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.usuarioService.listarTodos().pipe(finalize(() => {
      this.isLoading = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: usuarios => {
        this.usuarios = usuarios;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.changeDetector.markForCheck();
      }
    });
  }

  openForm(): void {
    this.form.reset({ nombre: '', email: '', password: '', rol: 'USUARIO' });
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
    this.usuarioService.crear(this.form.getRawValue()).pipe(finalize(() => {
      this.isSaving = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: usuario => {
        this.usuarios = [...this.usuarios, usuario];
        this.showForm = false;
        this.notifications.success('Usuario registrado', `${usuario.nombre} ya puede iniciar sesión.`);
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.changeDetector.markForCheck();
      }
    });
  }

  roleClass(role: UserRole): string {
    return role === 'ADMIN'
      ? 'bg-navy-900 text-white'
      : role === 'TECNICO'
        ? 'bg-sky-100 text-sky-700'
        : 'bg-slate-100 text-slate-600';
  }
}
