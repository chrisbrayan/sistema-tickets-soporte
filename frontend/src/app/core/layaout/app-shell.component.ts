import { Component, inject } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { AvatarComponent } from '../../shared/ui/avatar.component';
import { IconComponent } from '../../shared/ui/icon.component';

@Component({
  selector: 'app-shell',
  imports: [NgClass, RouterLink, RouterLinkActive, RouterOutlet, AvatarComponent, IconComponent],
  template: `
    <div class="min-h-screen bg-slate-100 text-slate-800">
      @if (sidebarOpen) {
        <div class="fixed inset-0 z-30 bg-navy-950/50 lg:hidden" (click)="sidebarOpen = false" aria-hidden="true"></div>
      }

      <aside
        class="fixed inset-y-0 left-0 z-40 flex w-72 flex-col bg-navy-900 text-slate-300 transition-transform duration-200 lg:translate-x-0"
        [ngClass]="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
        aria-label="Navegación lateral">
        <div class="flex items-center gap-3 px-6 py-5">
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-sky-brand text-white">
            <app-icon name="ticket" class="h-5 w-5" />
          </div>
          <div class="leading-tight">
            <p class="font-semibold text-white">Soporte TI</p>
            <p class="text-xs text-slate-400">Gestión de tickets</p>
          </div>
        </div>

        <nav class="mt-2 flex-1 space-y-1 px-3" aria-label="Navegación principal">
          <a routerLink="/dashboard" routerLinkActive="bg-white/10 text-white"
             [routerLinkActiveOptions]="{ exact: true }"
             (click)="sidebarOpen = false"
             class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/5 hover:text-white">
            <app-icon name="dashboard" class="h-5 w-5" />
            Dashboard
          </a>

          @if (auth.role() === 'TECNICO') {
            <a routerLink="/tickets/mis-asignados" routerLinkActive="bg-white/10 text-white"
               (click)="sidebarOpen = false"
               class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/5 hover:text-white">
              <app-icon name="ticket" class="h-5 w-5" />
              Mis asignados
            </a>
          } @else {
            <a routerLink="/tickets" routerLinkActive="bg-white/10 text-white"
               [routerLinkActiveOptions]="{ exact: true }"
               (click)="sidebarOpen = false"
               class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/5 hover:text-white">
              <app-icon name="ticket" class="h-5 w-5" />
              {{ auth.role() === 'USUARIO' ? 'Mis tickets' : 'Todos los tickets' }}
            </a>
          }

          @if (auth.role() !== 'TECNICO') {
            <a routerLink="/tickets/nuevo" routerLinkActive="bg-white/10 text-white"
               (click)="sidebarOpen = false"
               class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/5 hover:text-white">
              <app-icon name="plus" class="h-5 w-5" />
              Nuevo ticket
            </a>
          }

          @if (auth.role() === 'ADMIN') {
            <a routerLink="/admin/usuarios" routerLinkActive="bg-white/10 text-white"
               (click)="sidebarOpen = false"
               class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/5 hover:text-white">
              <app-icon name="users" class="h-5 w-5" />
              Usuarios y roles
            </a>
            <a routerLink="/admin/categorias" routerLinkActive="bg-white/10 text-white"
               (click)="sidebarOpen = false"
               class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/5 hover:text-white">
              <app-icon name="category" class="h-5 w-5" />
              Categorías
            </a>
          }
        </nav>

        <div class="border-t border-white/10 p-4">
          <button type="button" (click)="logout()"
                  class="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-slate-300 transition hover:bg-white/5 hover:text-white">
            <app-icon name="logout" class="h-5 w-5" />
            Cerrar sesión
          </button>
        </div>
      </aside>

      <div class="flex min-h-screen min-w-0 flex-col lg:pl-72">
        <header class="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 sm:px-8">
          <div class="flex min-w-0 items-center gap-3">
            <button type="button" (click)="sidebarOpen = true"
                    class="rounded-lg p-2 text-slate-500 transition hover:bg-slate-100 lg:hidden"
                    aria-label="Abrir menú">
              <app-icon name="menu" class="h-5 w-5" />
            </button>
            <span class="truncate text-sm font-medium text-slate-500">{{ pageTitle }}</span>
          </div>
          <div class="flex items-center gap-3">
            <div class="hidden text-right leading-tight sm:block">
              <p class="text-sm font-semibold text-navy-900">{{ displayName }}</p>
              <p class="text-xs uppercase text-slate-500">{{ auth.role() }}</p>
            </div>
            <app-avatar [name]="displayName" />
          </div>
        </header>

        <main class="mx-auto w-full max-w-7xl flex-1 p-4 sm:p-8">
          <router-outlet />
        </main>
      </div>
    </div>
  `
})
export class AppShellComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  sidebarOpen = false;

  get displayName(): string {
    return this.auth.currentLogin()?.userName ?? this.auth.subject()?.split('@')[0] ?? 'Usuario';
  }

  get pageTitle(): string {
    const url = this.router.url.split('?')[0];
    const role = this.auth.role();

    if (url.startsWith('/dashboard')) {
      return 'Dashboard';
    }
    if (url.startsWith('/admin/usuarios')) {
      return 'Usuarios y roles';
    }
    if (url.startsWith('/admin/categorias')) {
      return 'Categorías';
    }
    if (url.startsWith('/tickets/nuevo')) {
      return 'Nuevo ticket';
    }
    if (url.includes('/editar')) {
      return 'Editar ticket';
    }
    if (url.startsWith('/tickets/mis-asignados') || (role === 'TECNICO' && url.startsWith('/tickets'))) {
      return 'Mis asignados';
    }
    if (url.startsWith('/tickets')) {
      return role === 'ADMIN' ? 'Todos los tickets' : 'Mis tickets';
    }
    return 'Dashboard';
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
