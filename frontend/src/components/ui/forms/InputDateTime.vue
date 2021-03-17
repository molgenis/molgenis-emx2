<script>
import InputDate from "./InputDate";

export default {
  extends: InputDate,
  computed: {
    config() {
      return {
        wrap: true, // set wrap to true only when using 'input-group'
        dateFormat: "Y-m-dTH:i:S",
        allowInput: false,
        enableTime: true,
        clickOpens: !this.readonly,
      };
    },
  },
};
</script>
