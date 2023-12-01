<template>
  <Story title="Inputs/Text Area" :layout="{type: 'grid', width: '400px'}">
    <Variant title="Default">
      <InputTextArea id="patient-history" label="Enter patient history"/>
    </Variant>
    <Variant title="With Description">
      <InputTextArea id="patient-history" label="Enter patient history">
        <template v-slot:description>
          <p class="text-gray-600/80">Patients with....</p>
        </template>
      </InputTextArea>
    </Variant>
    <Variant title="Required">
      <InputTextArea id="patient-history" label="Enter patient history" :required="true"/>
    </Variant>
    <Variant title="Valid">
      <InputTextArea id="patient-history" label="Enter patient history" :valid="true"/>
    </Variant>
    <Variant title="Error">
      <InputTextArea id="patient-history" label="Enter patient history" error="Field cannot be blank"/>
    </Variant>
    <Variant title="Disabled">
      <InputTextArea id="patient-history" label="Enter patient history" :disabled="true"/>
    </Variant>
    <Variant title="With placeholder">
      <InputTextArea id="patient-history" label="Enter patient history" placeholder="Some text here"/>
    </Variant>
    <Variant title="Specifying Text Area rows">
      <InputTextArea id="patient-history" label="Enter patient history" :rows="6"/>
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import InputTextArea from './InputTextArea.vue';
</script>