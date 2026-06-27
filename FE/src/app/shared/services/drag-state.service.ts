import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DragStateService {
  readonly draggedCardId = signal<string | null>(null);

  startDrag(cardId: string): void {
    this.draggedCardId.set(cardId);
  }

  endDrag(): void {
    this.draggedCardId.set(null);
  }
}
