<template>
  <h2>Input Text component</h2>
  <p>
    The input text enables you to create an input element that takes user input
    via the keyboard or other device. The available input types are not only
    text, but date, email, number, password, search, etc. However, checkbox and
    radio types are in a separate component.
  </p>
  <div
    class="grid grid-cols-2 gap-6 my-5 [&>div>div]:bg-white [&>div>div]:p-4 [&_h3]:font-semibold [&_h3]:my-2"
  >
    <div>
      <h3>Input text: default use</h3>
      <InputText
        id="search-patients-default"
        :label="label"
        :placeholder="placeholder"
      />
    </div>
    <div>
      <h3>Input text: disabled</h3>
      <InputText
        id="search-patients-disabled"
        :label="label"
        :placeholder="placeholder"
        :disabled="true"
      />
    </div>
    <div>
      <h3>Input Text: as a required field</h3>
      <InputText
        id="search-patients-required"
        :label="label"
        :placeholder="placeholder"
        :required="true"
      >
        <template v-slot:description>
          <span class="text-gray-600/80"
            >Search for patient identifier, affiliation, etc.</span
          >
        </template>
      </InputText>
    </div>
    <div>
      <h3>Input text: positive validation result</h3>
      <InputText
        id="search-patients-valid"
        :label="label"
        :placeholder="placeholder"
        :valid="true"
      />
    </div>
    <div>
      <h3>Input text: negative validation result</h3>
      <InputText
        id="search-patients-error"
        :label="label"
        :placeholder="placeholder"
        error="Search term cannot be blank"
      />
    </div>
    <div>
      <h3>Input text: as a search input</h3>
      <InputText id="type-search" label="Search" type="search" />
    </div>
    <div>
      <h3>Input text: as an email input</h3>
      <InputText id="type-email" label="Enter your email" type="email" />
    </div>
    <div>
      <h3>Input text: as a password input</h3>
      <InputText
        id="type-password"
        label="Enter your password"
        type="password"
      />
    </div>
    <div>
      <h3>Input text: as a telephone input</h3>
      <InputText id="type-tel" label="Enter your telephone number" type="tel" />
    </div>
    <div>
      <h3>Input text: as a url input</h3>
      <InputText id="type-url" label="Enter a URL" type="url" />
    </div>
    <div>
      <h3>Input text: as a numeric input</h3>
      <InputText id="type-number" label="Enter a number" type="number" />
    </div>
    <div>
      <h3>Input text: as a date input</h3>
      <InputText id="type-date" label="Enter a date" type="date" />
    </div>
    <div>
      <h3>Input text: as a datetime local input</h3>
      <InputText
        id="type-datetime-local"
        label="Enter a date and time"
        type="datetime-local"
      />
    </div>
    <div>
      <h3>Input text: as a time input</h3>
      <InputText id="type-time" label="Enter a time" type="time" />
    </div>
  </div>
</template>

<script setup lang="ts">
const label = "Search for patients";
const placeholder = "'A-001' or 'A-001,A-002'";
</script>
