<script>
import InputString from './InputString';

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
      var specialKeys = [];
      specialKeys.push(8); // Backspace
      var keyCode = e.which ? e.which : e.keyCode;
      var ret =
        (keyCode >= 48 && keyCode <= 57) || specialKeys.indexOf(keyCode) !== -1;
      return ret;
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
