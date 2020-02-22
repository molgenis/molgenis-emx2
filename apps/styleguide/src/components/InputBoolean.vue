<template>
  <InputRadio v-model="value" :items="[true,false]" v-bind="$props" />
</template>

<script>
import _baseInput from './_baseInput'
import InputRadio from './InputRadio'
export default {
  components: { InputRadio },
  extends: _baseInput
}
</script>

<docs>
Example with defaultValue
```
<InputBoolean label="My first boolean" help="do you need some boolean help?"/>
```
</docs>
