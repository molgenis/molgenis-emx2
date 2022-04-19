<template>
  <demo-item id="input-radio" label="Input Radio">
    <label>Basic example</label>
    <div>
      <InputRadio
        id="input-radio-1"
        v-model="value1"
        :options="['options 1', 'option 2']"
        label="My radio input label"
        description="Some help needed?"
      />
      <div>You selected: {{ value1 }}</div>
    </div>

    <div class="mt-3">
      <label>Example with defaultValue</label>
      <div>
        <InputRadio
          id="input-radio-2"
          v-model="value2"
          :options="['option 1', 'option 2']"
          label="My radio input label"
        />
        <div>You selected: {{ value2 }}</div>
      </div>
    </div>
  </demo-item>
</template>
<script>
export default {
  data: function () {
    return {
      value1: null,
      value2: 'option 1',
    };
  },
};
</script>