<template>
  <div class="loading-screen">
    <div class="loading-screen-content">
      <div class="loading-screen-spinner">
        <Cog6ToothIcon class="heroicon cog-6-tooth" />
      </div>
      <p class="loading-message">{{ message }}</p>
    </div>
  </div>
</template>

<script>
import { Cog6ToothIcon } from '@heroicons/vue/24/outline'

// Create a loading screen while some action is being performed
export default {
  props: {
    // Specify the message to display
    message: {
      type: String,
      // `Loading`
      default: 'Loading'
    } 
  },
  components: { Cog6ToothIcon }
}
</script>

<style lang="scss">
@keyframes loadingIconSpin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loading-screen {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100vh;
  background-color: $blue-green-200;
  color: $gray-900;
  opacity: 0.8;
  
  .loading-screen-content {
    width: 90%;
    margin: 0 auto;
    z-index: 1;
    text-align: center;
    
    $icon-size: 62px;
    .heroicon.cog-6-tooth {
      width: $icon-size;
      height: $icon-size;
      transform-origin: center;
      animation: loadingIconSpin 6s linear infinite;
    }
    
    .loading-message {
      font-size: 18pt;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      font-weight: bold;
    }
  }
}
</style>