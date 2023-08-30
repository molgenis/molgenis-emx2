export default {
  /**
   * Unique identifier https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/id
   * Has no direct EMX2 implementation but can be constructed by concatenating the table name with the field name
   */
  id: {
    type: String,
    required: true,
  },
  name: {
    type: String,
    required: false,
  },
  label: {
    type: String,
    required: false,
  },
  placeholder: {
    type: String,
    required: false,
  },
  description: {
    type: String,
    required: false,
  },
  required: {
    type: Boolean,
    default: false,
  },
  readonly: {
    type: Boolean,
    default: false,
  },
  errorMessage: {
    type: String,
    required: false,
    default: () => null,
  },
};
