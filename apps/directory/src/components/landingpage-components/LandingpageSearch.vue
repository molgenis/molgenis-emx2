<template>
  <div class="input-group mb-3 searchbar">
    <input
      type="text"
      class="form-control"
      :placeholder="searchPlaceholder"
      :aria-label="ariaLabel"
      :style="css.searchBar.inputStyle"
      :class="css.searchBar.inputClasses"
    />
    <div class="input-group-append">
      <button
        type="button"
        class="btn search-button"
        :style="css.searchBar.buttonStyle"
        :class="css.searchBar.buttonClasses"
      >
        {{ buttonText }}
      </button>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    css: {
      type: Object,
      required: false,
      default: () => ({
        searchBar: {
          inputStyle: "",
          inputClasses: "border border-dark border-right-0",
          buttonStyle: "",
          buttonClasses:
            "btn-primary search-button border border-dark border-left-0",
        },
      }),
    },
    buttonText: {
      type: String,
      required: true,
    },
    searchPlaceholder: {
      type: String,
      required: true,
    },
    ariaLabel: {
      type: String,
      required: true,
    },
  },
};
</script>
<style scoped>
.search-button {
  min-width: 12rem;
}

.searchbar {
  height: 3rem;
}

.searchbar > input {
  height: 3rem;
  font-size: 1.2rem;
}
</style>
