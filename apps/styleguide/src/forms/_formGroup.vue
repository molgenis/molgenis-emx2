/** internal component that will not be shown in style guide */

<template>
  <div class="form-group" :class="inplace ? 'm0' : ''">
    <label v-if="label && !inplace && !editMeta" :for="id">
      <b>{{ label }}</b>
    </label>
    <label v-else-if="editMeta">
      <InputString
        :inplace="true"
        :value="label"
        @input="$emit('update:label', $event)"
      />
    </label>
    <span v-if="required" class="float-right">(required)</span>
    <slot />
    <small
      v-if="description && !inplace && !editMeta"
      :id="id + 'Description'"
      class="form-text text-muted"
    >
      {{ description }}
    </small>
    <small
      v-else-if="editMeta"
      :id="id + 'Description'"
      class="form-text text-muted"
    >
      <InputText
        :inplace="true"
        :value="description"
        @input="$emit('update:description', $event)"
      />
    </small>
    <div v-if="errorMessage" class="text-danger">{{ errorMessage }}</div>
  </div>
</template>

<script>
import InputString from "./InputString";
import InputText from "./InputText";

export default {
  components: { InputString, InputText },
  name: "FormGroup",
  data() {
    return {
      labelEdit: null,
    };
  },
  props: {
    /** id for which this is the group */
    id: String,
    /** value to be shown as input */
    placeholder: String,
    /** label to be shown next to the input */
    label: String,
    /** optional description string shown below */
    description: String,
    /** if required */
    required: Boolean,
    /** String with graphqlError state */
    errorMessage: String,
    /** whether inplace */
    inplace: Boolean,
    /** whether inplace metadata edit */
    editMeta: Boolean,
  },
};
</script>
