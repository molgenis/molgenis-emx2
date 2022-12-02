<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage"
  >
    <InputGroup>
      <textarea
        ref="textarea"
        :value="modelValue"
        class="form-control"
        :class="{ 'is-invalid': errorMessage }"
        :aria-describedby="id + 'Help'"
        :placeholder="placeholder"
        :readonly="readonly"
        @input="$emit('update:modelValue', $event.target.value)"
      />
      <template v-slot:append>
        <slot name="append"></slot>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";
import { nextTick } from "vue";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
    InputGroup,
  },
  methods: {
    resizeTextarea(event) {
      event.target.style.height = "auto";
      event.target.style.height = event.target.scrollHeight + "px";
    },
  },
  mounted() {
    const el = this.$refs.textarea;
    nextTick(() => {
      el.setAttribute(
        "style",
        "height:" + el.scrollHeight + "px;overflow-y:hidden;"
      );
    });
    el.addEventListener("input", this.resizeTextarea);
  },
  beforeDestroy() {
    this.$refs.textarea.addEventListener("input", this.resizeTextarea);
  },
};
</script>

<docs>
<template>
  <div>
    <label>Empty input text</label>
    <demo-item>
      <InputText
          id="input-text"
          v-model="value"
          label="My text label"
          placeholder="type here your text"
          description="Some help needed?"
      />
      You typed:<br/>
      <pre>{{ value }}</pre>
    </demo-item>
    <label>Empty input text</label>
    <demo-item>
      <InputText
          id="input-text2"
          v-model="value2"
          label="My text label"
          placeholder="type here your text"
          description="This should have default value?"
      />
      <br/>
      You typed:<br/>
      <pre>{{ value2 }}</pre>
    </demo-item>
    <demo-item>
      <InputText
          id="input-text3"
          v-model="value2"
          label="My readonly text"
          readonly
      />
    </demo-item>
  </div>
</template>
<script>
  export default {
    data: function() {
      return {
        value: null,
        value2: "this is a default value",
      };
    },
  };
</script>
</docs>
