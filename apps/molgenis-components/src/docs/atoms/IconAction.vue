<template>
  <demo-item id="icon-action" label="Icon Action">
    <label>times</label>
    <div>
      <icon-action icon="times" @click="alert('clicked')"></icon-action>
    </div>
    <label>chevron-down</label>
    <div>
      <icon-action icon="chevron-down" @click="alert('clicked')"></icon-action>
    </div>
    <label>chevron-up</label>
    <div>
      <icon-action icon="chevron-up" @click="alert('clicked')"></icon-action>
    </div>
    <label>snowflake</label>
    <div>
      <icon-action icon="snowflake" @click="alert('clicked')"></icon-action>
    </div>
  </demo-item>
</template>

<script>
export default {
  methods: {
    alert(text) {
      alert(text);
    }
  }
};
</script>