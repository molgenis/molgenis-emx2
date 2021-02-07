<template>
  <div v-if="inplace">
    <div
      v-if="focus"
      style="display: inline-block"
      class="dropdown show"
      v-click-outside="toggleFocus"
    >
      <div
        v-for="(el, idx) in arrayValue"
        :key="idx"
        class="dropdown-menu show"
      >
        <a
          v-for="option in options"
          class="dropdown-item"
          href="#"
          :key="option"
          @click.prevent="select(option, idx)"
          :class="{ 'text-primary': option == el }"
          >{{ option ? option : "&zwnj;" }}</a
        >
      </div>
    </div>
    <div
      @click="toggleFocus"
      @mouseover="hover = true"
      @mouseleave="hover = false"
      style="min-width: 1em"
    >
      {{ prettyValue ? prettyValue : "&zwnj;&zwnj;" }}
      <IconAction icon="caret-down" class="float-right hoverIcon" />
      <div v-if="error" class="text-danger">{{ error }}</div>
    </div>
  </div>
  <FormGroup v-else v-bind="$props">
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
        v-focus="inplace"
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
import MessageError from "./MessageError";
import IconAction from "./IconAction";
import vClickOutside from "v-click-outside";

export default {
  directives: {
    clickOutside: vClickOutside.directive,
  },
  extends: _baseInput,
  components: {
    InputAppend,
    FormGroup,
    MessageError,
    IconAction,
  },
  methods: {
    select(option, idx) {
      this.toggleFocus();
      this.arrayValue[idx] = option;
      this.emitValue();
    },
    showPlus(idx) {
      return (
        this.option &&
        this.arrayValue &&
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
Example in place
```
<template>
  <div>
    Dit is inline
    <InputSelect
        :inplace="true"
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
    />
    gezet
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
</docs>
