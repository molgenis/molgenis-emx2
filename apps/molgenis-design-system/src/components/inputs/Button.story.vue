<template>
  <Story title="Inputs/Buttons" :layout="{ type: 'grid', width: '400x' }">
    <Variant title="Primary">
      <Button label="Call to action" type="primary" size="tiny" />
      <br />
      <Button label="Call to action" type="primary" size="small" />
      <br />
      <Button label="Call to action" type="primary" size="medium" />
      <br />
      <Button label="Call to action" type="primary" size="large" />
    </Variant>
    <Variant title="Secondary">
      <Button label="Call to action" type="secondary" size="tiny" />
      <br />
      <Button label="Call to action" type="secondary" size="small" />
      <br />
      <Button label="Call to action" type="secondary" size="medium" />
      <br />
      <Button label="Call to action" type="secondary" size="large" />
    </Variant>
    <Variant title="Tertiary">
      <Button label="Call to action" type="tertiary" size="tiny" />
      <br />
      <Button label="Call to action" type="tertiary" size="small" />
      <br />
      <Button label="Call to action" type="tertiary" size="medium" />
      <br />
      <Button label="Call to action" type="tertiary" size="large" />
    </Variant>
    <Variant title="Outline">
      <Button label="Call to action" type="outline" size="tiny" />
      <br />
      <Button label="Call to action" type="outline" size="small" />
      <br />
      <Button label="Call to action" type="outline" size="medium" />
      <br />
      <Button label="Call to action" type="outline" size="large" />
    </Variant>
    <Variant title="Disabled">
      <Button label="Call to action" type="disabled" size="tiny" />
      <br />
      <Button label="Call to action" type="disabled" size="small" />
      <br />
      <Button label="Call to action" type="disabled" size="medium" />
      <br />
      <Button label="Call to action" type="disabled" size="large" />
    </Variant>
  </Story>
</template>
<script setup>
import Button from "./Button.vue";
</script>
