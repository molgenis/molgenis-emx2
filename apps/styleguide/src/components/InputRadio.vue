<template>
  <form-group v-bind="$props" class="radio-form-group">
    <div>
      <div
        v-for="(item, index) in options"
        :key="index"
        class="form-check form-check-inline"
        :class="{ 'is-invalid': error }"
      >
        <input
          :id="id + index"
          v-model="arrayValue[0]"
          class="form-check-input"
          type="radio"
          :value="item"
          :checked="defaultValue === item"
          :aria-describedby="id + 'Help'"
        />
        <label class="form-check-label" :for="id + index">{{ item }}</label>
      </div>
      <a
        class="radio-clear-value"
        href="#"
        v-if="arrayValue[0] !== null"
        @click.prevent="clearValue(0)"
      >
        clear
      </a>
    </div>
  </form-group>
</template>

<style>
.radio-clear-value {
  display: none;
}

.radio-form-group:hover .radio-clear-value {
  display: inline;
}
</style>

<script>
    import InputSelect from "./InputSelect";

    export default {
  extends: InputSelect
};
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
                    :options="['lion', 'ape', 'monkey','armadillo','rino','crocodile','tiger','coyote']"
                    help="some help here"
            />
            Selected: {{check}}
        </div>
    </template>
    <script>
        export default {
            data: function () {
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
