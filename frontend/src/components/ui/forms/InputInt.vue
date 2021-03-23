<script>
import InputString from './InputString.vue'

/** Input for integer values */
export default {
  extends: InputString,
  props: {
    parser: {
      default() {
        return (value) => {
          return parseInt(value)
        }
      },
    },
    placeholder: {
      default: 'Enter integer (does not accept A-Za-z,.)',
    },
  },
  methods: {
    isInt(e) {
      var specialKeys = []
      specialKeys.push(8) // Backspace
      var keyCode = e.which ? e.which : e.keyCode
      var ret =
        (keyCode >= 48 && keyCode <= 57) || specialKeys.indexOf(keyCode) !== -1
      return ret
    },
    keyhandler(event) {
      if (!this.isInt(event)) event.preventDefault()
    },
  },
}
</script>
