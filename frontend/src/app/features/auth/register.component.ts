import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { toApiError } from '../../core/http/api-error.util';
import { IconComponent } from '../../shared/ui/icon.component';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, IconComponent],
  template: `
    <main class="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-12">
      <section class="w-full max-w-md">
        <div class="mb-8 flex items-center justify-center gap-3">
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-navy-900 text-white">
            <app-icon name="ticket" class="h-5 w-5" />
          </div>
          <div class="leading-tight">
            <p class="font-semibold text-navy-900">Soporte TI</p>
            <p class="text-xs text-slate-500">Gestión de tickets</p>
          </div>
        </div>

        <div class="card p-7">
          <a routerLink="/login" class="inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-navy-900">
            <app-icon name="back" class="h-4 w-4" />
            Volver al inicio de sesión
          </a>
          <h1 class="mt-5 text-2xl font-bold text-navy-900">Crear cuenta</h1>
          <p class="mt-1 text-sm text-slate-500">Regístrate para crear y consultar tus solicitudes de soporte.</p>

          <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <div>
              <label class="label" for="register-name">Nombre completo</label>
              <input id="register-name" formControlName="nombre" class="input" placeholder="Tu nombre" autocomplete="name" />
              @if (submitted && form.controls.nombre.hasError('required')) {
                <span class="mt-1 block text-xs text-rose-600">El nombre es obligatorio.</span>
              } @else if (submitted && form.controls.nombre.hasError('minlength')) {
                <span class="mt-1 block text-xs text-rose-600">El nombre debe tener al menos 2 caracteres.</span>
              }
            </div>
            <div>
              <label class="label" for="register-email">Correo</label>
              <input id="register-email" formControlName="email" type="email" class="input" placeholder="nombre@empresa.com" autocomplete="email" />
              @if (submitted && form.controls.email.hasError('required')) {
                <span class="mt-1 block text-xs text-rose-600">El correo es obligatorio.</span>
              } @else if (submitted && form.controls.email.hasError('email')) {
                <span class="mt-1 block text-xs text-rose-600">Ingresa un correo válido.</span>
              }
            </div>
            <div>
              <label class="label" for="register-password">Contraseña</label>
              <input id="register-password" formControlName="password" type="password" class="input" placeholder="Mínimo 8 caracteres" autocomplete="new-password" />
              @if (submitted && form.controls.password.hasError('required')) {
                <span class="mt-1 block text-xs text-rose-600">La contraseña es obligatoria.</span>
              } @else if (submitted && form.controls.password.hasError('minlength')) {
                <span class="mt-1 block text-xs text-rose-600">Debe tener al menos 8 caracteres.</span>
              }
            </div>

            <button type="submit" [disabled]="isSaving" class="btn-primary w-full py-2.5">
              {{ isSaving ? 'Registrando…' : 'Registrarme' }}
            </button>
          </form>
        </div>
      </section>
    </main>
  `
})
export class RegisterComponent {
  readonly form = inject(FormBuilder).nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });
  submitted = false;
  isSaving = false;

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  submit(): void {
    this.submitted = true;
    this.form.markAllAsTouched();
    if (this.form.invalid || this.isSaving) {
      return;
    }
    this.isSaving = true;
    this.auth.registrar(this.form.getRawValue()).pipe(finalize(() => {
      this.isSaving = false;
      this.changeDetector.markForCheck();
    })).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { registered: '1' } }),
      error: error => {
        this.notifications.error(
          'No se pudo crear la cuenta',
          toApiError(error).message || 'Revisa los datos e inténtalo nuevamente.');
        this.changeDetector.markForCheck();
      }
    });
  }
}
