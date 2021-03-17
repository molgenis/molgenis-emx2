<template>
    <form-group v-bind="$props" class="checkbox-form-group">
        <div>
            <div
                v-for="(item, index) in options"
                :key="index"
                class="form-check form-check-inline"
                :class="{ 'is-invalid': errorMessage }"
            >
                <input
                    :id="id + index"
                    v-model="result"
                    :aria-describedby="id + 'Help'"
                    class="form-check-input"
                    type="checkbox"
                    :value="item"
                    @change="
                        $emit(
                            'input',
                            result.filter((v) => v !== 0 || v != null)
                        )
                    "
                >
                <label class="form-check-label" :for="id + index">{{ item }}</label>
            </div>
            <a
                class="checkbox-clear-value"
                href="#"
                @click.prevent="
                    result = [];
                    $emit('input', result);
                "
            >
                clear
            </a>
        </div>
    </form-group>
</template>

<script>
import InputSelect from "./InputSelect.vue";

export default {
  extends: InputSelect,
  props: {
    list: {
      default: true,
    },
  },
  data() {
    return {
      result: [],
    };
  },
  computed: {
    valueArray() {
      let result = this.value;
      if (!result) result = null;
      if (!Array.isArray(result)) {
        result = [result];
      }
      result = this.removeNulls(result);
      return result;
    },
  },
  created() {
    this.result = this.valueArray != [null] ? this.valueArray : [];
  },
};
</script>

<style>
.checkbox-clear-value {
  display: none;
}

.checkbox-form-group:hover .checkbox-clear-value {
  display: inline;
}
</style>


