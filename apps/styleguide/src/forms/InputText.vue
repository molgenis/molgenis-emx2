<template>
  <span v-if="inplace && !focus" @click="toggleFocus">
    {{ value ? value : "" }}
    <IconAction class="hoverIcon" icon="pencil-alt" />
  </span>
  <FormGroup
    v-else
    :id="id"
    :label="label"
    :description="description"
    v-bind="$props"
    v-on="$listeners"
  >
    <InputAppend
      v-for="(el, idx) in valueArray"
      :key="idx"
      v-bind="$props"
      :showClear="showClear(idx)"
      @clear="
        clearValue(idx);
        key++;
      "
      :showPlus="showPlus(idx)"
      :showMinus="showMinus(idx)"
      @add="addRow"
    >
      <ResizableTextarea :key="key">
        <textarea
          v-focus="inplace && !list"
          :id="id + idx"
          v-model="valueArray[idx]"
          :class="{ 'form-control': true, 'is-invalid': errorMessage }"
          :aria-describedby="id + 'Help'"
          :placeholder="placeholder"
          :readonly="readonly"
          @input="emitValue($event, idx)"
          @blur="toggleFocus"
        />
      </ResizableTextarea>
    </InputAppend>
  </FormGroup>
</template>

<script>
import _baseInput from "./_baseInput.vue";
import InputAppend from "./_inputAppend";
import { IconAction } from "@molgenis/molgenis-components";
import ResizableTextarea from "./ResizableTextarea";

/** Input for text */
export default {
  extends: _baseInput,
  components: {
    InputAppend,
    FormGroup: () => import("./_formGroup"), //because it uses itself in nested form
    IconAction,
    ResizableTextarea,
  },
  data() {
    return {
      //used to elegantly trigger refresh the textareas on clear
      key: 0,
    };
  },
};
</script>

<docs>
Example
```
<template>
  <div>
    <InputText
        v-model="value"
        label="My text label"
        placeholder="type here your text"
        description="Some help needed?"
    />
    <br/>
    You typed: {{ value }}
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
Example with default value
```
<template>
  <div>
    <InputText
        v-model="value"
        :defaultValue="value"
        label="My text label"
        placeholder="type here your text"
        description="Some help needed?"
    />
    <br/>
    You typed: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: "some default value here"
      };
    }
  };
</script>
```
Example with list
```
<template>
  <div>
    <InputText
        v-model="value"
        :list="true"
        label="My text label"
        placeholder="type here your text"
        description="Some help needed?"
    />
    <br/>
    You typed: {{ value }}
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
Example with inplace
```
<template>
  <div>
    <InputText
        v-model="value"
        :inplace="true"
        label="My text label"
        placeholder="type here your text"
        description="Some help needed?"
    />
    <br/>
    You typed: {{ value }}
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
