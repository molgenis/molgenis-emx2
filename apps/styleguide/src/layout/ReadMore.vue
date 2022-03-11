<template>
  <div>
    <span v-if="text === 'undefined'"></span>
    <span v-else-if="text.length <= length">{{ text }}</span>
    <span v-else-if="expand">
      {{ text }}
      <a @click.stop.prevent="expand = false" href="#">...show less </a>
    </span>
    <span v-else>
      {{ text.slice(0, length) }}
      <a @click.stop.prevent="expand = true" href="#" title="Expand text">
        ...show more
      </a>
    </span>
  </div>
</template>

<script>
export default {
  props: {
    text: String,
    length: {
      type: Number,
      default: () => 20,
    },
  },
  data() {
    return {
      expand: false,
    };
  },
};
</script>

<docs>
example
```
<ReadMore text="
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
          magna aliqua. Venenatis lectus magna fringilla urna. Etiam tempor orci eu lobortis. Integer quis auctor elit
          sed vulputate mi sit. Lacinia at quis risus sed vulputate odio ut enim blandit. Nibh praesent tristique magna
          sit amet purus. Eleifend donec pretium vulputate sapien nec sagittis. Facilisi morbi tempus iaculis urna id
          volutpat. Ultrices neque ornare aenean euismod
          "/>
```
example with shorter text
```
<ReadMore text="
          Lorem ipsum dolor sit
          "/>
```
</docs>
