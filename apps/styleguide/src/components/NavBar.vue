<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
    <button
      @click="toggle"
      class="navbar-toggler"
      type="button"
      data-toggle="collapse"
      data-target="#navbarNav"
      aria-controls="navbarNav"
      aria-expanded="false"
      aria-label="Toggle navigation"
    >
      <span class="navbar-toggler-icon"></span>
    </button>
    <a v-if="logo" class="navbar-brand" href="#">
      <img :src="logo" alt="brand-logo" class="molgenis-navbar-logo" />
    </a>
    <div
      class="collapse navbar-collapse"
      :class="{ show: show }"
      id="navbarNav"
    >
      <ul class="navbar-nav">
        <li
          v-for="item in items"
          :key="item.label"
          :class="['nav-item', item.active ? 'active' : '']"
        >
          <a class="nav-link" :href="item.href">{{ item.label }} </a>
        </li>
      </ul>
    </div>
    <slot />
  </nav>
</template>

<script>
/** You can use the slot to put a component in the right of menu, e.g. an 'Account' component */
export default {
  props: {
    /** the navbar items */
    items: Array,
    /** logo to show*/
    logo: String
  },
  data: function() {
    return {
      show: false
    }
  },
  methods: {
    toggle() {
      this.show = !this.show
    }
  }
}
</script>

<docs>
    Example
    ```
    <NavBar logo="assets/img/molgenis_logo.png" :items="[
        {label:'Home',href:'/', active:true},
        {label:'My search',href:'http://google.com'},
        {label:'My movies',href:'http://youtube.com'}
     ]">Something in the slot
    </NavBar>
    ```
</docs>
