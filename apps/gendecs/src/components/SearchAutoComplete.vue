<template>
  <div class="autocomplete">
    <InputSearch v-model="HpoTerm"
           @input="onChange"
           @keydown.down="onArrowDown"
           @keydown.up="onArrowUp"
           @keydown.enter="onEnter"
           type="text"/>
    <ul v-show="isOpen" class="autocomplete-results">
      <li
        v-if="isLoading"
        class="loading"
        >
        Loading results...
      </li>
      <li v-else
          v-for="(result, i) in results" :key="i"
          @click="setResult(result)"
          class="autocomplete-result"
          :class="{ 'is-active': i === arrowCounter }">
        {{ result }}
      </li>
    </ul>
  </div>
</template>


<script>

import {InputSearch} from "@mswertz/emx2-styleguide";

export default {
  name: "SearchAutoComplete",
  emits: "selectedHpoTerm",
  components: {
    InputSearch
  },
  props: {
    items: {
      type: Array,
      required: false,
      default: () => [],
    },
    isAsync: {
      type: Boolean,
      required: false,
      default: false,
    }
  },
  data() {
    return {
      HpoTerm: '',
      results: [],
      isOpen: false,
      arrowCounter: -1,
      isLoading: false
    };
  },
  watch: {
    items: function(value, oldValue) {
      if (value.length !== oldValue.length) {
        this.results = value;
        // this.isOpen = true;
        this.isLoading= false;
      }
    }
  },
  mounted() {
    document.addEventListener('click', this.handleClickOutside);
  },
  destroyed() {
    document.removeEventListener('click', this.handleClickOutside);
  },
  methods: {
    setResult(result) {
      this.HpoTerm = result;
      this.isOpen = false;
      this.$emit('selectedHpoTerm', result);
    },
    filterResults() {
      this.results = this.items.filter(item => item.toLowerCase().indexOf(this.HpoTerm.toLowerCase()) > -1);
    },
    onChange() {
      // this.$emit('input', this.HpoTerm);

      if (this.isAsync) {
        this.isLoading = true
      } else {
        this.filterResults();
        this.isOpen = true;
      }
    },
    handleClickOutside(event) {
      if (!this.$el.contains(event.target)) {
        this.arrowCounter = -1;
        this.isOpen = false;
      }
    },
    onArrowDown() {
      if (this.arrowCounter < this.results.length) {
        this.arrowCounter = this.arrowCounter + 1;
      } else {
        this.arrowCounter = 0;
      }
    },
    onArrowUp() {
      if (this.arrowCounter > 0) {
        this.arrowCounter = this.arrowCounter - 1;
      } else {
        this.arrowCounter = this.results.length -1;
      }
    },
    onEnter() {
      this.HpoTerm = this.results[this.arrowCounter];
      this.arrowCounter = -1;
      this.isOpen = false;
    }
  }

};
</script>

<style scoped>
.autocomplete {
  position: relative;
}

.autocomplete-results {
  padding: 0;
  margin: 0;
  border: 1px solid #eeeeee;
  height: 120px;
  min-height: 1em;
  max-height: 6em;
  overflow: auto;
}

.autocomplete-result {
  list-style: none;
  text-align: left;
  padding: 4px 2px;
  cursor: pointer;
}
.autocomplete-result.is-active,
.autocomplete-result:hover {
  background-color: #4AAE9B;
  color: white;
}
</style>