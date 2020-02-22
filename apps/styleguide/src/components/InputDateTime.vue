<script>
import InputDate from './InputDate'

export default {
  extends: InputDate,
  data: function () {
    return {
      config: {
        wrap: true, // set wrap to true only when using 'input-group'
        dateFormat: 'Y-m-dTH:i:S',
        allowInput: false,
        enableTime: true
      }
    }
  }
}
</script>

<docs>
Example
```
<template>
  <div>
    <InputDateTime v-model="value" label="My date time input label" help="Some help needed?" />
    <br />
    You typed: {{value}}
  </div>
</template>
<script>
export default {
  data: function() {
    return {
      value: null
    };
  }
};
</script>
```
</docs>
