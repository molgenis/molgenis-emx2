import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    account: {
      email: null
    },
    version: process.env.PACKAGE_VERSION
  },
  mutations: {
    signin (state, email) {
      state.account.email = email
    },
    signout (state) {
      state.account.email = null
    }
  },
  getters: {
    appVersion: (state) => {
      return state.packageVersion
    }
  }
})
