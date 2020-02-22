<template>
  <form-group v-bind="$props">
    <input
      :id="id"
      v-model="value"
      :class="{ 'form-control': true, 'is-invalid': error }"
      :aria-describedby="id + 'Help'"
      :placeholder="placeholder"
      :readonly="readonly"
      v-on="$listeners"
      @keypress="keyhandler"
    />
  </form-group>
</template>

<script>
import _baseInput from './_baseInput.vue'

export default {
  extends: _baseInput,
  methods: {
    keyhandler (event) {
      return event
    }
  }
}
</script>

<docs>
Example
```
<template>
  <div>
    <LayoutForm>
      <InputString v-model="value" label="My string input label" help="Some help needed?" />
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
Example with initial value
```
<template>
  <div>
    <LayoutForm>
      <InputString
        v-model="value"
        default="my default value"
        label="My string input label"
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
      value: "blaat"
    };
  }
};
</script>
```
Example readonly
```
<InputString label="test" :readonly="true" defaultValue="can't change me" help="Should not be able to edit this"/>
```
</docs>
