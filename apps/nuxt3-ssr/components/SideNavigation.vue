<script setup>
const route = useRoute();
defineProps({
  title: {
    type: String,
  },
  image: {
    type: String,
  },
});
function setSideMenuStyle(hash) {
  if (hash == route.hash) {
    return "w-full block my-2 border-l-4 menu-active pl-4 font-bold hover:cursor-pointer";
  } else {
    return "w-full block my-2 hover:font-bold hover:cursor-pointer";
  }
}
</script>

<template>
  <nav
    class="text-body-base bg-white rounded-t-3px rounded-b-50px px-12 py-16 shadow-primary"
  >
    <div v-if="title || image" class="mb-6 font-display text-heading-4xl">
      <h2 v-if="title">{{ title }}</h2>
      <img v-if="image" :src="image" />
    </div>
    <ul>
      <li>
        <a href="#Description" :class="setSideMenuStyle('#Description')">
          Description
        </a>
      </li>
      <li>
        <a href="#GeneralDesign" :class="setSideMenuStyle('#GeneralDesign')">
          General design
        </a>
      </li>
      <li>
        <a href="#Contributors" :class="setSideMenuStyle('#Contributors')">
          Contact &amp; contributors
        </a>
      </li>
      <li>
        <a href="#Variables" :class="setSideMenuStyle('#Variables')">
          Variables &amp; topics
        </a>
      </li>
      <li>
        <a href="#AvailableData" :class="setSideMenuStyle('#AvailableData')">
          Available data &amp; samples
        </a>
      </li>
      <li>
        <a href="#Subpopulations" :class="setSideMenuStyle('#Subpopulations')">
          Subpopulations
        </a>
      </li>
      <li>
        <a
          href="#CollectionEvents"
          :class="setSideMenuStyle('#CollectionEvents')"
        >
          Collection events
        </a>
      </li>
      <li>
        <a href="#Networks" :class="setSideMenuStyle('#Networks')">
          Networks
        </a>
      </li>
      <li>
        <a href="#Partners" :class="setSideMenuStyle('#Partners')">
          Partners
        </a>
      </li>
    </ul>
  </nav>
</template>
