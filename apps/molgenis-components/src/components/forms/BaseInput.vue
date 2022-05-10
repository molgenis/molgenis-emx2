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
     * relates to EMX2 columnName
     */
    name: {
      type: String,
      required: false,
      default: function () {
        return this.id;
      },
    },
    value: {
      type: [String, Number, Object, Array, Boolean, File],
      default: null,
    },
    /**
     * Caption for an item in a user interface.
     * relates to EMX2 columnName
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
    /**
     * A string specifying the type of control to render.
     * (note: no direct relation to emx2 type, emx2 types are implemented as vue components)
     */
    type: {
      type: String,
      required: false,
      default: () => "text",
    },
    required: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    placeholderValue() {
      return this.placeholder !== null &&
        this.placeholder !== undefined &&
        this.placeholder.length
        ? this.placeholder
        : this.label;
    },
  },
};
</script>
