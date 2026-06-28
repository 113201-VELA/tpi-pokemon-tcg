import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import {
  ActivePokemonDTO,
  CONDITION_CONFIG,
  SpecialCondition,
  getHpClass,
  getHpPercent,
  groupEnergies,
} from '../../domain/models/game.models';
import { CardResponse } from '../../../deck-builder/domain/models/card.models';

@Component({
  selector: 'app-active-pokemon-slot',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="active-slot-inner" (click)="cardClick.emit(pokemon().card)">
      @if (pokemon().card) {
        <img
          class="card-img"
          [class]="'card-img ' + cardRotationClass()"
          [src]="pokemon().card!.imageSmall"
          [alt]="pokemon().card!.name"
        />
      }

      <!-- HP bar -->
      <div class="hp-bar-wrap">
        <div
          class="hp-bar"
          [class]="hpClass()"
          [style.width.%]="hpPercent()"
        ></div>
      </div>
      <span class="hp-text">{{ pokemon().currentHp }}/{{ pokemon().maxHp }}</span>

      <!-- Special conditions -->
      @if (pokemon().conditions.length) {
        <div class="conditions-row">
          @for (cond of pokemon().conditions; track cond) {
            <span
              class="condition-badge"
              [style.background]="conditionColor(cond)"
            >{{ cond }}</span>
          }
        </div>
      }

      <!-- Attached energies -->
      @if (pokemon().attachedEnergies.length) {
        <div class="energies-row">
          @for (group of energyGroups(); track group.type) {
            <span class="energy-chip">{{ group.icon }} {{ group.count }}</span>
          }
        </div>
      }
    </div>
  `,
  styleUrl: './active-pokemon-slot.css',
})
export class ActivePokemonSlot {
  readonly pokemon   = input.required<ActivePokemonDTO>();
  readonly cardClick = output<CardResponse | null>();

  readonly conditionConfig = CONDITION_CONFIG;

  hpPercent(): number {
    return getHpPercent(this.pokemon().currentHp, this.pokemon().maxHp);
  }

  hpClass(): string {
    return getHpClass(this.hpPercent());
  }

  energyGroups() {
    return groupEnergies(this.pokemon().attachedEnergies ?? []);
  }

  conditionColor(cond: SpecialCondition): string {
    return this.conditionConfig[cond]?.color ?? '#666';
  }

  cardRotationClass(): string {
    const conditions = this.pokemon().conditions ?? [];
    if (conditions.includes('PARALYZED')) return 'card-img--rotated-cw';
    if (conditions.includes('ASLEEP'))    return 'card-img--rotated-ccw';
    if (conditions.includes('CONFUSED'))  return 'card-img--rotated-180';
    return '';
  }
}
