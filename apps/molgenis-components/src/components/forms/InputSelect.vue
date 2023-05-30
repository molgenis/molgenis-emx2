<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage"
  >
    <MessageError v-if="!options || !options.length">
      No options provided
    </MessageError>
    <select
      v-else-if="!readonly"
      :id="id"
      :modelValue="modelValue"
      :readonly="readonly"
      class="form-control"
      @change="$emit('update:modelValue', $event.target.value)"
    >
      <option
        v-if="!required"
        :selected="modelValue === undefined || modelValue === null"
      >
        {{ placeholder }}
      </option>
      <option
        v-for="(option, index) in options"
        :key="index + option"
        :value="option"
        :selected="modelValue == option"
      >
        {{ option }}
      </option>
    </select>
    <input
      :id="id"
      v-else
      class="form-control"
      type="text"
      readonly
      :value="modelValue"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import MessageError from "./MessageError.vue";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
    MessageError,
  },
  props: {
    options: { type: Array, required: true },
  },
  created() {
    if (this.required && this.options.length === 1) {
      this.$emit("update:modelValue", this.options[0]);
    }
  },
};
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <InputSelect
        description="Normal select input"
        id="input-select"
        label="Animals"
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
      />
      Selected: {{ check }}
    </DemoItem>
    <DemoItem>
      <InputSelect
        id="input-select-required"
        description="Required select input"
        label="Required Animals"
        required
        v-model="requiredCheck"
        :options="['lion', 'ape', 'monkey']"
      />
      Selected: {{ requiredCheck }}
    </DemoItem>
    <DemoItem>
      <InputSelect
        description="Empty select input"
        id="input-select"
        label="No animals"
        v-model="check"
        :options="[]"
      />
    </DemoItem>
    <DemoItem>
      <InputSelect
        id="input-select"
        label="Readonly"
        v-model="readonlyModel"
        :options="['lion', 'ape', 'monkey']"
        readonly
      />
      Selected: {{ readonlyModel }}
    </DemoItem>
  </div>
</template>
<script>
export default {
  data: function () {
    return {
      check: null,
      requiredCheck: null,
      empty: null,
      readonlyModel: 'lion'
    };
  },
};
</script>
</docs>
