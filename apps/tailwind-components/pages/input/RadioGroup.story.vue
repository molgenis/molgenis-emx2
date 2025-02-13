<template>
  <InputTestContainer showState>
    <p>
      There are multiple ways to create radio buttons using the tailwind
      component library. You can either create them manually using the
      <code>InputRadio</code> and <code>InputLabel</code> components, but this
      requires a but more work to link form elements to ensure they are part of
      the same group. The examples below demonstrate the three different ways to
      use the <code>InputRadioGroup</code> component.
    </p>
    <ul class="list-disc ml-14 my-2">
      <li>Default use: setting the name and values</li>
      <li>
        Custom labels: passing an array of labels to display instead of the
        values
      </li>
      <li>
        Showing a clear selection button: rendering the radio inputs with a
        clear selection button, labels and a default value
      </li>
    </ul>
    <h3>Input radio group examples</h3>
    <form class="mt-4 [&_fieldset]:mb-4">
      <fieldset class="flex flex-col gap-1">
        <legend class="text-title">
          Example 1: Do you agree to the terms and conditions?
        </legend>
        <InputRadioGroup
          id="example-1"
          :options="[{ value: 'no' }, { value: 'yes' }]"
          v-model="question1Response"
        />
        <output>
          <span>Selection: {{ question1Response }}</span>
        </output>
      </fieldset>
      <fieldset>
        <legend class="text-title">
          Example 2: Specify the level of security needed.
        </legend>
        <InputRadioGroup
          id="example-2"
          v-model="question2Response"
          :options="[
            { value: 'level-1', label: 'Level 1 (A)' },
            { value: 'level-2', label: 'Level 2 (AA)' },
            { value: 'level-3', label: 'Level 3 (AAA)' },
          ]"
        />
        <output>
          <span>Selection: {{ question2Response }}</span>
        </output>
      </fieldset>
      <fieldset>
        <legend class="text-title">
          Example 3: Select the patient's group allocation. If unknown, please
          leave blank.
        </legend>
        <InputRadioGroup
          id="example-3"
          v-model="question3Response"
          :options="[
            { value: 'control', label: 'Healthy controls' },
            {
              value: 'intervention',
              label: 'Experimental cohort',
            },
            { value: 'placebo', label: 'Placebo cohort' },
          ]"
          :show-clear-button="true"
        />
        <output>
          <span>Selection: {{ question3Response }}</span>
        </output>
      </fieldset>
    </form>
  </InputTestContainer>
</template>

<script lang="ts" setup>
const question1Response = ref<string>("");
const question2Response = ref<string>("");
const question3Response = ref<string>("intervention");
</script>
