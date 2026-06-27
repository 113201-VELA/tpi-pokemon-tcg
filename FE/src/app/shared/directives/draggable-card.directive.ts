import {
  Directive,
  ElementRef,
  inject,
  input,
  OnInit,
} from '@angular/core';
import { DragStateService } from '../services/drag-state.service';

@Directive({
  selector: '[appDraggableCard]',
  host: {
    '(dragstart)': 'onDragStart($event)',
    '(dragend)': 'onDragEnd()',
  },
})
export class DraggableCardDirective implements OnInit {
  readonly cardId = input.required<string>();

  static readonly TRANSFER_KEY = 'application/pokemon-card-id';

  private readonly dragState = inject(DragStateService);

  constructor(private readonly el: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    this.el.nativeElement.setAttribute('draggable', 'true');
  }

  onDragStart(event: DragEvent): void {
    if (!event.dataTransfer) return;
    event.dataTransfer.setData(DraggableCardDirective.TRANSFER_KEY, this.cardId());
    event.dataTransfer.effectAllowed = 'move';
    this.el.nativeElement.style.opacity = '0.5';
    this.dragState.startDrag(this.cardId());
  }

  onDragEnd(): void {
    this.el.nativeElement.style.opacity = '';
    this.dragState.endDrag();
  }
}
