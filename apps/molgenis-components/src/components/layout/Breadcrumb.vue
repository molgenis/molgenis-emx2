<template>
  <nav aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li
        v-for="(url, label, index) in crumbs"
        :key="label"
        class="breadcrumb-item"
        :class="{ active: label === lastKey }"
      >
        <span v-if="useRouterLink" class="router-link">
          <a v-if="label == lastKey" aria-current="page">{{ label }}</a>
          <router-link v-else :to="url">
            {{ label }}
          </router-link>
        </span>
        <span v-else class="atag">
          <a v-if="label == lastKey" aria-current="page">{{ label }}</a>
          <a v-else :href="url">{{ label }}</a>
        </span>

        <span class="dropdown">
          <span v-if="dropdown && index === 0">
            <i
              class="text-primary dropdown-toggle dropdown-toggle-split pr-0"
              @click="toggleDropdown"
            />
            <div class="dropdown-menu" :class="{ show: showDropdown }">
              <span v-if="useRouterLink" class="router-link">
                <router-link
                  v-for="(url, label, index) in dropdown"
                  class="dropdown-item text-primary"
                  :to="url"
                  :key="`${index}_routerlink`"
                >
                  {{ label }}
                </router-link>
              </span>
              <span v-else class="atag">
                <a
                  v-for="(url, label, index) in dropdown"
                  class="dropdown-item text-primary"
                  :href="url"
                  :key="index"
                >
                  {{ label }}
                </a>
              </span>
            </div>
          </span>
        </span>
      </li>
    </ol>
  </nav>
</template>

<script>
export default {
  name: "Breadcrumb",
  props: {
    /* list of crumbs, map of  {'label':'url'} */
    crumbs: Object,
    /* list of dropdown, map of  {'label':'url'} */
    dropdown: Object,
    useRouterLink: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      showDropdown: false,
    };
  },
  computed: {
    lastKey() {
      return Object.keys(this.crumbs)[Object.keys(this.crumbs).length - 1];
    },
  },
  methods: {
    toggleDropdown() {
      this.showDropdown = !this.showDropdown;
    },
  },
};
</script>

<docs>
<template>
  <div>
    <label>Example without nested</label>
    <breadcrumb
        :crumbs="{ Home: '/', Schema: '/schema', Page: '/schema/page' }"
    />
    <label>Example with nested</label>
    <breadcrumb
        :crumbs="{ Home: '/', Schema: '/schema', Page: '/schema/page' }"
        :dropdown="{ Other: '/other', Other2: '/other2' }"
    />
  </div>
</template>
</docs>
