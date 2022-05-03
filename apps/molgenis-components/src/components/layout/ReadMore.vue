<template>
  <div>
    <span v-if="text === 'undefined'"></span>
    <span v-else-if="text.length <= length">{{ text }}</span>
    <span v-else-if="expand">
      {{ text }}
      <a @click.stop="expand = false" href="#">...show less </a>
    </span>
    <span v-else>
      {{ text.slice(0, length) }}
      <a @click.stop="expand = true" href="#" title="Expand text">
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
<template>
  <demo-item>
    <read-more
        text="I'm baby food truck chia asymmetrical venmo, crucifix hexagon whatever. +1 hammock live-edge la croix mumblecore semiotics. Brunch mumblecore vape, cardigan leggings iceland blue bottle single-origin coffee migas authentic typewriter polaroid slow-carb seitan. Chicharrones normcore YOLO, XOXO occupy bicycle rights vexillologist schlitz. Quinoa yuccie typewriter, raw denim offal helvetica williamsburg jean shorts cred roof party affogato post-ironic pour-over."
    ></read-more>
  </demo-item>
</template>
</docs>
