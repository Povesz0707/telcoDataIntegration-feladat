import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { AuthService } from '../../services/auth.service';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonModule, CardModule, InputTextModule, MessageModule, PasswordModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  readonly i18n = inject(LanguageService);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl('/home'),
      error: error => {
        this.error.set(error.status === 401
          ? this.i18n.t('auth.invalidCredentials')
          : this.i18n.t('errors.server'));
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }
}
