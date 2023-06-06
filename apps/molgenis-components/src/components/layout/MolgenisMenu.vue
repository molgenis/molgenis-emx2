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
    <a v-if="logo" class="navbar-brand" href="/">
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
            :isMenuItem="true"
          >
            <a
              v-for="sub in item.submenu"
              class="dropdown-item"
              :href="addBaseUrl(sub.href)"
              :key="sub.label"
              :target="sub.newWindow ? '_blank' : '_self'"
              >{{ sub.label }}</a
            >
          </ButtonDropdown>
          <a
            v-else
            class="nav-link"
            :href="addBaseUrl(item.href)"
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
import ButtonDropdown from "../forms/ButtonDropdown.vue";

/** You can use the slot to put a component in the right of menu, e.g. an 'Account' component */
export default {
  components: {
    ButtonDropdown,
  },
  props: {
    /** the navbar items in format {name:'name', href:'href',role:[], submenu:[]}.
     * If href is null then you will return to baseURL.
     * Href is prefixed with 'baseURL' unless startwith 'http' or '/'.
     * If role then menu items are filtered based on session.roles.
     * Submenu is optional
     */
    items: Array,
    /** logo to show*/
    logo: String,
    /** session information, so we can check role permissions */
    session: Object,
    /** prefix for relative href. Will default to schema name, i.e. first directory in path, e.g. "/pet store/ */
    baseURL: {
      type: String,
      default: () => {
        let path = window.location.pathname.split("/")[1];
        //add trailing slash if path; when in root we return only /
        return "/" + (path ? path + "/" : "");
      },
    },
  },
  computed: {
    permittedItems() {
      return this.items.filter(this.permitted);
    },
    homeUrl() {
      const findFirst = (menu) => {
        return menu.find((item) => {
          //will be first non-submenu item that is permitted
          if (item.href) {
            return item;
          }

          // in case it is a item with submenu and without href, find first submenu item
          if (item.submenu) {
            return findFirst(item.submenu.filter(this.permitted));
          }
        });
      };

      const firstItem = findFirst(this.permittedItems);

      //default: go home
      return firstItem ? this.addBaseUrl(firstItem.href) : this.baseURL;
    },
  },
  methods: {
    addBaseUrl(href) {
      // fully qualified URLs or relative URL navigation supported (although we should deprecate the '../' option
      if (
        href &&
        (href.startsWith("http://") ||
          href.startsWith("https://") ||
          href.startsWith("/") ||
          href.startsWith(".."))
      ) {
        return href;
      } else {
        //relative paths use the baseURL
        return this.baseURL + (href ? href : "");
      }
    },
    permitted(item) {
      if (!item.role) {
        return true;
      }
      if (this.session && Array.isArray(this.session.roles)) {
        if (this.session.email === "admin") {
          return true;
        }
        if (item.role === "Viewer") {
          return this.session.roles.some((r) =>
            ["Viewer", "Editor", "Manager", "Owner"].includes(r)
          );
        } else if (item.role === "Editor") {
          return this.session.roles.some((r) =>
            ["Editor", "Manager", "Owner"].includes(r)
          );
        } else if (item.role === "Manager") {
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
<template>
  <div>Simple example
    <MolgenisMenu logo="assets/img/molgenis_logo_white.png" :items="[
        {label:'Home',href:'', active:true},
        {label:'My search',href:'http://google.com'},
        {label:'My movies',href:'http://youtube.com'}
     ]">Something in the slot
    </MolgenisMenu>
    Example with submenu
    <MolgenisMenu logo="assets/img/molgenis_logo_white.png" :items="[
        {label:'Home',href:'', active:true},
        {label:'My search',href:'http://google.com', role:'Manager'},
        {label:'My sub',href:'http://youtube.com', submenu:
          [{label:'My other search',href:'http://bing.com'}]
        }
     ]" :session="{roles:['Viewer']}">Something in the slot
    </MolgenisMenu>
  </div>
</template>
</docs>
