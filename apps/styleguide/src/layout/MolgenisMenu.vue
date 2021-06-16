<template>
  <nav class="navbar bg-primary navbar-expand-lg navbar-dark">
    <button
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
    <a v-if="logo" class="navbar-brand" :href="homeUrl">
      <img :src="logo" alt="brand-logo" height="30" />
    </a>
    <div class="collapse navbar-collapse" id="navbarNav">
      <ul class="navbar-nav" v-if="items">
        <li
          v-for="item in permittedItems"
          :key="item.label"
          class="nav-item"
          :class="{
            active: item.active,
            dropdown: item.submenu,
          }"
        >
          <ButtonDropdown
            class="nav-item"
            v-if="item.submenu && item.submenu.length > 0"
            :label="item.label"
            icon="caret-down"
            isMenuItem="true"
          >
            <a
              v-for="sub in item.submenu"
              class="dropdown-item"
              :href="sub.href"
              :key="sub.label"
              :target="sub.newWindow ? '_blank' : '_self'"
              >{{ sub.label }}</a
            >
          </ButtonDropdown>
          <a
            v-else
            class="nav-link"
            :href="item.href"
            :target="item.newWindow ? '_blank' : '_self'"
            >{{ item.label }}
          </a>
        </li>
      </ul>
    </div>
    <slot />
  </nav>
</template>

<script>
import ButtonDropdown from "../forms/ButtonDropdown";

/** You can use the slot to put a component in the right of menu, e.g. an 'Account' component */
export default {
  components: {
    ButtonDropdown,
  },
  props: {
    /** the navbar items */
    items: Array,
    /** logo to show*/
    logo: String,
    /** session information, so we can check role permissions */
    session: Object,
  },
  computed: {
    permittedItems() {
      return this.items.filter(this.permitted);
    },
    homeUrl() {
      //will be first non-submenu item that is permitted
      if (this.permittedItems.length > 0) {
        let firstItem = this.permittedItems[0];
        // in case it is a item with submenu and without href, find first submenu item
        while (firstItem && firstItem.href == null && firstItem.submenu) {
          let sub = firstItem.submenu.filter((i) => this.permitted(i));
          firstItem = sub[0];
        }
        if (firstItem && firstItem.href) return firstItem.href;
      }
      //defaut: go home
      return "../";
    },
  },
  methods: {
    permitted(item) {
      if (!item.role) {
        return true;
      }
      if (this.session && Array.isArray(this.session.roles)) {
        if (this.session.email == "admin") {
          return true;
        }
        if (item.role == "Viewer") {
          return this.session.roles.some((r) =>
            ["Viewer", "Editor", "Manager", "Owner"].includes(r)
          );
        } else if (item.role == "Editor") {
          return this.session.roles.some((r) =>
            [("Editor", "Manager", "Owner")].includes(r)
          );
        } else if (item.role == "Manager") {
          return this.session.roles.some((r) =>
            ["Manager", "Owner"].includes(r)
          );
        }
      }
      return false;
    },
  },
};
</script>

<docs>
Example
```
<MolgenisMenu logo="assets/img/molgenis_logo.png" :items="[
        {label:'Home',href:'/', active:true},
        {label:'My search',href:'http://google.com'},
        {label:'My movies',href:'http://youtube.com'}
     ]">Something in the slot
</MolgenisMenu>
```
Example with submenu
```
<MolgenisMenu logo="assets/img/molgenis_logo.png" :items="[
        {label:'Home',href:'/', active:true},
        {label:'My search',href:'http://google.com', role:'Manager'},
        {label:'My sub',href:'http://youtube.com', submenu:
          [{label:'My other search',href:'http://bing.com'}]
        }
     ]" :session="{roles:['Viewer']}">Something in the slot
</MolgenisMenu>
```
</docs>
