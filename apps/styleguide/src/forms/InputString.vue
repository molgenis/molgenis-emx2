<template>
  <span>
    <span v-if="inplace && !focus && !errorMessage" @click="toggleFocus">
      <span v-if="list && value">{{ value.join(', ') }}</span>
      <span v-else> {{ value ? value : '&zwnj;&zwnj;' }}</span>
    </span>
    <FormGroup v-else v-bind="$props" v-on="$listeners">
      <InputAppend
        v-for="(item, idx) in valueArray"
        :key="idx"
        v-bind="$props"
        :showClear="(!inplace || list) && showClear(idx)"
        @clear="clearValue(idx)"
        @add="addRow"
        :showPlus="showPlus(idx)"
        :showMinus="showMinus(idx)"
      >
        <input
          v-focus="inplace && !list"
          :value="item"
          :class="{'form-control': true, 'is-invalid': errorMessage}"
          :aria-describedby="id + 'Help'"
          :placeholder="placeholder"
          :readonly="readonly"
          @keypress="keyhandler"
          @input="emitValue($event, idx)"
          @blur="toggleFocus"
        />
      </InputAppend>
    </FormGroup>
    <IconAction
      v-if="inplace && !focus"
      class="hoverIcon"
      icon="pencil-alt"
      @click="toggleFocus"
    />
  </span>
</template>

<script>
import BaseInput from './_baseInput.vue';
import InputAppend from './_inputAppend';
import IconAction from './IconAction';

export default {
  extends: BaseInput,
  components: {
    InputAppend,
    FormGroup: () => import('./_formGroup'), //because it uses itself in nested form
    IconAction
  },
  methods: {
    keyhandler(event) {
      return event;
    }
  }
};
</script>

<style scoped>
.is-invalid {
  background-image: none;
}

span:hover .hoverIcon {
  visibility: visible;
}
</style>

<docs>
Example
```
<template>
  <div>
    <InputString v-model="value" label="My string input label" description="Some help needed?"/>
    You typed: {{ JSON.stringify(value) }}
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
Example with default value
```
<template>
  <div>
    <InputString
        v-model="value"
        label="My string input label"
        description="Some help needed?"
    />
    <br/>
    You typed: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: "blaat"
      };
    }
  };
</script>
```
Example readonly
```
<InputString label="test" :readonly="true" value="can't change me" description="Should not be able to edit this"/>
```
Example list
```
<template>
  <div>
    <InputString v-model="value" :list="true" label="test"
                 description="should be able to manage a list of values"/>
    <br/>
    You typed: {{ JSON.stringify(value) }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: ['aap', 'noot']
      };
    },
  };
</script>
```
Example in place
```
<template>
  <div>
    In place some
    <InputString label="test" v-model="value" :inplace="true" description="Should be able to edit in place"/>
    text.<br/>
    value: {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {value: null}
    }
  }
</script>
```
Example list in place
```
<template>
  <div>
    In place some
    <InputString label="test" :list="true" v-model="value" :inplace="true"
                 description="Should be able to edit in place"/>
    text.<br/>
    value: {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {value: ['aap', 'noot']}
    }
  }
</script>
```

Metadata edit example
```
<template>
  <div>
    <InputString :label.sync="column.label" v-model="column.value" :editMeta="true"
                 :description.sync="column.description"/>
    text.<br/>
    column :
    <pre>{{ column }}</pre>
  </div>
</template>
<script>
  export default {
    data() {
      return {column: {value: null, label: 'testlabel', description: 'test description'}}
    }
  }
</script>
```
</docs>
