<template>
  <FormGroup :id="id" :label="label" :description="description">
    <ResizableTextarea>
      <textarea
        :id="id"
        :value="value"
        class="form-control"
        :aria-describedby="id + 'Help'"
        :placeholder="placeholder"
        @input="$emit('input', $event.target.value)"
      />
    </ResizableTextarea>
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import ResizableTextarea from "./ResizableTextarea.vue";

/** Input for text */
export default {
  extends: BaseInput,
  components: {
    FormGroup,
    ResizableTextarea,
  },
};
</script>

<docs>
<template>
  <div>
    <LayoutForm>
      <InputText
        id="input-text"
        v-model="value"
        label="My text label"
        placeholder="type here your text"
        description="Some help needed?"
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
</docs>
