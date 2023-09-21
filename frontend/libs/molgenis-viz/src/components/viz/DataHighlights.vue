<template>
  <h3 :class="setTitleClassNames">{{ title }}</h3>
  <ul class="data-highlights">
    <li class="data-highlight" v-for="key in Object.keys(data)" key="key">
      <data :value="data[key]" class="data-value">
        <span class="data-label">{{ key }}</span>
      </data>
    </li>
  </ul>
</template>

<script>
// Data highlights are used to display an interesting value for a given
// scenario (e.g., total of *x*, recruit to date, etc.). This component
// is designed to give a quick look &mdash;or highlight of&mdash; variables of
// interest. It is suggested to use this component at the top of a
// dashboard and to display 3 to 5 values. It is not recommended to
// display more than 4 or one value. Titles must be short and consise as
// this element can be rather small. Limit to one or two words max.
//
// @group VISUALISATIONS
export default {
  name: "DataHighlights",
  props: {
    // one or two words that describes the value
    title: {
      type: String,
    },
    // If false, titles will be visually hidden
    showTitle: {
      type: Boolean,
      // `false`
      default: false,
    },
    // An object containing one or more key-value pairs
    data: {
      type: Object,
      required: true,
      validator: (object) => {
        return Object.keys(object).length <= 5;
      },
    },
  },
  computed: {
    setTitleClassNames() {
      const base = "data-highlights-title";
      const visibility = !this.showTitle ? "visually-hidden" : "";
      return [base, visibility].join(" ");
    },
  },
};
</script>

<style lang="scss">
.data-highlights {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 1em;
  list-style: none;
  padding: 0;
  margin: 0;

  .data-highlight {
    box-sizing: border-box;
    padding: 1em;
    background-color: $blue-800;
    border-radius: 6px;
    flex-grow: 1;
    box-shadow: 0 0 4px 3px $gray-transparent-200;

    .data-value {
      .data-label {
        display: block;
        margin-bottom: 21px;
        font-size: 11pt;
        text-transform: uppercase;
        font-weight: bold;
        color: $blue-100;
        letter-spacing: 0.08em;
      }

      &::after {
        content: attr(value);
        display: block;
        font-size: 36pt;
        text-align: center;
        color: $blue-050;
      }
    }
  }
}

.data-highlights-title {
  font-weight: bold;
}
</style>
