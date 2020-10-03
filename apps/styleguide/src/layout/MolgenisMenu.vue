<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
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
    <a v-if="logo" class="navbar-brand" href="/">
      <img :src="logo" alt="brand-logo" class="molgenis-navbar-logo" />
    </a>
    <div class="collapse navbar-collapse" id="navbarNav">
      <ul class="navbar-nav">
        <li
          v-for="item in items.filter(i => permitted(i))"
          :key="item.label"
          class="nav-item"
          :class="{
            active: item.active,
            dropdown: item.submenu
          }"
        >
          <ButtonDropdown
            class="nav-item"
            v-if="item.submenu"
            :label="item.label"
            icon="caret-down"
          >
            <a
              v-for="sub in item.submenu"
              class="dropdown-item"
              :href="sub.href"
              :key="sub.label"
              >{{ sub.label }}</a
            >
          </ButtonDropdown>
          <a v-else class="nav-link" :href="item.href">{{ item.label }} </a>
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
    ButtonDropdown
  },
  props: {
    /** the navbar items */
    items: Array,
    /** logo to show*/
    logo: String,
    /** session information, so we can check role permissions */
    session: Object
  },
  methods: {
    permitted(item) {
      console.log(
        "item: " +
          JSON.stringify(item) +
          "session: " +
          JSON.stringify(this.session)
      );
      if (!item.role) {
        console.log("no check needed");
        return true;
      }
      console.log(" check needed " + Object.keys(this.session));
      //user admin
      if (this.session) {
        if (this.session.email === "admin") return true;
        //todo, smarter roles system, should have all roles user has than this is peanuts
        if (Array.isArray(this.session.roles)) {
          console.log("checking roles");
          if (item.role == "Viewer") return true;
          else if (
            (item.role == "Editor" && this.session.roles.includes("Editor")) ||
            this.session.roles.includes("Manager")
          )
            return true;
          else if (
            item.role == "Manager" &&
            this.session.roles.includes("Manager")
          )
            return true;
        }
      }
      return false;
    }
  }
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
     ]" :session="{roles:['Manager']}">Something in the slot
</MolgenisMenu>
```
</docs>
