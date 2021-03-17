<template>
    <div>
        <div style="background-color: #f4f4f4; min-height: calc(100vh - 70px)">
            <MolgenisTheme :href="css" />
            <MolgenisTheme
                href="https://fonts.googleapis.com/css?family=Oswald:500|Roboto|Roboto+Mono&display=swap"
            />
            <MolgenisTheme
                href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"
            />
            <MolgenisMenu
                active="My search"
                :items="menu"
                logo="/apps/styleguide/assets/img/molgenis_logo.png"
                :session="session"
            >
                <MolgenisSession v-model="session" />
            </MolgenisMenu>
            <div class="container-fluid p-3" style="padding-bottom: 50px">
                <h1 v-if="title">
                    {{ title }}
                </h1>
                <slot />
            </div>
        </div>
        <Footer>
            <span v-if="session && session.manifest">
                Version:
                <a
                    :href="
                        'https://github.com/molgenis/molgenis-emx2/releases/tag/v' +
                            session.manifest.SpecificationVersion
                    "
                >
                    {{ session.manifest.SpecificationVersion }}
                </a>
            </span>
        </Footer>
    </div>
</template>

<script>
import MolgenisMenu from "./MolgenisMenu.vue";
import MolgenisSession from "./MolgenisSession.vue";
import MolgenisTheme from "./MolgenisTheme.vue";
import Footer from "./MolgenisFooter.vue";
import DefaultMenuMixin from "../mixins/DefaultMenuMixin.vue";

/**
 Provides wrapper for your apps, including a little bit of contextual state, most notably 'account' that can be reacted to using v-model.
 */
export default {
  components: { MolgenisSession, MolgenisMenu, Footer, MolgenisTheme },
  mixins: [DefaultMenuMixin],
  props: {
    menuItems: Array,
    title: String,
  },
  data: function () {
    return {
      session: null,
      cssURL: null,
      fullscreen: false,
    };
  },
  computed: {
    css() {
      if (this.cssURL) return this.cssURL;
      else
        return "/public_html/apps/styleguide/assets/css/bootstrap-molgenis-blue.css";
    },
    menu() {
      if (this.session && this.session.settings && this.session.settings.menu) {
        return this.session.settings.menu;
      } else if (this.menuItems) {
        return this.menuItems;
      } else {
        return this.defaultMenu;
      }
    },
  },
  watch: {
    session: {
      deep: true,
      handler() {
        if (
          this.session != undefined &&
          this.session.settings &&
          this.session.settings.cssURL
        ) {
          console.log("changed url " + this.session.settings.cssURL);
          this.cssURL = this.session.settings.cssURL;
        }
        this.$emit("input", this.session);
      },
    },
  },
  methods: {
    toggle() {
      this.fullscreen = !this.fullscreen;
    },
  },
};
</script>
