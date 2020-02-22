<template>
  <div>
    <nav class="navbar navbar-expand-lg navbar-light bg-light">
      <button
        class="navbar-toggler"
        type="button"
        data-toggle="collapse"
        data-target="#navbarText"
        aria-controls="navbarText"
        aria-expanded="false"
        aria-label="Toggle navigation"
      >
        <span class="navbar-toggler-icon"></span>
      </button>
      <a class="navbar-brand" href="#" @click.prevent="view = 'home'">
        <img
          src="https://master.dev.molgenis.org/img/Logo_Blue_Small.png"
          alt
        />
      </a>
      <div id="navbarText" class="collapse navbar-collapse">
        <ul v-if="schema" class="navbar-nav mr-auto">
          <li class="nav-item" :class="{ active: view === 'explorer' }">
            <a class="nav-link" href="#" @click.prevent="view = 'explorer'"
              >Tables</a
            >
          </li>
          <li class="nav-item">
            <a class="nav-link" href="#" @click.prevent="view = 'schema'"
              >Schema</a
            >
          </li>
          <li class="nav-item">
            <a class="nav-link" href="#" @click.prevent="view = 'import'"
              >Import</a
            >
          </li>
          <li class="nav-item">
            <a
              class="nav-link"
              :href="'/api/playground.html?schema=/api/graphql/' + schema"
              target="_blank"
              >GraphQL</a
            >
          </li>
        </ul>
      </div>
      <Account />
    </nav>
    <div class="container" style="margin-top: 60px;">
      <slot />
    </div>
  </div>
</template>

<script>
import Account from '../organisms/Account.vue'

export default {
  components: {
    Account
  }
}
</script>

<docs>
Example 

```
<Menu />
```
</docs>
