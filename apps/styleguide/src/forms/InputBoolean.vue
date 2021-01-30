<script>
import InputRadio from "./InputRadio";

/** Show true/false switch */
export default {
  extends: InputRadio,
  props: {
    options: {
      default: () => [true, false],
    },
  },
};
</script>

<docs>
Example
```
<template>
  <div>
    <InputBoolean v-model="value" label="My first boolean" help="do you need some boolean help?"/>
    value: {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: null
      }
    }
  }
</script>
```
Example with defaultValue
```
<template>
  <div>
    <InputBoolean v-model="value" label="My first boolean"
                  help="do you need some boolean help?"/>
    value: {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: true
      }
    }
  }
</script>
```
</docs>
