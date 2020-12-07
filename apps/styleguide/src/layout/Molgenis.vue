<template>
  <div>
    <MolgenisTheme :href="css" />
    <MolgenisTheme
      href="https://fonts.googleapis.com/css?family=Oswald:500|Roboto|Roboto+Mono&display=swap"
    />
    <MolgenisTheme
      href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"
    />
    <NavBar
      logo="https://master.dev.molgenis.org/img/Logo_Blue_Small.png"
      active="My search"
      :items="menu"
    >
      <Session v-model="session" />
    </NavBar>
    <div style="background: #fafafa">
      <div
        :class="fullscreen ? 'container-xl' : 'container-fluid'"
        style="background: #fafafa"
        @keydown.esc="toggle"
        :style="
          fullscreen
            ? 'position: absolute; top: 0; right: 0; bottom: 0; left: 0; z-index:1000'
            : 'padding-top: 20px; padding-bottom: 20px;'
        "
      >
        <div class="col-md-12">
          <div class="row">
            <IconAction
              class="fullscreen-icon"
              :icon="fullscreen ? 'compress' : 'expand'"
              @click="toggle"
            />
            <h1 v-if="title">{{ title }}</h1>
          </div>
          <div>
            <slot />
          </div>
        </div>
      </div>
      <div class="row justify-content-md-center">
        <div class="col-md-auto">
          <Footer
            ><span v-if="session && session.manifest"
              >version:
              <a
                :href="
                  'https://github.com/mswertz/molgenis-emx2/releases/tag/v' +
                  session.manifest.SpecificationVersion
                "
                >{{ session.manifest.SpecificationVersion }}</a
              ></span
            >
          </Footer>
        </div>
      </div>
    </div>
    <ShowMore title="debug">
      <pre>
cssURL = {{ cssURL }}

session = {{ session }}
      </pre>
    </ShowMore>
  </div>
</template>

<style>
.fullscreen-icon {
  float: right;
  position: absolute;
  top: 0px;
  right: 0px;
}
</style>

<script>
import NavBar from "./MolgenisMenu";
import Session from "./MolgenisSession";
import MolgenisTheme from "./MolgenisTheme";
import ShowMore from "./ShowMore";
import Footer from "./MolgenisFooter";
import IconAction from "../forms/IconAction";
import DefaultMenuMixin from "../mixins/DefaultMenuMixin";

/**
 Provides wrapper for your apps, including a little bit of contextual state, most notably 'account' that can be reacted to using v-model.
 */
export default {
  components: { Session, NavBar, Footer, MolgenisTheme, ShowMore, IconAction },
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
        console.log("handler");
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

<docs>
```
<template>
  <Molgenis :menuItems="[
        {label:'Home',href:'/'},
        {label:'My search',href:'http://google.com'},
        {label:'My movies',href:'http://youtube.com'}
     ]" title="My title" v-model="molgenis">
    <template>
      <p>Some contents and I can see the molgenis state via v-model = {{ JSON.stringify(molgenis) }}</p>
    </template>
  </Molgenis>
</template>
<script>
  export default {
    data() {
      return {
        molgenis: null
      }
    }
  }
</script>
```
</docs>
