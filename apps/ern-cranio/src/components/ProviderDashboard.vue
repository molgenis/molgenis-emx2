<template>
  <div class="provider-display">
    <slot></slot>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

const route = useRouter();
const path = ref(route.currentRoute.value.href);

onMounted(() => {
  const links = document.querySelectorAll(".navlinks li a");
  links.forEach(link => {
    if (link.getAttribute('href') === path.value) {
      link.parentElement.classList.add('link-selected');
    } else {
      link.parentElement.classList.remove('link-selected');
    }
  });
});

</script>