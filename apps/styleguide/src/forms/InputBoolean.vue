<template>
  <div v-if="inplace">
    <div class="form-check form-check-inline" :class="{ 'is-invalid': error }">
      <input
        :id="id + index"
        v-model="arrayValue[0]"
        class="form-check-input"
        type="checkbox"
        :aria-describedby="id + 'Help'"
      />
    </div>
  </div>
  <InputRadio
    v-else
    v-bind="$props"
    v-model="arrayValue[0]"
    :options="[true, false]"
  />
</template>

<script>
import BaseInput from "./_baseInput.vue";
import InputRadio from "./InputRadio";

export default {
  components: { InputRadio },
  extends: BaseInput,
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
Example inplace
```
<template>
  <div>
    <InputBoolean v-model="value" :inplace="true" label="My first boolean"
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
