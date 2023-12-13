<script>
export default {
  name: "BaseInput",
  props: {
    /**
     * Unique identifier https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/id
     * Has no direct EMX2 implementation but can be constructed by concatenating the table name with the field name
     */
    id: {
      type: String,
      required: true,
    },
    /**
     * Name of the element. For example used by the server to identify the fields in form submits.
     * relates to EMX2 columnId
     */
    name: {
      type: String,
      required: false,
    },
    modelValue: {
      type: [String, Number, Object, Array, Boolean],
      default: null,
    },
    /**
     * Caption for an item in a user interface.
     * relates to EMX2 label
     */
    label: {
      type: String,
      required: false,
    },
    placeholder: {
      type: String,
      required: false,
    },
    /**
     * EMX2 description
     */
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
  },
  computed: {
    placeholderValue() {
      return this.placeholder;
    },
  },
  emits: ["update:modelValue"],
};
</script>
