<template>
  <form-group
    :id="id"
    :label="label"
    :placeholder="placeholder"
    :description="description"
  >
    <input
      :id="id"
      v-model="valueArray[0]"
      class="form-control"
      type="password"
      :aria-describedby="id + 'Help'"
      :placeholder="placeholder"
      @input="emitValue"
      @keyup.enter.stop="$emit('enterPressed')"
    />
  </form-group>
</template>

<script>
import _baseInput from "./_baseInput.vue";
import FormGroup from "./_formGroup";

/** Input for passwords */
export default {
  extends: _baseInput,
  components: { FormGroup },
};
</script>

<docs>
Example
```
<template>
  <div>
    <LayoutForm>
      <InputPassword
          v-model="value"
          label="My password label"
          placeholder="type here your password"
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
```
</docs>
