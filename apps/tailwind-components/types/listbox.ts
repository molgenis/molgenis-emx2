
export type IListboxValue = string | number | boolean | undefined | null;

export interface IListboxOption {
  value: IListboxValue;
  label?: string;
}

export interface IInternalListboxOption extends IListboxOption {
  index: number;
  elemId: string;
}
