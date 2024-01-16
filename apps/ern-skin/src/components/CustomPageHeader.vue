<template>
  <div :class="containerClassNames">
    <div
    v-if="imageSrc"
      :class="`height-${height} header-image`"
      :style="`background-image: url(${imageSrc})`"
    />
    <div class="header-content">
      <h1 class="header-title">{{ title }}</h1>
      <h2 class="header-subtitle" v-if="subtitle">{{ subtitle }}</h2>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  
  // A short title that describes the page
  title: {
    type: String,
    required: true
  },
  
  // A brief description about the page
  subtitle: {
    type: String,
  },
  
  // Location of an image to display
  imageSrc: String,
  
  // specify the height of the image
  height: {
    // `'small' / 'medium' / 'large' / `xlarge / 'full'`
    type: String,
    // `small`
    default: "large",
    validator: (value) => {
      return ["small", "medium", "large", 'xlarge', "full"].includes(value);
    },
  },
    
  // the horizontal position of the title and subtitle
  titlePositionX: {
    // `'left' / 'center' / 'right'`
    type: String,
    // `left`
    default: "left",
    validator: (value) => {
      return ["left", "center", "right"].includes(value);
    },
  },
  // the vertical position of the title and subtitle
  titlePositionY: {
    // `'top' / 'center' / 'bottom'`
    type: String,
    // `center`
    default: "center",
    validator: (value) => {
      return ["top", "center", "bottom"].includes(value);
    },
  },
  
});


const containerClassNames = computed(() => {
  const css = [
    'custom-page-header',
    `text-position-x-${props.titlePositionX} text-position-y-${props.titlePositionY}`,
    `padding-h-${props.horizontalPadding} padding-v-${props.verticalPadding}`
  ]
  
  if (props.imageSrc) {
    css.push('header-image-background');
  }
  
  return css.join(' ');
});

</script>

<style lang="scss">
.custom-page-header {
  position: relative;
  
  .header-image {
    background-size: cover;
    background-position: 0 0;
  }
  
  .header-content {
    width: 90%;
    margin: 0 auto;
    padding: 1em 0;
    
    h1, h2 {
      margin: 0;
      line-height: 1.3;
      color: currentColor;
    }
    
    .header-title {
      font-size: 13pt;
      text-transform: uppercase;
      letter-spacing: 4px;
      font-weight: bold;
    }

    .header-subtitle {
      font-size: 32pt;
      font-weight: 200;
    }
  }
}
</style>