<template>
  <FormGroup :id="id" :label="label" :required="required" :description="description" :errorMessage="errorMessage">
    <InputGroup>
      <table class="table table-bordered table-sm mb-0">
        <tr v-for="(el, idx) in modelValue">
          <td v-if="modelValue.length > 1" class="pt-0 td-shrink text-right">{{ el.locale }}:</td>
          <td class="p-0">
            <InputText :id="id + ':' + el.locale" v-model="modelValue[idx]['value']" class="mb-0" @input="" />
          </td>
        </tr>
      </table>
    </InputGroup>
  </FormGroup>
</template>

<style>
.td-shrink {
  width: 0.1%;
  white-space: nowrap;
}
.small {
  float: right;
  transform: scale(0.8, 0.8);
  transform-origin: top right;
  -ms-transform: scale(0.8, 0.8); /* IE 9 */
  -webkit-transform: scale(0.8, 0.8); /* Safari and Chrome */
  -o-transform: scale(0.8, 0.8); /* Opera */
  -moz-transform: scale(0.8, 0.8); /* Firefox */
}
</style>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import InputText from "./InputText.vue";
import InputSelect from "./InputSelect.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";
import { deepClone } from "../utils.ts";

export default {
  extends: BaseInput, //gives us 'options'
  components: {
    InputSelect,
    InputText,
    FormGroup,
    InputGroup,
  },
  props: {
    locales: {
      type: Array,
      default: () => ["en"],
    },
  },
  data() {
    return {
      localeSelect: undefined, //for selecting locale
    };
  },
  mounted() {
    const value = !Array.isArray(this.modelValue) ? [{ locale: "en", value: "" }] : deepClone(this.modelValue);
    const initializedValue = initializeLocales(this.locales, value);
    this.$emit("update:modelValue", initializedValue);
  },
};

function initializeLocales(locales, value) {
  let newValue = [...value];
  locales.forEach((locale) => {
    if (locale && !value.find((el) => el.locale === locale)) {
      newValue.push({ locale: locale, value: "" });
    }
    if (!value.find((el) => el.locale === "en")) {
      newValue.push({ locale: locale, value: "en" });
    }
  });
  return newValue;
}
</script>

<docs>
<template>
  <div>
    <InputTextLocalized label="With null" id="localized-text-input1" v-model="value0"/>
    value: {{value0}}
   <InputTextLocalized label="Without value" id="localized-text-input1" v-model="value1"/>
    value: {{value1}}
    <InputTextLocalized label="With active value" :locales="['fr','de']" id="localized-text-input2" v-model="value2"/>
    value: {{value2}}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value0: null,
        value1: [],
        value2: [{locale: 'en', value: 'some value'}]

      };
    }
  }
</script>
</docs>
