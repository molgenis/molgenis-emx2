<template>
  <FormGroup :id="id" :label="label" :description="description">
    <MessageError v-if="!options">No options provided</MessageError>
    <select
      v-else
      :id="id"
      :value="value"
      class="form-control"
      @change="$emit('input', $event.target.value)"
    >
      <option v-if="!required" :selected="value === undefined" />
      <option
        v-for="(option, index) in options"
        :key="index"
        :value="option"
        :selected="value == option"
      >
        {{ option }}
      </option>
    </select>
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import MessageError from "./MessageError.vue";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
    MessageError,
  },
  props: {
    options: Array,
  },
};
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <div> InputSelect </div>
      <InputSelect
        id="input-select"
        label="Animals"
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
      />
    </DemoItem>
   <DemoItem>
      <div> InputSelect - required </div>
      <InputSelect
        id="input-select"
        label="Animals"
        required
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
      />
    </DemoItem>
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
</docs>
