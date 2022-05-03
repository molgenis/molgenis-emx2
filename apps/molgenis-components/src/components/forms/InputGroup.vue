<template>
  <div class="input-group">
    <div class="input-group-prepend">
      <slot name="prepend"></slot>
    </div>

    <slot></slot>

    <div class="input-group-append">
      <slot name="append"></slot>
    </div>
  </div>
</template>

<docs>
<template>
  <div>
    <label>Pre example</label>
    <demo-item>
      <InputGroup>
        <template v-slot:prepend>
          <button class="btn btn-outline-primary" type="button">Button</button>
        </template>
        <input id="prepend-input" type="text" class="form-control"/>
      </InputGroup>
    </demo-item>
    <label>append example</label>
    <demo-item>
      <InputGroup>
        <template v-slot:append>
          <button class="btn btn-outline-primary" type="button">Button</button>
        </template>
        <input id="append-input" type="text" class="form-control"/>
      </InputGroup>
    </demo-item>
  </div>
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
</docs>
