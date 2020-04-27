<template>
  <form-group v-bind="$props">
    <div>
      <div
        v-for="(item, index) in options"
        :key="index"
        class="form-check form-check-inline"
        :class="{ 'is-invalid': error }"
      >
        <input
          :id="id + index"
          @change="
            arrayValue.includes(item)
              ? (arrayValue = arrayValue.filter(c => c != item))
              : arrayValue.push(item)
          "
          class="form-check-input"
          type="checkbox"
          :value="item"
          :checked="arrayValue.includes(item)"
          :aria-describedby="id + 'Help'"
        />
        <label class="form-check-label" :for="id + index">{{ item }}</label>
      </div>
      <a
        href="#"
        v-if="arrayValue.filter(c => c != undefined).length > 0"
        @click.prevent="arrayValue = [null]"
      >
        clear
      </a>
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
            <InputCheckbox
                    label="Animals"
                    v-model="value"
                    :defaultValue="value"
                    :options="['lion', 'ape', 'monkey']"
                    help="some help here"
            />
            Selected: {{value}}
        </div>
    </template>
    <script>
        export default {
            data: function () {
                return {
                    value: ['ape', 'lion']
                };
            }
        };
    </script>
    ```
</docs>
