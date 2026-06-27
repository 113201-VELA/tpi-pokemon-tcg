import {
  Directive,
  ElementRef,
  input,
  OnInit,
} from '@angular/core';

/**
 * Makes any element draggable and stores the card ID in the drag event.
 * Use on each card image in the hand during setup and main phase.
 *
 * Usage:
 *   <img appDraggableCard [cardId]="card.id" ... />
 */
@Directive({
  selector: '[appDraggableCard]',
  host: {
    '(dragstart)': 'onDragStart($event)',
    '(dragend)': 'onDragEnd()',
  },
})
export class DraggableCardDirective implements OnInit {
  /** The card ID to transfer during drag. */
  readonly cardId = input.required<string>();

  /** Key used in DataTransfer to identify dragged card data. */
  static readonly TRANSFER_KEY = 'application/pokemon-card-id';

  constructor(private readonly el: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    this.el.nativeElement.setAttribute('draggable', 'true');
  }

  onDragStart(event: DragEvent): void {
    if (!event.dataTransfer) return;
    event.dataTransfer.setData(DraggableCardDirective.TRANSFER_KEY, this.cardId());
    event.dataTransfer.effectAllowed = 'move';
    this.el.nativeElement.style.opacity = '0.5';
  }

  onDragEnd(): void {
    this.el.nativeElement.style.opacity = '';
  }
}
