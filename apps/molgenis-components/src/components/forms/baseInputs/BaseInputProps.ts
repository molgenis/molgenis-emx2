export default interface BaseInputProps {
  /**
   * Unique identifier https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/id
   * Has no direct EMX2 implementation but can be constructed by concatenating the table name with the field name
   */
  id: string;
  name?: string;
  label?: string;
  placeholder?: string;
  description?: string;
  required?: boolean;
  readonly?: boolean;
  errorMessage?: string;
}
