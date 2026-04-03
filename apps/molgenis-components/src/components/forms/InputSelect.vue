<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage"
  >
    <MessageError v-if="!options || !options.length">
      {{ noOptionsProvidedMessage }}
    </MessageError>
    <select
      v-else-if="!readonly"
      :id="id"
      :modelValue="modelValue"
      :readonly="readonly"
      class="form-control"
      @change="updateModelValue($event.target.value)"
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
    noOptionsProvidedMessage: { type: String, default: "No options provided" },
  },
  methods: {
    updateModelValue: function (value) {
      this.$emit("update:modelValue", value == "" ? null : value);
    },
  },
};
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <InputSelect
        description="Normal select input"
        id="input-select-normal"
        label="Animals"
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
      />
      Selected: {{ check }}
    </DemoItem>
    <DemoItem>
      <InputSelect
          description="With default value 'ape'"
          id="input-select-default"
          label="Default value set"
          v-model="defaultValue"
          :options="['lion', 'ape', 'monkey']"
      />
      Selected: {{ defaultValue }}
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
      <br />IMPORTANT: When using "required", always use a default value. Do not use "null"/"undefined"!
    </DemoItem>
    <DemoItem>
      <InputSelect
        description="Empty select input"
        id="input-select-empty"
        label="No animals"
        v-model="check"
        :options="[]"
      />
    </DemoItem>
    <DemoItem>
      <InputSelect
        id="input-select-readonly"
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
      defaultValue: 'ape',
      requiredCheck: 'lion',
      empty: null,
      readonlyModel: 'lion'
    };
  },
};
</script>
</docs>
