<script>
export default {
  name: 'BaseInput',
  props: {
    /**
     * Unique identifier https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/id
     * Has no direct emx2 implementenation but can be constructend by concatinating the table name with the field name
     */
    id: {
      type: String,
      required: true
    },
    /**
     * Name of the element. For example used by the server to identify the fields in form submits.
     * relates to emx 2 columnName
     */
    name: {
      type: String,
      required: false,
      default: function () { return this.id }
    },
    value: {type: [String, Number, Object, Array, Boolean], default: null},
    /**
     * Caption for an item in a user interface.
     * relates to emx 2 columnName
     */
    label: {
      type: String,
      required: false
    },
    /**
     * Additional information describing the field 
     * relates to emx 2 description
     */
    helpText: {
      type: String,
      required: false
    },
    /**
     * A string specifying the type of control to render. 
     * (note: no direct relation to emx2 type, emx2 types are implemented as vue components)
     */
    type: {
      type: String,
      required: false,
      default: () => 'text'
    }
  },
  methods: {
    isNonEmptyString(s) {
      return s !== null && s !== undefined && s.length;
    }
  }
};
</script>
