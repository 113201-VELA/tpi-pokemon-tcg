import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../data-access/services/auth.service';

@Component({
  selector: 'app-register-page',
  templateUrl: './register-page.html',
  styleUrl: './register-page.css',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  onSubmit(): void {
    if (this.form.invalid || this.loading()) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const { username, email, password } = this.form.value;

    this.authService.register({
      username: username!,
      email: email!,
      password: password!
    }).subscribe({
      next: () => this.router.navigate(['/decks']),
      error: (err) => {
        this.errorMessage.set(err.error?.message ?? 'Error al registrarse. Intentá de nuevo.');
        this.loading.set(false);
      }
    });
  }
}
