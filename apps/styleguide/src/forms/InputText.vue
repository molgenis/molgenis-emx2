<template>
  <span v-if="inplace && !focus" @click="toggleFocus">
    {{ prettyValue ? prettyValue : "" }}
    <IconAction class="hoverIcon" icon="edit" />
  </span>
  <FormGroup v-else :id="id" :label="label" :help="help">
    <InputAppend
      v-for="(el, idx) in arrayValue"
      :key="idx"
      v-bind="$props"
      @clear="clearValue(idx)"
      :plus="
        list && !readonly && el != undefined && idx === arrayValue.length - 1
      "
      @add="addRow"
    >
      <textarea
        :id="id"
        v-focus="inplace"
        v-model="arrayValue[idx]"
        class="form-control"
        :aria-describedby="id + 'Help'"
        :placeholder="placeholder"
        v-on="$listeners"
        @blur="toggleFocus"
      />
    </InputAppend>
  </FormGroup>
</template>

<script>
import _baseInput from "./_baseInput.vue";
import InputAppend from "./_inputAppend";
import FormGroup from "./_formGroup";
import IconAction from "./IconAction";

/** Input for text */
export default {
  extends: _baseInput,
  components: {
    InputAppend,
    FormGroup,
    IconAction,
  },
};
</script>

<docs>
Example
```
<template>
  <div>
    <LayoutForm>
      <InputText
          v-model="value"
          label="My text label"
          placholder="type here your text"
          help="Some help needed?"
      />
    </LayoutForm>
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
    <LayoutForm>
      <InputText
          v-model="value"
          :defaultValue="value"
          label="My text label"
          placholder="type here your text"
          help="Some help needed?"
      />
    </LayoutForm>
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
    <LayoutForm>
      <InputText
          v-model="value"
          :list="true"
          label="My text label"
          placholder="type here your text"
          help="Some help needed?"
      />
    </LayoutForm>
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
    <LayoutForm>
      <InputText
          v-model="value"
          :inplace="true"
          label="My text label"
          placholder="type here your text"
          help="Some help needed?"
      />
    </LayoutForm>
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
