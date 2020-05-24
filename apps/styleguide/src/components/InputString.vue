<template>
  <FormGroup v-bind="$props">
    <InputAppend
      v-for="(item, idx) in arrayValue"
      :key="idx"
      v-bind="$props"
      :showClear="showClear(idx)"
      @clear="clearValue(idx)"
      :showPlus="showPlus(idx)"
      @add="addRow"
    >
      <input
        :id="id + idx"
        v-model="arrayValue[idx]"
        :class="{ 'form-control': true, 'is-invalid': error }"
        :aria-describedby="id + 'Help'"
        :placeholder="placeholder"
        :readonly="readonly"
        v-on="$listeners"
        @keypress="keyhandler"
      />
    </InputAppend>
  </FormGroup>
</template>

<script>
import BaseInput from './_baseInput.vue'
import InputAppend from './_inputAppend'
import FormGroup from './_formGroup'

export default {
  extends: BaseInput,
  components: {
    InputAppend,
    FormGroup
  },
  methods: {
    keyhandler(event) {
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
            <InputString v-model="value" label="My string input label" help="Some help needed?"/>
            You typed: {{JSON.stringify(value)}}
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
                    :defaultValue="value"
                    label="My string input label"
                    help="Some help needed?"
            />
            <br/>
            You typed: {{value}}
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
    <InputString label="test" :readonly="true" defaultValue="can't change me" help="Should not be able to edit this"/>
    ```
    Example list
    ```
    <template>
        <div>
            <InputString v-model="value" :list="true" label="test" :defaultValue="['aap','noot']"
                         help="should be able to manage a list of values"/>
            <br/>
            You typed: {{JSON.stringify(value)}}
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
</docs>
