<script setup lang="ts">
const termsAndConditionsExample = ref<string>("");
const termsAndConditionsExampleReq = ref<string>("");
const signupNewsletterExample = ref<boolean>(false);
const toppingsSelection = ref<string[]>(["tomatoes", "basil"]);
</script>

<template>
  <h2>Checkbox component</h2>
  <p class="mb-2">
    Use the <code>InputCheckbox</code> component if you would like to display a
    single checkbox element. However, this component only returns the input
    element. You will also need to include the
    <code>InputCheckboxIcon</code> and <code>InputLabel</code> components, as
    well as set the css styles to make sure the design is consistent with the
    rest of the frontend. This process is rather straightforward, but we also
    encourage developers to use the <code>InputCheckboxGroup</code> component
    for convenience.
  </p>
  <p class="mb-2">
    The following examples demonstrate the use of both input checkbox components
    with varying configurations.
  </p>
  <form
    class="flex flex-col gap-6 my-5 [&>div]:p-1 [&_h3]:font-semibold [&_h3]:my-2"
  >
    <div>
      <h3>InputCheckbox Example: Default checkbox</h3>
      <InputCheckbox
        id="checkbox-example-newsletter-signup"
        class="sr-only"
        :value="true"
        v-model="signupNewsletterExample"
      />
      <InputLabel
        class="flex flex-row gap-1"
        for="checkbox-example-newsletter-signup"
      >
        <InputCheckboxIcon class="mr-2.5" :checked="signupNewsletterExample" />
        Would you like to subscribe to our newsletter?
      </InputLabel>
      <p>Answer: {{ signupNewsletterExample }}</p>
    </div>
    <div>
      <h3>InputCheckbox Example: Checkbox with yes and no values defined</h3>
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
        class="flex flex-row gap-1"
        for="checkbox-example-terms-and-conditions"
      >
        <InputCheckboxIcon
          class="mr-2.5"
          :checked="termsAndConditionsExample === 'Yes'"
        />
        Do you agree to the terms and conditions?
      </InputLabel>
      <output>
        <span>Selection: {{ termsAndConditionsExample }}</span>
      </output>
    </div>
    <div>
      <h3>
        InputCheckbox Example: Checkbox with yes and no values defined and as a
        required input
      </h3>
      <InputCheckbox
        id="checkbox-example-terms-and-conditions-required"
        class="sr-only"
        v-model="termsAndConditionsExampleReq"
        true-value="Yes"
        false-value="No"
        checked="true"
        required="true"
      />
      <InputLabel
        class="flex flex-row gap-1"
        for="checkbox-example-terms-and-conditions-required"
        :required="true"
      >
        <InputCheckboxIcon
          class="mr-2.5"
          :checked="termsAndConditionsExampleReq === 'Yes'"
        />
        Do you aggree to the terms and conditions?
      </InputLabel>
      <p>Answer: {{ termsAndConditionsExampleReq }}</p>
    </div>
    <div>
      <h3>InputCheckboxGroup example</h3>
      <p class="mb-2">
        The <code>InputCheckboxGroup</code> should be used if there are more
        than one related checkboxes. In the example, below, we demonstrate how
        to create more than one checkbox with custom values and labels, as well
        as set default options.
      </p>
      <legend class="text-title">
        Which toppings would you like on your pizza? You may select more than
        one.
      </legend>
      <InputCheckboxGroup
        id="example-4"
        v-model="toppingsSelection"
        :checkboxOptions="[
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
      <output>
        <span>Selection: {{ toppingsSelection }}</span>
      </output>
    </div>
  </form>
</template>
