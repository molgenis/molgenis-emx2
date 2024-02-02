<template>
  <div class="form-group">
    <label v-if="label !== null && label !== undefined" :for="id">
      <b> {{ label }}</b>
    </label>
    <span v-if="required === 'true' || required === true" class="float-right">
      (required)
    </span>
    <slot></slot>
    <small v-if="errorMessage" class="text-danger form-text">
      {{ errorMessage }}
    </small>
    <small
      v-if="hasDescription(description)"
      :id="id + '-help-text'"
      class="form-text text-muted"
    >
      {{ description }}
    </small>
  </div>
</template>

<script>
export default {
  name: "FormGroup",
  props: {
    id: {
      type: String,
      required: true,
    },
    label: {
      type: String,
      required: false,
    },
    description: {
      type: String,
      required: false,
    },
    required: {
      type: [String, Boolean],
      required: false,
      default: () => false,
    },
    errorMessage: {
      type: String,
      required: false,
    },
  },
  methods: {
    hasDescription(description) {
      return (
        description !== null && description !== undefined && description.length
      );
    },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <div>
      <label for="my-id">default form group</label>
      <FormGroup id="my-id"
        label="my label"
        description="my description">
        <InputGroup>
          <InputString id="my-id"></InputString>
        </InputGroup>
        </FormGroup>
    </div>

    <div class="mt-5">
      <label for="my-id2">required field form group</label>
      <FormGroup id="my-id2"
        label="my label"
        required="'true'"
        description="my description">
        <InputGroup>
          <InputString id="my-id2"></InputString>
        </InputGroup>
        </FormGroup>
    </div>
  </demo-item>
</template>
</docs>
