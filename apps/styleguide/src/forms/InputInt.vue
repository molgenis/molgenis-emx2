<script>
import InputString from './InputString';
import {CODE_0, CODE_9, CODE_BACKSPACE} from '../constants';

/** Input for integer values */
export default {
  extends: InputString,
  props: {
    placeholder: {
      default: 'Enter integer (does not accept A-Za-z,.)'
    },
    parser: {
      default() {
        return (value) => parseInt(value);
      }
    }
  },
  methods: {
    keyhandler(event) {
      if (!this.isInt(event)) event.preventDefault();
    },
    isInt(e) {
      let specialKeys = [];
      specialKeys.push(CODE_BACKSPACE);
      const keyCode = e.which ? e.which : e.keyCode;
      return (
        (keyCode >= CODE_0 && keyCode <= CODE_9) ||
        specialKeys.indexOf(keyCode) !== -1
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
    <LayoutForm>
      <InputInt v-model="value" label="My int input label" description="Some help needed?"/>
    </LayoutForm>
    <br/>
    You typed: {{ JSON.stringify(value) }}
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
Example list
```
<template>
  <div>
    <LayoutForm>
      <InputInt :list="true" v-model="value" label="My int input label" description="Some help needed?"/>
    </LayoutForm>
    <br/>
    You typed: {{ JSON.stringify(value) }}
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
