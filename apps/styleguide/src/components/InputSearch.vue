<template>
  <form-group v-bind="$props">
    <div class="input-group">
      <span class="input-group-prepend">
        <button class="btn border-right-0 border" type="button">
          <i class="fa fa-search"></i>
        </button>
      </span>
      <input
        :id="id"
        v-model="value"
        class="form-control border-left-0 border"
        type="search"
        :placeholder="placeholder"
      />
    </div>
  </form-group>
  <!--
  <div class="form-group has-search">
    <span class="fa fa-search form-control-feedback"></span>
    <input type="text" class="form-control" placeholder="Search" v-model="value" />
  </div>-->
</template>

<script>
import _baseInput from './_baseInput.vue'

export default {
  extends: _baseInput,
  data: () => {
    return { value: null }
  }
}
</script>

<style scoped>
.has-search .form-control {
  padding-left: 2.375rem;
}

.has-search .form-control-feedback {
  position: fixed;
  display: block;
  width: 2.375rem;
  height: 2.375rem;
  line-height: 2.375rem;
  text-align: center;
  pointer-events: none;
  color: #aaa;
}
</style>

<docs>
  Example:
  ```
  <template>
  <div>
    <InputSearch v-model="searchTerms" />
    Search terms: {{searchTerms}}
  </div>
</template>
<script>
export default {
  data: function() {
    return {
      searchTerms: null
    };
  }
};
</script>

```
</docs>
