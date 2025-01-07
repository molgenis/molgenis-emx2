<script setup lang="ts">
const id = "story-input-text-area-1";
const hasError = ref<boolean>(false);
const required = ref<boolean>(false);
const disabled = ref<boolean>(false);
const valid = ref<boolean>(false);
const placeholder = ref<string>("This is placeholder text");
const fieldValue = ref<string>("");
</script>

<template>
  <div class="flex">
    <div class="m-4 flex-1">
      <InputLabel
        :for="id"
        :required="required"
        :has-error="hasError"
        :valid="valid"
        :disabled="disabled"
      >
        Enter text
      </InputLabel>
      <InputTextArea
        :id="id"
        v-model="fieldValue"
        :required="required"
        :disabled="disabled"
        :valid="valid"
        :has-error="hasError"
        :placeholder="placeholder"
      />
    </div>
    <form class="m-4 flex-1">
      <legend>Textarea options</legend>
      <fieldset class="mt-4 mb-2">
        <legend class="m-2 px-2">Modify props</legend>
        <div class="mb-2">
          <InputCheckbox id="input-text-area-required" v-model="required" />
          <InputLabel for="input-text-area-required"> Required </InputLabel>
        </div>
        <div class="mb-2">
          <InputCheckbox id="input-text-area-has-error" v-model="hasError" />
          <InputLabel for="input-text-area-has-error"> Show error </InputLabel>
        </div>
        <div class="mb-2">
          <InputCheckbox id="input-text-area-disabled" v-model="disabled" />
          <InputLabel for="input-text-area-disabled" v-model="disabled">
            Disable input
          </InputLabel>
        </div>
        <div class="mb-2">
          <InputCheckbox id="input-text-area-valid" v-model="valid" />
          <InputLabel for="input-text-area-valid"> Validate input </InputLabel>
        </div>
      </fieldset>
      <div class="mt-4">
        <InputLabel for="text-area-placeholder"> Set placeholder </InputLabel>
        <InputString id="text-area-placeholder" v-model="placeholder" />
      </div>
      <div class="mt-4">
        <p>Current value</p>
        <output class="block p-2 mt-2 bg-white h-28 overflow-scroll">
          {{ fieldValue }}
        </output>
        <Button
          @click="fieldValue = ''"
          type="primary"
          size="small"
          class="mt-2"
        >
          clear input value
        </Button>
      </div>
    </form>
  </div>
</template>
