<template>
  <div v-if="inplace">
    <div
      class="form-check form-check-inline"
      :class="{ 'is-invalid': errorMessage }"
    >
      <input
        :id="id"
        v-model="valueArray[0]"
        class="form-check-input"
        type="checkbox"
        :aria-describedby="id + 'Help'"
        @change="$emit('input', $event.target.checked)"
      />
    </div>
  </div>
  <InputRadio
    v-else
    v-bind="$props"
    v-model="valueArray[0]"
    :options="[true, false]"
    @input="emitValue"
    v-on="$listeners"
  />
</template>

<script>
import BaseInput from "./_baseInput.vue";
import InputRadio from "./InputRadio";

export default {
  components: { InputRadio },
  extends: BaseInput,
  methods: {
    emitValue() {
      console.log("input " + this.valueArray);
      this.$emit(
        "input",
        this.valueArray[0] ? this.valueArray[0] === "true" : null
      );
    },
  },
};
</script>

<docs>
Example
```
<template>
  <div>
    <InputBoolean v-model="value" label="My first boolean" description="do you need some boolean help?"/>
    value: {{ value }} type: {{ typeof value }}

  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: false
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
                  description="do you need some boolean help?"/>
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
                  description="do you need some boolean help?"/>
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
