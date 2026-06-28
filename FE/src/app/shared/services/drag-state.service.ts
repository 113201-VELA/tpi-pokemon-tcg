import { Injectable, signal } from '@angular/core';

export type DragSourceType = 'card' | 'pokemon';

export interface DragState {
  type: DragSourceType;
  cardId: string;
  instanceId?: string;
}

@Injectable({ providedIn: 'root' })
export class DragStateService {
  readonly current = signal<DragState | null>(null);

  readonly draggedCardId = signal<string | null>(null);

  readonly draggedInstanceId = signal<string | null>(null);

  startCardDrag(cardId: string): void {
    this.current.set({ type: 'card', cardId });
    this.draggedCardId.set(cardId);
    this.draggedInstanceId.set(null);
  }

  startPokemonDrag(cardId: string, instanceId: string): void {
    this.current.set({ type: 'pokemon', cardId, instanceId });
    this.draggedCardId.set(cardId);
    this.draggedInstanceId.set(instanceId);
  }

  endDrag(): void {
    this.current.set(null);
    this.draggedCardId.set(null);
    this.draggedInstanceId.set(null);
  }
}
