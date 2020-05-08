<template>
  <div>
    <Theme
      href="http://localhost:8080/apps/schema/css/bootstrap-molgenis-blue.css"
    />
    <Theme
      href="https://fonts.googleapis.com/css?family=Oswald:500|Roboto|Roboto+Mono&display=swap"
    />
    <Theme
      href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"
    />
    <NavBar
      logo="https://master.dev.molgenis.org/img/Logo_Blue_Small.png"
      active="My search"
      :items="menuItems"
    >
      <Account v-model="account" />
    </NavBar>
    <div style="background: #fafafa">
      <div class="container" style="padding-top: 60px; padding-bottom: 60px;">
        <div class="row">
          <div class="col-md-12">
            <h1 v-if="title">{{ title }}</h1>
            <slot v-bind:account="account" />
          </div>
        </div>
      </div>
      <div class="row justify-content-md-center">
        <div class="col-md-auto">
          <Footer />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import NavBar from '../components/NavBar'
import Account from './Account'
import Theme from '../components/Theme'

export default {
  components: { Account, NavBar, Theme },
  props: {
    menuItems: Array,
    title: String
  },
  data: function() {
    return {
      account: null
    }
  },
  methods: {}
}
</script>

<docs>
    ```
    <Molgenis :menuItems="[
        {label:'Home',href:'/'},
        {label:'My search',href:'http://google.com'},
        {label:'My movies',href:'http://youtube.com'}
     ]" title="My title">
        <template v-slot:default="slotProps">
            <p>Some contents and I can see the account via slotProps = {{JSON.stringify(slotProps)}}</p>
        </template>
    </Molgenis>
    ```
</docs>
