<template>
  <form-group v-bind="$props">
    <select :id="id" v-model="value" class="custom-select">
      <option selected disabled></option>
      <option v-for="(item,index) in items" :key="index" :value="item">{{item}}</option>
    </select>
  </form-group>
</template>

<script>
import _baseInput from './_baseInput.vue'

export default {
  extends: _baseInput,
  props: {
    items: Array
  }
}
</script>

<docs>
Example with defaultValue
```
<template>
  <div>
    <InputSelect
      label="Animals"
      v-model="check"
      defaultValue="ape"
      :items="['lion', 'ape', 'monkey']"
    />
    Selected: {{check}}
    <ButtonAction @click="clear">Clear</ButtonAction>
  </div>
</template>
<script>
export default {
  data: function() {
    return {
      check: null
    };
  },
  methods: {
    clear() {
      this.check = null;
    }
  }
};
</script>
```
</docs>
