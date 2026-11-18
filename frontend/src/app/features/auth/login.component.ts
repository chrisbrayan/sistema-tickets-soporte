import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { toApiError } from '../../core/http/api-error.util';
import { IconComponent } from '../../shared/ui/icon.component';

interface DemoAccount {
  role: string;
  email: string;
  password: string;
}

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, IconComponent],
  template: `
    <main class="grid min-h-screen bg-slate-100 lg:grid-cols-2">
      <section class="relative hidden flex-col justify-between overflow-hidden bg-navy-900 p-12 text-white lg:flex">
        <div class="pointer-events-none absolute -right-24 -top-24 h-96 w-96 rounded-full bg-sky-brand/20 blur-3xl"></div>
        <div class="pointer-events-none absolute -bottom-32 -left-16 h-96 w-96 rounded-full bg-navy-700 blur-3xl"></div>

        <div class="relative flex items-center gap-3">
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-sky-brand text-white">
            <app-icon name="ticket" class="h-5 w-5" />
          </div>
          <span class="text-lg font-semibold">Soporte TI</span>
        </div>

        <div class="relative max-w-xl">
          <h1 class="text-4xl font-bold leading-tight">Gestiona cada solicitud de soporte en un solo lugar.</h1>
          <p class="mt-5 text-base leading-7 text-slate-300">Registra, asigna y da seguimiento a los tickets técnicos de toda la organización.</p>
        </div>

        <p class="relative text-sm text-slate-400">Sistema interno de Gestión de Tickets de Soporte Técnico</p>
      </section>

      <section class="flex items-center justify-center px-6 py-12">
        <div class="w-full max-w-sm">
          <h2 class="text-2xl font-bold text-navy-900">Iniciar sesión</h2>
          <p class="mt-1 text-sm text-slate-500">Ingresa con tu correo corporativo.</p>

          <form class="mt-8 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
            <div>
              <label class="label" for="login-email">Correo</label>
              <input id="login-email" formControlName="email" type="email" class="input" placeholder="nombre@empresa.com" autocomplete="email" />
              @if (submitted && form.controls.email.hasError('required')) {
                <span class="mt-1 block text-xs text-rose-600">El correo es obligatorio.</span>
              } @else if (submitted && form.controls.email.hasError('email')) {
                <span class="mt-1 block text-xs text-rose-600">Ingresa un correo válido.</span>
              }
            </div>

            <div>
              <label class="label" for="login-password">Contraseña</label>
              <input id="login-password" formControlName="password" type="password" class="input" placeholder="••••••••" autocomplete="current-password" />
              @if (submitted && form.controls.password.hasError('required')) {
                <span class="mt-1 block text-xs text-rose-600">La contraseña es obligatoria.</span>
              }
            </div>

            @if (isLoading) {
              <div class="h-1 overflow-hidden rounded-full bg-blue-100"><div class="h-full w-1/2 animate-pulse rounded-full bg-sky-brand"></div></div>
            }

            <button type="submit" [disabled]="isLoading" class="btn-primary w-full py-2.5">
              {{ isLoading ? 'Ingresando…' : 'Ingresar' }}
            </button>
          </form>

          <div class="mt-8 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <p class="label">Cuentas de demostración</p>
            <div class="mt-1 space-y-1">
              @for (account of demoAccounts; track account.email) {
                <button type="button" (click)="useDemo(account)"
                        class="flex w-full items-center justify-between gap-3 rounded-lg px-2 py-1.5 text-left text-sm transition hover:bg-slate-50">
                  <span class="font-medium text-navy-800">{{ account.role }}</span>
                  <span class="truncate text-xs text-slate-500">{{ account.email }}</span>
                </button>
              }
            </div>
          </div>
        </div>
      </section>
    </main>
  `
})
export class LoginComponent implements OnInit {
  readonly demoAccounts: DemoAccount[] = [
    { role: 'Administrador', email: 'admin@soporte.com', password: 'admin123' },
    { role: 'Técnico', email: 'tecnico@soporte.com', password: 'tecnico123' },
    { role: 'Usuario', email: 'usuario@soporte.com', password: 'usuario123' }
  ];

  isLoading = false;
  submitted = false;

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('registered') === '1') {
      this.notifications.success('Cuenta creada', 'Ya puedes iniciar sesión con tus credenciales.');
    }
  }

  useDemo(account: DemoAccount): void {
    this.form.setValue({ email: account.email, password: account.password });
  }

  submit(): void {
    this.submitted = true;
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    this.isLoading = true;
    const credentials = this.form.getRawValue();
    this.auth.login({ email: credentials.email.trim(), password: credentials.password })
      .pipe(
        catchError(error => {
          this.notifications.error(
            'Credenciales inválidas',
            toApiError(error).message || 'Revisa tu correo y contraseña e inténtalo nuevamente.');
          this.changeDetector.markForCheck();
          return of(null);
        }),
        finalize(() => {
          this.isLoading = false;
          this.changeDetector.markForCheck();
        })
      )
      .subscribe(response => {
        if (!response) {
          return;
        }
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
        this.router.navigateByUrl(returnUrl);
      });
  }
}
