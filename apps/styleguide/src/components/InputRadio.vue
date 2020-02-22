<template>
  <form-group v-bind="$props">
    <div>
      <div
        v-for="(item,index) in items"
        :key="index"
        class="form-check form-check-inline"
        :class="{'is-invalid':error}"
      >
        <input
          :id="id+index"
          v-model="value"
          class="form-check-input"
          type="radio"
          :value="item"
          :checked="defaultValue === item"
          :aria-describedby="id + 'Help'"
        />
        <label class="form-check-label" :for="id+index">{{item}}</label>
      </div>
    </div>
  </form-group>
</template>

<script>
import InputSelect from './InputSelect'

export default {
  extends: InputSelect
}
</script>

<docs>
Example with defaultValue
```
<template>
  <div>
    <InputRadio
      label="Animals"
      v-model="check"
      defaultValue="ape"
      :items="['lion', 'ape', 'monkey']"
      help="some help here"
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
