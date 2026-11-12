import type { IInputValueLabel } from "../../metadata-utils/src/types";

export interface IInternalListboxOption<T> {
  item: T;
  value: IInputValueLabel["value"];
  label: string;
  index: number;
  elemId: string;
}

export interface IListboxLiRef {
  li: HTMLLIElement;
}
