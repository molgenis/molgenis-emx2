<script>
import InputString from './InputString'

/** Input for decimal values */
export default {
  extends: InputString,
  props: {
    placeholder: {
      default: 'Please enter non-decimal number (does not accept A-Za-z)'
    }
  },
  methods: {
    keyhandler (event) {
      if (!this.isInt(event)) event.preventDefault()
    },
    isInt (e) {
      var specialKeys = []
      specialKeys.push(8) // Backspace
      var keyCode = e.which ? e.which : e.keyCode
      var ret =
        (keyCode >= 48 && keyCode <= 57) || specialKeys.indexOf(keyCode) !== -1
      return ret
    }
  }
}
</script>

<docs>
Example
```
<template>
  <div>
    <LayoutForm>
      <InputInt v-model="value" label="My int input label" help="Some help needed?" />
    </LayoutForm>
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
