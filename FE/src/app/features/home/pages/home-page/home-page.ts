import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../auth/data-access/services/auth.service';

@Component({
  selector: 'app-home-page',
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomePage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  logout(): void {
    this.authService.logout();
  }

  goToPrefs(): void {
    this.router.navigate(['/preferences']);
  }
}
