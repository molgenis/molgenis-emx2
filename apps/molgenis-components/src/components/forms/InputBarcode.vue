<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="stringError"
  >
  <InputGroup>
      <template v-slot:prepend>
        <slot name="prepend"></slot>
      </template>
      <input
        :id="id"
        :ref="id"
        :name="name"
        :value="modelValue"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        type="text"
        class="form-control"
        :class="{ 'is-invalid': stringError }"
        :aria-describedby="id"
        :placeholder="placeholder"
        :readonly="readonly"
      />
      <IconAction icon="camera" label="Scan barcode"/>
      <template v-slot:append>
        <slot name="append"></slot>
        
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script lang="ts">
export default {
  name: "InputBarcode"
};
</script>

<script lang="ts" setup>
import { computed } from "vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";
import IconAction from "./IconAction.vue";

import type { IBaseInput } from "../../Interfaces/IInputs";

interface IBarcode extends IBaseInput {
  stringLength?: number; 
}

const props = withDefaults(
  defineProps<IBarcode>(),
  {
    stringLength: 255,
  }
);

const stringError = computed(() => {
  if (props.modelValue && props.modelValue.length && props.modelValue.length > props.stringLength) {
    return `Please limit to ${props.stringLength} characters.`;
  } else {
    return props.errorMessage;
  }
})

</script>

<docs>
<template>
  <div>
    <InputBarcode id="input-barcode-1" v-model="value" label="My barcode input label" description="Enter or scan the barcode"/>
    You typed: {{ JSON.stringify(value) }}<br/>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: "1234"
      };
    }
  };
</script>

</docs>