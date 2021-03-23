<template>
  <span v-if="inplace && !focus" @click="toggleFocus">
    {{ value ? value : "" }}
    <IconAction class="hoverIcon" icon="pencil" />
  </span>
  <FormGroup
    v-else :id="id"
    :help="help"
    :label="label" v-bind="$props"
  >
    <InputAppend
      v-for="(el, idx) in valueArray"
      :key="idx"
      v-bind="$props"
      :show-clear="showClear(idx)"
      :show-minus="showMinus(idx)"
      :show-plus="showPlus(idx)"
      @add="addRow"
      @clear="clearValue(idx)"
    >
      <textarea
        :id="id + idx"
        ref="textarea"
        v-model="valueArray[idx]"
        v-autogrow
        :aria-describedby="id + 'Help'"
        :class="{ 'form-control': true, 'is-invalid': errorMessage }"
        :placeholder="placeholder"
        :readonly="readonly"
        style="resize: none;"
        @input="emitValue($event, idx)"
      />
    </InputAppend>
  </FormGroup>
</template>

<script>
import _baseInput from './_baseInput.vue'
import FormGroup from './_formGroup.vue'
import IconAction from './IconAction.vue'
import InputAppend from './_inputAppend.vue'

/** Input for text */
export default {
  components: {
    FormGroup,
    IconAction,
    InputAppend,
  },
  directives: {
    autogrow: {
      // thank you! https://github.com/wrabit/vue-textarea-autogrow-directive/blob/master/src/VueTextareaAutogrowDirective.js
      inserted(el) {
        let minHeight, observe

        // If used in a component library such as buefy, a wrapper will be used on the component so check if a child is the textarea
        el =
          el.tagName.toLowerCase() === 'textarea'
            ? el
            : el.querySelector('textarea')

        minHeight = parseFloat(
          getComputedStyle(el).getPropertyValue('min-height'),
        )

        if (window.attachEvent) {
          observe = function(element, event, handler) {
            element.attachEvent('on' + event, handler)
          }
        } else {
          observe = function(element, event, handler) {
            element.addEventListener(event, handler, false)
          }
        }

        let resize = () => {
          el.style.height = 'auto'
          let border = el.style.borderTopWidth * 2
          el.style.height =
            (el.scrollHeight < minHeight ? minHeight : el.scrollHeight) +
            border +
            'px'
        }

        // 0-timeout to get the already changed el
        let delayedResize = () => {
          window.setTimeout(resize, 0)
        }

        // When the textarea is being shown / hidden
        var respondToVisibility = (element, callback) => {
          let intersectionObserver = new IntersectionObserver(
            (entries) => {
              entries.forEach((entry) => {
                if (entry.intersectionRatio > 0) callback()
              })
            },
            {
              root: document.documentElement,
            },
          )

          intersectionObserver.observe(element)
        }

        respondToVisibility(el, resize)
        observe(el, 'beforeInput', resize)
        observe(el, 'change', resize)
        observe(el, 'cut', delayedResize)
        observe(el, 'paste', delayedResize)
        observe(el, 'drop', delayedResize)
        observe(el, 'input', resize)

        resize()
      },
      componentUpdated(el) {
        // in case values are changed from outside in
        let minHeight = parseFloat(
          getComputedStyle(el).getPropertyValue('min-height'),
        )

        let resize = () => {
          el.style.height = 'auto'
          let border = el.style.borderTopWidth * 2
          el.style.height =
            (el.scrollHeight < minHeight ? minHeight : el.scrollHeight) +
            border +
            'px'
        }

        resize()
      },
    },
  },
  extends: _baseInput,
}
</script>
