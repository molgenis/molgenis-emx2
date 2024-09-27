<template>
  <h2>Radio Component</h2>
  <p>
    There are multiple ways to create radio buttons using the tailwind component
    libray. You can either create them manually using the
    <code>InputRadio</code> and <code>InputLabel</code> components, but this
    requires a but more work to link form elements to ensure they are part of
    the same group. The examples below demonstrate the three different ways to
    use the <code>InputRadioGroup</code> component.
  </p>
  <ul class="list-disc ml-12 my-2">
    <li>Default use: setting the name and values</li>
    <li>
      Custom labels: passing an array of labels to display instead of the values
    </li>
    <li>
      Showing a clear selection button: rendering the radio inputs with a clear
      selection button
    </li>
  </ul>
  <h3>Input radio group examples</h3>
  <form class="mt-4 [&_fieldset]:mb-4">
    <fieldset class="flex flex-col gap-1">
      <legend class="text-title">
        Example 1: Do you agree to the terms and conditions?
      </legend>
      <InputRadioGroup id="example-1" :values="['No', 'Yes']" />
    </fieldset>
    <fieldset>
      <legend class="text-title">
        Example 2: Specify the level of security needed.
      </legend>
      <InputRadioGroup
        id="example-2"
        :values="['level-1', 'level-2', 'level-3']"
        :labels="['Level 1 (A)', 'Level 2 (AA)', 'Level 3 (AAA)']"
      />
    </fieldset>
    <fieldset>
      <legend class="text-title">
        Example 3: Select the patient's group allocation. If unknown, please
        leave blank.
      </legend>
      <InputRadioGroup
        id="example-3"
        :values="['control', 'intervention', 'placebo']"
        :labels="['Healthy controls', 'Experimental cohort', 'Placebo cohort']"
        :show-clear-button="true"
      />
    </fieldset>
  </form>
</template>
