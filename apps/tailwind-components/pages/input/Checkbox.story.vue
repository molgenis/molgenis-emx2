<script setup lang="ts">
const termsAndConditionsExample = ref<string>("");
const signupNewsletterExample = ref<boolean>(false);
const toppingsSelection = ref<string[]>(["tomatoes", "basil"]);
</script>

<template>
  <form class="flex flex-col gap-6 my-5 [&_h3]:font-semibold [&_h3]:my-2">
    <div>
      <h3>Default checkbox</h3>
      <div class="flex justify-start items-center">
        <InputCheckbox
          id="checkbox-example-newsletter-signup"
          class="sr-only"
          :value="true"
          v-model="signupNewsletterExample"
        />
        <InputLabel
          class="flex justify-start items-center"
          for="checkbox-example-newsletter-signup"
        >
          <InputCheckboxIcon :checked="signupNewsletterExample" />
          <span>Would you like to subscribe to our newsletter?</span>
        </InputLabel>
      </div>
      <output>
        <span>Answer: {{ signupNewsletterExample }}</span>
      </output>
    </div>
    <div>
      <h3>Checkbox with yes and no values defined</h3>
      <div class="flex justify-start items-center">
        <InputCheckbox
          id="checkbox-example-terms-and-conditions"
          class="sr-only"
          true-value="Yes"
          false-value="No"
          :value="true"
          :checked="termsAndConditionsExample"
          v-model="termsAndConditionsExample"
        />
        <InputLabel
          class="flex justify-start items-center"
          for="checkbox-example-terms-and-conditions"
        >
          <InputCheckboxIcon :checked="termsAndConditionsExample === 'Yes'" />
          <span>Do you agree to the terms and conditions?</span>
        </InputLabel>
      </div>
      <output>
        <span>Selection: {{ termsAndConditionsExample }}</span>
      </output>
    </div>
    <div>
      <h3>Checkbox group example</h3>
      <fieldset>
        <legend class="text-title">
          Which toppings would you like on your pizza? You may select more than
          one.
        </legend>
        <InputCheckboxGroup
          id="example-4"
          v-model="toppingsSelection"
          :options="[
            {
              value: 'tomatoes',
              label: 'Roma tomatoes',
            },
            {
              value: 'pepperoni',
              label: 'Pepperoni',
            },
            {
              value: 'mozzerella',
              label: 'Fresh mozzerella',
            },
            {
              value: 'chillies',
              label: 'Chillies',
            },
            {
              value: 'basil',
              label: 'Fresh basil',
            },
          ]"
          :showClearButton="true"
        />
      </fieldset>
      <output>
        <span>Selection: {{ toppingsSelection }}</span>
      </output>
    </div>
  </form>
</template>
