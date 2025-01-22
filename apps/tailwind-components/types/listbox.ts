
import type { IInputValueLabel } from "../../metadata-utils/src/types"; 

export interface IInternalListboxOption extends IInputValueLabel {
  index: number;
  elemId: string;
}

export interface IListboxUlRef {
  ul: HTMLUListElement;
}

export interface IListboxButtonef {
  button: HTMLButtonElement;
}
export interface IListboxLiRef {
  li: HTMLLIElement;
}