<script>
export default {
  name: 'BaseInput',
  props: {
    id: {
      type: String,
      required: true
    },
    name: {
      type: String,
      required: false,
      default: function () { return this.id }
    },
    value: {type: [String, Number, Object, Array, Boolean], default: null},
    label: {
      type: String,
      required: false
    },
    placeholder: {
      type: String,
      required: false
    },
    helpText: {
      type: String,
      required: false
    },
    type: {
      type: String,
      required: false,
      default: () => 'text'
    }
  },
  computed: {
    placeholderValue () {
      return this.placeholder !== null && this.placeholder !== undefined && this.placeholder.length ? this.placeholder : this.label
    }
  }
};
</script>