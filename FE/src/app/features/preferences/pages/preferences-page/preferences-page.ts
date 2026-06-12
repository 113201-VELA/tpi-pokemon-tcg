import {
  ChangeDetectionStrategy, Component, inject, signal
} from '@angular/core';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../auth/data-access/services/auth.service';
import { PlayerService } from '../../data-access/services/player.service';

@Component({
  selector: 'app-preferences-page',
  templateUrl: './preferences-page.html',
  styleUrl: './preferences-page.css',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PreferencesPage {
  private readonly router        = inject(Router);
  private readonly authService   = inject(AuthService);
  private readonly playerService = inject(PlayerService);
  private readonly fb            = inject(FormBuilder);

  readonly currentUser = this.authService.currentUser;

  // ── Nickname form ────────────────────────────────────────────────────
  readonly nicknameForm = this.fb.group({
    nickname: [
      this.currentUser()?.nickname ?? '',
      [Validators.required, Validators.maxLength(30)]
    ]
  });

  // ── Password form ────────────────────────────────────────────────────
  readonly passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword:     ['', [Validators.required, Validators.minLength(8)]]
  });

  // ── State ────────────────────────────────────────────────────────────
  readonly savingNickname  = signal(false);
  readonly savingPassword  = signal(false);
  readonly nicknameSuccess = signal<string | null>(null);
  readonly nicknameError   = signal<string | null>(null);
  readonly passwordSuccess = signal<string | null>(null);
  readonly passwordError   = signal<string | null>(null);

  // ── Actions ──────────────────────────────────────────────────────────
  saveNickname(): void {
    if (this.nicknameForm.invalid) return;

    const nickname = this.nicknameForm.value.nickname!.trim();
    if (nickname === this.currentUser()?.nickname) return;

    this.savingNickname.set(true);
    this.nicknameSuccess.set(null);
    this.nicknameError.set(null);

    this.playerService.updateNickname({ nickname }).subscribe({
      next: () => {
        this.authService.updateCurrentUserNickname(nickname);
        this.savingNickname.set(false);
        this.nicknameSuccess.set('Nickname actualizado correctamente.');
      },
      error: (err: HttpErrorResponse) => {
        this.savingNickname.set(false);
        this.nicknameError.set(
          err.error?.message ?? 'No se pudo actualizar el nickname.'
        );
      }
    });
  }

  savePassword(): void {
    if (this.passwordForm.invalid) return;

    this.savingPassword.set(true);
    this.passwordSuccess.set(null);
    this.passwordError.set(null);

    this.playerService.updatePassword({
      currentPassword: this.passwordForm.value.currentPassword!,
      newPassword:     this.passwordForm.value.newPassword!
    }).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.passwordSuccess.set('Contraseña actualizada correctamente.');
        this.passwordForm.reset();
      },
      error: (err: HttpErrorResponse) => {
        this.savingPassword.set(false);
        this.passwordError.set(
          err.error?.message ?? 'No se pudo actualizar la contraseña.'
        );
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/home']);
  }
}
