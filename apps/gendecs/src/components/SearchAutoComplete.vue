<template>
  <div class="autocomplete">
    <h2>Select HPO term</h2>
    <InputString v-model="hpoTerms" :list="true" :readonly="this.readOnly"
           @input="onChange"
           @keydown.down="onArrowDown"
           @keydown.up="onArrowUp"
           @keydown.enter="onEnter"
           type="text"
            />
      <div v-if="typeof hpoTerms[hpoTerms.length - 1] === 'undefined'">
      </div>
      <div v-else-if="hpoTerms[hpoTerms.length - 1].length > 4">
        <ul v-show="isOpen" class="autocomplete-results">
          <li
            v-if="isLoading"
            class="loading"
            >
            <div>
              <Spinner/>
            </div>
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
    <ButtonOutline @click="emitResults">Submit HPO Terms</ButtonOutline>
  </div>
</template>


<script>

import {
  Info,
  InputSearch,
  Spinner,
  InputString,
  ButtonOutline
} from "@mswertz/emx2-styleguide";

export default {
  name: "SearchAutoComplete",
  emits: "selectedHpoTerms",
  components: {
    InputSearch,
    Info,
    Spinner,
    InputString,
    ButtonOutline
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
    },
    readOnly: {
      type: Boolean,
      default: false,
    },
  }, // TODO add watcher for automatic emitting?
  data() {
    return {
      hpoTerms: [""],
      hpoTerm: '',
      results: [],
      isOpen: false,
      arrowCounter: -1,
      isLoading: false,
      hpoTermsResults: [],
    };
  },
  mounted() {
    document.addEventListener('click', this.handleClickOutside);
  },
  destroyed() {
    document.removeEventListener('click', this.handleClickOutside);
  },
  methods: {
    /**
     * Function invoked if an HPO term is clicked. It adds the result
     * to this.hpoTerms and this.hpoTermsResults.
     * @param result
     */
    setResult(result) {
      if (this.hpoTermsResults.length >= 1) {
        this.hpoTerms[this.hpoTerms.length - 1] = result;
        this.hpoTerms.push("");
      } else {
        this.hpoTerms[0] = result;
        this.hpoTerms.push("");
      }

      this.isOpen = false;
      this.hpoTermsResults.push(result);
    },
    emitResults() {
      this.$emit('selectedHpoTerms', this.hpoTermsResults);
    },
    filterResults() {
      this.results = this.items.filter(item => item.toLowerCase().indexOf(
          this.hpoTerms[this.hpoTerms.length - 1].toLowerCase()) > -1);
    },
    /**
     * Function that is invoked when a letter is typed in the form and
     * performs some checks to make sure the value is correct when deleting/backspacing.
     * It invokes this.filterResults.
     */
    onChange() {
      if(this.hpoTerms.length - 1 !== this.hpoTermsResults.length) {
        this.hpoTermsResults = this.hpoTerms;
      }
      if (typeof this.hpoTerms[this.hpoTerms.length - 1] === 'undefined') {
        this.hpoTerms[this.hpoTerms.length - 1] = "";
      }
      if (this.hpoTerms[this.hpoTerms.length - 1].length === 0 && this.hpoTerms.length > 1) {
        this.hpoTerms[this.hpoTerms.length - 1] = "";

      }
      this.filterResults();
      this.isOpen = true;
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
      this.hpoTerm = this.results[this.arrowCounter];
      this.arrowCounter = -1;
      this.isOpen = false;
    }
  }
};
</script>

<style scoped>
h2{
  text-align: center;
}
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