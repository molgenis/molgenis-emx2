<template>
  <form-group :id="id" :label="label" :help="help">
    <textarea
      :id="id"
      v-model="value"
      class="form-control"
      :aria-describedby="id + 'Help'"
      :placeholder="placeholder"
      v-on="$listeners"
    />
  </form-group>
</template>

<script>
import _baseInput from './_baseInput.vue'

/** Input for text */
export default {
  extends: _baseInput
}
</script>

<docs>
Examlple
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
    <br />
    You typed: {{value}}
  </div>
</template>
<script>
export default {
  data: function() {
    return {
      value: null
    };
  }
};
</script>
```
</docs>
