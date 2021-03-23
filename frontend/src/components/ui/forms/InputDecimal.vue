<script>
import InputString from './InputString.vue'

/** Input for decimal values */
export default {
  extends: InputString,
  props: {
    // for outputing not string but floats
    parser: {
      default() {
        return (value) => {
          return parseFloat(value)
        }
      },
    },
    placeholder: {
      default: 'enter decimal (does not accept A-Za-z,)',
    },
  },
  methods: {
    isDecimal(e) {
      var keyCode = e.which ? e.which : e.keyCode
      var ret =
        (keyCode >= 48 && keyCode <= 57) ||
        keyCode === 8 ||
        (keyCode === 46 && !this.value.includes('.'))
      return ret
    },
    keyhandler(event) {
      if (!this.isDecimal(event)) event.preventDefault()
    },
  },
}
</script>
