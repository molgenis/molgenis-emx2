<template>
  <span v-if="inplace">
    <div v-if="focus" class="dropdown show" v-click-outside="toggleFocus">
      <div
        v-for="(el, idx) in valueArray"
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
    <span
      @click="toggleFocus"
      @mouseover="hover = true"
      @mouseleave="hover = false"
      style="min-width: 1em"
      class="d-flex flex-nowrap"
    >
      {{ value ? value : "&zwnj;&zwnj;" }}
      <IconAction icon="pencil-alt" class="hoverIcon" />
      <div v-if="errorMessage" class="text-danger">{{ errorMessage }}</div>
    </span>
  </span>
  <FormGroup v-else v-bind="$props" v-on="$listeners">
    <InputAppend
      v-for="(el, idx) in valueArray"
      :key="idx + '.' + valueArray.length"
      v-bind="$props"
      :showPlus="showPlus(idx)"
      :showClear="false"
      @add="addRow"
    >
      <MessageError v-if="!options">No options provided</MessageError>
      <select
        v-focus="inplace"
        v-else
        :id="id"
        :value="valueArray[idx]"
        :class="{ 'form-control': true, 'is-invalid': errorMessage }"
        @change="emitValue($event, idx)"
      >
        <option v-if="!required" :selected="el === undefined" />
        <option
          v-for="(option, index) in options.filter(
            (o) => el == o || !valueArray.includes(o)
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
      this.emitValue(option, idx);
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
    This is select with value
    <InputSelect
        :inplace="true"
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
    />
    shown inline <br/>
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
