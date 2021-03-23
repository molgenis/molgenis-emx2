<script>
import InputDate from './InputDate.vue'

export default {
  extends: InputDate,
  computed: {
    config() {
      return {
        allowInput: false,
        clickOpens: !this.readonly,
        dateFormat: 'Y-m-dTH:i:S',
        enableTime: true,
        wrap: true, // set wrap to true only when using 'input-group'
      }
    },
  },
}
</script>
