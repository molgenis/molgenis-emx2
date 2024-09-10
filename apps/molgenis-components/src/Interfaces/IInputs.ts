export type IBaseInput = {
  id: string;
  name?: string;
  modelValue?: string;
  label?: string;
  placeholder?: string;
  description?: string;
  required?: boolean | string;
  readonly?: boolean;
  errorMessage?: string;
};
