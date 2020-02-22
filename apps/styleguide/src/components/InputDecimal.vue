<script>
import InputString from './InputString'

/** Input for decimal values */
export default {
  extends: InputString,
  props: {
    placeholder: {
      default: 'Please enter decimal number (does not accept A-Za-z)'
    }
  },
  methods: {
    keyhandler (event) {
      if (!this.isDecimal(event)) event.preventDefault()
    },
    isDecimal (e) {
      var keyCode = e.which ? e.which : e.keyCode
      var ret =
        (keyCode >= 48 && keyCode <= 57) ||
        keyCode === 8 ||
        (keyCode === 46 && !this.value.includes('.'))
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
      <InputDecimal v-model="value" label="My decimal input label" help="Some help needed?" />
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
