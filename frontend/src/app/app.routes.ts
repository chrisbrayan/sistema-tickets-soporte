import { Routes } from '@angular/router';
import { AppShellComponent } from './core/layout/app-shell.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { UserManagementComponent } from './features/admin/user-management.component';
import { CategoryManagementComponent } from './features/admin/category-management.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { TicketDetailComponent } from './features/tickets/ticket-detail.component';
import { TicketFormComponent } from './features/tickets/ticket-form.component';
import { TicketListComponent } from './features/tickets/ticket-list.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    title: 'Iniciar sesión | Mesa de soporte'
  },
  {
    path: 'registro',
    component: RegisterComponent,
    title: 'Crear cuenta | Mesa de soporte'
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardComponent, title: 'Dashboard | Mesa de soporte' },
      { path: 'tickets', component: TicketListComponent, title: 'Tickets | Mesa de soporte' },
      { path: 'tickets/mis-asignados', component: TicketListComponent, data: { assignedOnly: true }, title: 'Tickets asignados | Mesa de soporte' },
      {
        path: 'tickets/nuevo',
        component: TicketFormComponent,
        canActivate: [roleGuard],
        data: { roles: ['USUARIO', 'ADMIN'] },
        title: 'Nuevo ticket | Mesa de soporte'
      },
      { path: 'admin/usuarios', component: UserManagementComponent, canActivate: [roleGuard], data: { roles: ['ADMIN'] }, title: 'Usuarios y roles | Mesa de soporte' },
      { path: 'admin/categorias', component: CategoryManagementComponent, canActivate: [roleGuard], data: { roles: ['ADMIN'] }, title: 'Categorías | Mesa de soporte' },
      { path: 'tickets/:id', component: TicketDetailComponent, title: 'Detalle del ticket | Mesa de soporte' },
      {
        path: 'tickets/:id/editar',
        component: TicketFormComponent,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        title: 'Editar ticket | Mesa de soporte'
      }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
