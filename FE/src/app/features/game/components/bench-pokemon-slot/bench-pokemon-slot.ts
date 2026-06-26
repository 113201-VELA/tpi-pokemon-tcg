import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import {
  BenchPokemonDTO,
  getHpClass,
  getHpPercent,
  groupEnergies,
} from '../../domain/models/game.models';
import { CardResponse } from '../../../deck-builder/domain/models/card.models';

@Component({
  selector: 'app-bench-pokemon-slot',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="bench-slot-inner" (click)="cardClick.emit(pokemon().card)">
      @if (pokemon().card) {
        <img
          class="card-img-bench"
          [src]="pokemon().card!.imageSmall"
          [alt]="pokemon().card!.name"
        />
      }
      <div class="hp-bar-wrap">
        <div
          class="hp-bar"
          [class]="hpClass()"
          [style.width.%]="hpPercent()"
        ></div>
      </div>
      @if (pokemon().attachedEnergies?.length) {
        <div class="energies-row">
          @for (group of energyGroups(); track group.type) {
            <span class="energy-chip-sm">{{ group.icon }}{{ group.count }}</span>
          }
        </div>
      }
    </div>
  `,
  styleUrl: './bench-pokemon-slot.css',
})
export class BenchPokemonSlot {
  readonly pokemon   = input.required<BenchPokemonDTO>();
  readonly cardClick = output<CardResponse | null>();

  hpPercent(): number {
    return getHpPercent(this.pokemon().currentHp, this.pokemon().maxHp);
  }

  hpClass(): string {
    return getHpClass(this.hpPercent());
  }

  energyGroups() {
    return groupEnergies(this.pokemon().attachedEnergies ?? []);
  }
}
