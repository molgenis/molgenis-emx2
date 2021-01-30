<template>
  <FormGroup v-bind="$props">
    <InputAppend
      v-for="(el, idx) in arrayValue"
      :key="idx"
      v-bind="$props"
      @clear="clearValue(idx)"
      :showPlus="showPlus(idx)"
      :showClear="showClear(idx)"
      @add="addRow"
    >
      <MessageError v-if="!options">No options provided</MessageError>
      <select
        v-else
        :id="id"
        v-model="arrayValue[idx]"
        :class="{ 'form-control': true, 'is-invalid': error }"
      >
        <option
          v-if="!list || el == undefined"
          :selected="el === undefined"
          disabled
        />
        <option
          v-for="(option, index) in options.filter(
            (o) => el == o || !arrayValue.includes(o)
          )"
          :key="index"
          :value="option"
          :selected="el == option"
        >
          {{ option }}
        </option>
      </select>
    </InputAppend>
  </FormGroup>
</template>

<script>
import _baseInput from "./_baseInput.vue";
import InputAppend from "./_inputAppend";
import FormGroup from "./_formGroup";

export default {
  extends: _baseInput,
  components: {
    InputAppend,
    FormGroup,
  },
  methods: {
    showPlus(idx) {
      return (
        this.option &&
        this.list &&
        !this.readonly &&
        this.arrayValue[idx] != undefined &&
        idx === this.arrayValue.length - 1 &&
        this.options.filter((o) => !this.arrayValue.includes(o)).length > 0
      );
    },
  },
  props: {
    options: Array,
  },
};
</script>

<docs>
Example
```
<template>
  <div>
    <InputSelect
        label=" Animals
                "
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
    />
    Selected: {{ check }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        check: null
      };
    }
  };
</script>
```
Example with default
```
<template>
  <div>
    <InputSelect
        label="Animals"
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
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
    }
  };
</script>
```

Example list with default
```
<template>
  <div>
    <InputSelect
        label="Animals"
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
        :list="true"
    />
    Selected: {{ check }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        check: ['ape']
      };
    }
  };
</script>
```
</docs>
