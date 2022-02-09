<template>
  <form-group v-bind="$props" class="radio-form-group" v-on="$listeners">
    <div>
      <div
        v-for="(item, idx) in options"
        :key="idx"
        class="form-check form-check-inline"
        :class="{ 'is-invalid': errorMessage }"
      >
        <input
          :id="id + idx"
          v-model="valueArray[0]"
          class="form-check-input"
          type="radio"
          :value="item"
          :checked="valueArray[0] == item"
          :aria-describedby="id + 'Help'"
          @change="emitValue($event, idx)"
        />
        <label class="form-check-label" :for="id + idx">{{ item }}</label>
      </div>
      <a
        class="radio-clear-value"
        href="#"
        v-if="!required && valueArray[0] !== null"
        @click.prevent="clearValue(0)"
      >
        clear
      </a>
    </div>
  </form-group>
</template>

<style>
.radio-clear-value {
  display: none;
}

.radio-form-group:hover .radio-clear-value {
  display: inline;
}
</style>

<script>
import InputSelect from "./InputSelect";

export default {
  extends: InputSelect,
};
</script>

<docs>
Example with default
```
<template>
  <div>
    <InputRadio
        label="Animals"
        v-model="check"
        :options="['lion', 'ape', 'monkey','armadillo','rino','crocodile','tiger','coyote']"
        description="some help here"
    />
    Selected: {{ check }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        check: 'ape'
      };
    },
    methods: {
      clear() {
        this.check = null;
      }
    }
  };
</script>
```
</docs>
