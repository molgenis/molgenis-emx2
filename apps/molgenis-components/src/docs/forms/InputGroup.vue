<template>
  <demo-item id="input-group" label="Input Group">
    <div>
      <label>Pre example</label>
      <InputGroup>
        <template v-slot:prepend>
          <button class="btn btn-outline-primary" type="button">Button</button>
        </template>
        <input id="prepend-input" type="text" class="form-control" />
      </InputGroup>
    </div>

    <div class="mt-3">
      <label>append example</label>
      <InputGroup>
        <template v-slot:append>
          <button class="btn btn-outline-primary" type="button">Button</button>
        </template>
        <input id="append-input" type="text" class="form-control" />
      </InputGroup>
    </div>
  </demo-item>
</template>
<script>
export default {
  data: function () {
    return {
      value: null,
    };
  },
};
</script> 