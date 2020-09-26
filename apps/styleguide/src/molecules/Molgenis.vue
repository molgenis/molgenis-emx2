<template>
  <div>
    <Theme :href="css" />
    <Theme
      href="https://fonts.googleapis.com/css?family=Oswald:500|Roboto|Roboto+Mono&display=swap"
    />
    <Theme
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
      <div class="container" style="padding-top: 60px; padding-bottom: 60px;">
        <div class="row">
          <div class="col-md-12">
            <h1 v-if="title">{{ title }}</h1>
            <slot />
          </div>
        </div>
      </div>
      <div class="row justify-content-md-center">
        <div class="col-md-auto">
          <Footer />
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

<script>
import NavBar from "../components/NavBar";
import Session from "./Session";
import Theme from "../components/Theme";
import ShowMore from "../components/ShowMore";

/**
 Provides wrapper for your apps, including a little bit of contextual state, most notably 'account' that can be reacted to using v-model.
 */
export default {
  components: { Session, NavBar, Theme, ShowMore },
  props: {
    menuItems: Array,
    title: String
  },
  data: function() {
    return {
      session: null,
      cssURL: null
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
      } else {
        return this.menuItems;
      }
    }
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
      }
    }
  },
  methods: {}
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
