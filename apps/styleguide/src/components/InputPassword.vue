<template>
  <form-group :id="id" :label="label" :placeholder="placeholder" :help="help">
    <input
      :id="id"
      v-model="value"
      class="form-control"
      type="password"
      :aria-describedby="id + 'Help'"
      :placeholder="placeholder"
      v-on="$listeners"
    />
  </form-group>
</template>

<script>
import _baseInput from './_baseInput.vue'

/** Input for passwords */
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
      <InputPassword
        v-model="value"
        label="My password label"
        placholder="type here your password"
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
