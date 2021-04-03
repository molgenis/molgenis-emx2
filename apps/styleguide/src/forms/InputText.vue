<template>
  <span v-if="inplace && !focus" @click="toggleFocus">
    {{ value ? value : "" }}
    <IconAction class="hoverIcon" icon="pencil" />
  </span>
  <FormGroup
    v-else
    :id="id"
    :label="label"
    :description="description"
    v-bind="$props"
  >
    <InputAppend
      v-for="(el, idx) in valueArray"
      :key="idx"
      v-bind="$props"
      :showClear="showClear(idx)"
      @clear="clearValue(idx)"
      :showPlus="showPlus(idx)"
      :showMinus="showMinus(idx)"
      @add="addRow"
    >
      <textarea
        v-autogrow
        ref="textarea"
        :id="id + idx"
        v-model="valueArray[idx]"
        :class="{ 'form-control': true, 'is-invalid': errorMessage }"
        :aria-describedby="id + 'Help'"
        :placeholder="placeholder"
        :readonly="readonly"
        @input="emitValue($event, idx)"
        style="resize: none"
      />
    </InputAppend>
  </FormGroup>
</template>

<script>
import _baseInput from "./_baseInput.vue";
import InputAppend from "./_inputAppend";
import FormGroup from "./_formGroup";
import IconAction from "./IconAction";

/** Input for text */
export default {
  extends: _baseInput,
  components: {
    InputAppend,
    FormGroup,
    IconAction,
  },
  directives: {
    autogrow: {
      //thank you! https://github.com/wrabit/vue-textarea-autogrow-directive/blob/master/src/VueTextareaAutogrowDirective.js
      inserted(el) {
        let observe, minHeight;

        // If used in a component library such as buefy, a wrapper will be used on the component so check if a child is the textarea
        el =
          el.tagName.toLowerCase() === "textarea"
            ? el
            : el.querySelector("textarea");

        minHeight = parseFloat(
          getComputedStyle(el).getPropertyValue("min-height")
        );

        if (window.attachEvent) {
          observe = function (element, event, handler) {
            element.attachEvent("on" + event, handler);
          };
        } else {
          observe = function (element, event, handler) {
            element.addEventListener(event, handler, false);
          };
        }

        let resize = () => {
          el.style.height = "auto";
          let border = el.style.borderTopWidth * 2;
          el.style.height =
            (el.scrollHeight < minHeight ? minHeight : el.scrollHeight) +
            border +
            "px";
        };

        // 0-timeout to get the already changed el
        let delayedResize = () => {
          window.setTimeout(resize, 0);
        };

        // When the textarea is being shown / hidden
        var respondToVisibility = (element, callback) => {
          let intersectionObserver = new IntersectionObserver(
            (entries) => {
              entries.forEach((entry) => {
                if (entry.intersectionRatio > 0) callback();
              });
            },
            {
              root: document.documentElement,
            }
          );

          intersectionObserver.observe(element);
        };

        respondToVisibility(el, resize);
        observe(el, "beforeInput", resize);
        observe(el, "change", resize);
        observe(el, "cut", delayedResize);
        observe(el, "paste", delayedResize);
        observe(el, "drop", delayedResize);
        observe(el, "input", resize);

        resize();
      },
      componentUpdated(el) {
        //in case values are changed from outside in
        let minHeight = parseFloat(
          getComputedStyle(el).getPropertyValue("min-height")
        );

        let resize = () => {
          el.style.height = "auto";
          let border = el.style.borderTopWidth * 2;
          el.style.height =
            (el.scrollHeight < minHeight ? minHeight : el.scrollHeight) +
            border +
            "px";
        };

        resize();
      },
    },
  },
};
</script>

<docs>
Example
```
<template>
  <div>
    <LayoutForm>
      <InputText
          v-model="value"
          label="My text label"
          placholder="type here your text"
          description="Some help needed?"
      />
    </LayoutForm>
    <br/>
    You typed: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: null
      };
    }
  };
</script>
```
Example with default value
```
<template>
  <div>
    <LayoutForm>
      <InputText
          v-model="value"
          :defaultValue="value"
          label="My text label"
          placholder="type here your text"
          description="Some help needed?"
      />
    </LayoutForm>
    <br/>
    You typed: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: "some default value here"
      };
    }
  };
</script>
```
Example with list
```
<template>
  <div>
    <LayoutForm>
      <InputText
          v-model="value"
          :list="true"
          label="My text label"
          placholder="type here your text"
          description="Some help needed?"
      />
    </LayoutForm>
    <br/>
    You typed: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: null
      };
    }
  };
</script>
```
Example with inplace
```
<template>
  <div>
    <LayoutForm>
      <InputText
          v-model="value"
          :inplace="true"
          label="My text label"
          placholder="type here your text"
          description="Some help needed?"
      />
    </LayoutForm>
    <br/>
    You typed: {{ value }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: null
      };
    }
  };
</script>
```
</docs>
