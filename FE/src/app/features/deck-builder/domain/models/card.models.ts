export type CardSupertype = 'POKEMON' | 'ENERGY' | 'TRAINER';
export type EnergyType =
  | 'GRASS' | 'FIRE' | 'WATER' | 'LIGHTNING' | 'PSYCHIC'
  | 'FIGHTING' | 'DARKNESS' | 'METAL' | 'FAIRY' | 'DRAGON' | 'COLORLESS';

export interface Attack {
  name: string;
  cost: EnergyType[];
  damage: string;
  text: string;
}

export interface TypeModifier {
  type: EnergyType;
  value: string;
}

export interface Ability {
  name: string;
  text: string;
  type: string;
}

export interface CardResponse {
  id: string;
  setId: string;
  name: string;
  supertype: CardSupertype;
  subtypes: string[];
  hp?: number;
  types: string[];
  evolvesFrom?: string;
  attacks: Attack[];
  weaknesses: TypeModifier[];
  resistances: TypeModifier[];
  retreatCost: string[];
  abilities: Ability[];
  basicEnergy: boolean;
  imageSmall: string;
  imageLarge: string;
  rarity: string;
  number: string;
}

export interface CardPage {
  content: CardResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  last: boolean;
}

export interface CardFilters {
  name?: string;
  type?: CardSupertype;
  energyType?: EnergyType;
  pokemonSubtype?: 'Basic' | 'EX' | 'MEGA';
}
