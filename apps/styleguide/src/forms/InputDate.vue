<template>
  <form-group v-bind="$props" v-on="$listeners">
    <InputAppend
      v-for="(el, idx) in valueArray"
      :key="idx + valueArray.length"
      v-bind="$props"
      :showClear="showClear(idx)"
      @clear="clearValue(idx)"
      :showMinus="showMinus(idx)"
      :showPlus="showPlus(idx)"
      @add="addRow"
    >
      <input
        v-if="readonly"
        readonly
        v-model="valueArray[idx]"
        :class="{'form-control': true, 'is-invalid': errorMessage}"
      />
      <FlatPickr
        v-else
        v-model="valueArray[idx]"
        style="background: white"
        class="form-control active"
        :class="{'is-invalid': errorMessage}"
        :config="config"
        :placeholder="placeholder"
        :disabled="readonly"
        @input="emitValue($event, idx)"
      />
    </InputAppend>
  </form-group>
</template>

<script>
import _baseInput from './_baseInput.vue';
import FormGroup from './_formGroup';
import FlatPickr from 'vue-flatpickr-component';
import 'flatpickr/dist/flatpickr.css';
import InputAppend from './_inputAppend';

/** Show a data input */
export default {
  extends: _baseInput,
  components: {
    FlatPickr,
    InputAppend,
    FormGroup
  },
  computed: {
    config() {
      return {
        wrap: true, // set wrap to true only when using 'input-group'
        dateFormat: 'Y-m-d',
        allowInput: false,
        clickOpens: !this.readonly
      };
    }
  }
};
</script>

<docs>
Example
```
<template>
  <div>
    <InputDate v-model="value" label="My date input label" description="Some help needed?"/>
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
Example readonly with default value
```
<template>
  <div>
    <InputDate :readonly="true" v-model="value" label="My date input label"
               description="Some help needed?"/>
    <br/>
    You typed: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: '2020-1-1'
      };
    }
  };
</script>
```
Example with default value
```
<template>
  <div>
    <InputDate
        v-model="value"
        label="My date input label"
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
        value: '2020-01-10'
      };
    }
  };
</script>
```
Example with errorMessage set
```
<InputDate label="My date input label" errorMessage="Some error message is shown"/>
```
Example with list set
```
<template>
  <div>
    <InputDate :list="true" v-model="value" label="My date input label"/>
    Value: {{ value }}
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
Example with list default
```
<template>
  <div>
    <InputDate :list="true" v-model="value" label="My date input label"/>
    Value: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: ['2020-1-1', '2020-1-2']
      };
    }
  };
</script>
```
</docs>
