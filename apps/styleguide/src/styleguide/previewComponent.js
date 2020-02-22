import store from '../store'

// for vue styleguidist
export default previewComponent => {
  // https://vuejs.org/v2/guide/render-function.html
  return {
    store,
    render (createElement) {
      return createElement(previewComponent)
    }
  }
}
