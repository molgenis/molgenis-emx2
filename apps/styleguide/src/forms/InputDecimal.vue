<script>
import InputString from './InputString';
import {CODE_0, CODE_9, CODE_BACKSPACE, CODE_DELETE} from '../constants';

/** Input for decimal values */
export default {
  extends: InputString,
  props: {
    placeholder: {
      default: 'enter decimal (does not accept A-Za-z,)'
    },
    parser: {
      default() {
        return (value) => {
          return parseFloat(value);
        };
      }
    }
  },
  methods: {
    keyhandler(event) {
      if (!this.isDecimal(event)) event.preventDefault();
    },
    isDecimal(event) {
      const keyCode = event.which ? event.which : event.keyCode;
      return (
        (keyCode >= CODE_0 && keyCode <= CODE_9) ||
        keyCode === CODE_BACKSPACE ||
        (keyCode === CODE_DELETE && !this.value.toString().includes('.'))
      );
    }
  }
};
</script>

<docs>
Example
```
<template>
  <div>
    <InputDecimal v-model="value" label="My decimal input label" description="Some help needed?"/>
    <br/>
    You typed: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: null
      };
    }
  };
</script>
```
Example with list
```
<template>
  <div>
    <InputDecimal :list="true" v-model="value" label="My decimal input label" description="Some help needed?"/>
    You typed: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: null
      };
    }
  };
</script>
```


</docs>
