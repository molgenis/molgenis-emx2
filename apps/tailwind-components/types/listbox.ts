
export type IListboxValueArray = string;

export interface IListboxOption {
  value: string | undefined | null;
  label?: string;
}

export interface IInternalListboxOption extends IListboxOption {
  index: number;
  elemId: string;
}
