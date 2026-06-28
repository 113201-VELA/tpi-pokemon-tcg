import {
  Directive,
  ElementRef,
  inject,
  input,
  OnInit,
} from '@angular/core';
import { DragStateService } from '../services/drag-state.service';

@Directive({
  selector: '[appDraggablePokemon]',
  host: {
    '(dragstart)': 'onDragStart($event)',
    '(dragend)':   'onDragEnd()',
  },
})
export class DraggablePokemonDirective implements OnInit {
  readonly cardId = input.required<string>();
  readonly instanceId = input.required<string>();

  static readonly CARD_KEY     = 'application/pokemon-card-id';
  static readonly INSTANCE_KEY = 'application/pokemon-instance-id';

  private readonly dragState = inject(DragStateService);

  constructor(private readonly el: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    this.el.nativeElement.setAttribute('draggable', 'true');
  }

  onDragStart(event: DragEvent): void {
    if (!event.dataTransfer) return;
    event.dataTransfer.setData(DraggablePokemonDirective.CARD_KEY, this.cardId());
    event.dataTransfer.setData(DraggablePokemonDirective.INSTANCE_KEY, this.instanceId());
    event.dataTransfer.effectAllowed = 'move';
    this.el.nativeElement.style.opacity = '0.5';
    this.dragState.startPokemonDrag(this.cardId(), this.instanceId());
  }

  onDragEnd(): void {
    this.el.nativeElement.style.opacity = '';
    this.dragState.endDrag();
  }
}
