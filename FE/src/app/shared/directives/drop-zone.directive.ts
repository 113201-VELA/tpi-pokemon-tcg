import {
  Directive,
  ElementRef,
  input,
  output,
} from '@angular/core';
import { DraggableCardDirective } from './draggable-card.directive';

/**
 * Marks an element as a valid drop target for draggable cards.
 * Emits the dropped card ID via (cardDropped).
 *
 * Usage:
 *   <div appDropZone [acceptDrop]="true" (cardDropped)="onDrop($event)"></div>
 */
@Directive({
  selector: '[appDropZone]',
  host: {
    '(dragover)': 'onDragOver($event)',
    '(dragleave)': 'onDragLeave()',
    '(drop)': 'onDrop($event)',
  },
})
export class DropZoneDirective {
  /**
   * When false, the drop zone ignores drag events.
   * Use to disable slots that are already occupied or not valid targets.
   */
  readonly acceptDrop = input<boolean>(true);

  /** Emits the card ID of the card dropped onto this zone. */
  readonly cardDropped = output<string>();

  /**
   * Emits when a Pokémon in play (bench) is dropped onto this zone.
   * Contains both the card ID and the runtime instance ID.
   */
  readonly pokemonDropped = output<{ cardId: string; instanceId: string }>();

  constructor(private readonly el: ElementRef<HTMLElement>) {}

  onDragOver(event: DragEvent): void {
    if (!this.acceptDrop()) return;
    event.preventDefault();
    event.dataTransfer!.dropEffect = 'move';
    this.el.nativeElement.classList.add('drop-zone--active');
  }

  onDragLeave(): void {
    this.el.nativeElement.classList.remove('drop-zone--active');
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.el.nativeElement.classList.remove('drop-zone--active');
    if (!this.acceptDrop()) return;

    const cardId = event.dataTransfer?.getData(
      DraggableCardDirective.TRANSFER_KEY
    ) || event.dataTransfer?.getData('application/pokemon-card-id');

    const instanceId = event.dataTransfer?.getData(
      'application/pokemon-instance-id'
    );

    if (instanceId && cardId) {
      this.pokemonDropped.emit({ cardId, instanceId });
    } else if (cardId) {
      this.cardDropped.emit(cardId);
    }
  }
}
