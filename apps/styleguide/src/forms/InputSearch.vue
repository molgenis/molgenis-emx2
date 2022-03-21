<template>
  <form-group v-bind="$props" v-on="$listeners">
    <div class="input-group">
      <span class="input-group-prepend bg-white rounded">
        <button
          class="btn border-right-0 border btn-outline-primary"
          type="button"
        >
          <i class="fa fa-search"></i>
        </button>
      </span>
      <input
        :id="id"
        v-model="valueArray[0]"
        :class="{'form-control': true, 'is-invalid': errorMessage}"
        :aria-describedby="id + 'Help'"
        :placeholder="placeholder"
        :readonly="readonly"
        @keypress="keyhandler"
        @input="emitValue"
      />
      <AppendClear @clear="clearValue(0)" />
    </div>
  </form-group>
</template>

<script>
import InputString from "./InputString.vue";
import AppendClear from "./_appendClear";

export default {
  extends: InputString,
  components: {AppendClear}
};
</script>

<docs>
Example:
```
<template>
  <div>
    <InputSearch v-model="value"/>
    Search terms: {{ value }}
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
