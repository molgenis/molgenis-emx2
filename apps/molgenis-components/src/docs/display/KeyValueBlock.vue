<template>
  <demo-item id="key-value-block" label="Key value block">
    <label class="font-italic"
      >single item example ( label is hidden or used as heading )</label
    >
    <key-value-block
      heading="my heading"
      :items="[{ key: 'key1', label: 'label 1', value: 'value 1' }]"
    ></key-value-block>

    <label class="font-italic">multiple items example</label>
    <key-value-block
      heading="my heading"
      :items="[
        { key: 'key1', label: 'label 1', value: 'value 1' },
        { key: 'key2', label: 'label 2', value: 'value 2' },
      ]"
    ></key-value-block>
  </demo-item>
</template>
