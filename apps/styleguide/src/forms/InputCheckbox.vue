<template>
  <form-group v-bind="$props" class="checkbox-form-group" v-on="$listeners">
    <div>
      <div
        v-for="(item, index) in options"
        :key="index"
        class="form-check form-check-inline"
        :class="{ 'is-invalid': errorMessage }"
      >
        <input
          :id="id + index"
          v-model="result"
          class="form-check-input"
          type="checkbox"
          :value="item"
          @change="
            $emit(
              'input',
              result.filter((v) => v !== 0 || v !== null)
            )
          "
          :aria-describedby="id + 'Help'"
        />
        <label class="form-check-label" :for="id + index">{{ item }}</label>
      </div>
      <a
        class="checkbox-clear-value"
        href="#"
        @click.prevent="
          result = [];
          $emit('input', result);
        "
      >
        clear
      </a>
    </div>
  </form-group>
</template>

<style>
.checkbox-clear-value {
  display: none;
}

.checkbox-form-group:hover .checkbox-clear-value {
  display: inline;
}
</style>

<script>
import InputSelect from "./InputSelect";

export default {
  extends: InputSelect,
  props: {
    list: {
      default: true,
    },
  },
  computed: {
    valueArray() {
      let result = this.value;
      if (!result) result = null;
      if (!Array.isArray(result)) {
        result = [result];
      }
      result = this.removeNulls(result);
      return result;
    },
  },
  data() {
    return {
      result: [],
    };
  },
  created() {
    this.result = this.valueArray != [null] ? this.valueArray : [];
  },
};
</script>

<docs>
Example with defaultValue
```
<template>
  <div>
    <InputCheckbox
        label="Animals"
        v-model="value"
        :options="['lion', 'ape', 'monkey']"
        description="some help here"
    />
    Selected: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: ['ape', 'lion']
      };
    }
  };
</script>
```
</docs>
