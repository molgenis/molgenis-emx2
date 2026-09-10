# Accessibility guidelines

Web accessibility is the practice that ensures web-based content can be accessed by anyone, regardless of how. Whether you use a screen reader, prefer to use the keyboard to navigate pages, or fall somewhere in between, you can easily access a page's content. This guide will provide an overview on-

- Addressing accessibility by following good semantic HTML practices
- Meeting the [Web Content Accessibility Guidelines (WCAG)](https://www.w3.org/WAI/standards-guidelines/wcag/)
- How to conduct an accessibility review of PRs

To aid in this process, the [Web Content Accessibility Guidelines (WCAG)](https://www.w3.org/WAI/standards-guidelines/wcag/) provide criteria on how to .asdgasdgasdg

## Barriers to accessible content and semantic HTML

The most common barrier in web pages is that they contain poor HTML markup such as using an inappropriate HTML element or creating a custom element instead a native HTML element. For example, it is (unfortunately) fairly common to see a button that redirects users to another page.

```vue
<template>
    <button @click="window.location.href='...'">
        Go to page abc
    </button>
</template>
```

Buttons are only to be used when an action is performed and never act as link. Instead, it is more appropriate to use the anchor element and style it as a button. By using the correct HTML element (i.e., good semantic HTML practices), the user has access to common browser actions such as "open in new tab" or "save to bookmarks" that wouldn't be available if a button is used.

```vue
<template>
    <a href="path/to/some/page" class="my-button-classes">
        Go to page abc
    </a>
</template>

<style>
.my-button-classes {
    /* styles here */
}
</style>
```

In addition, it is important to not only use the correct element, but understand how a user will interact with an element and configure it accordingly. For example, let's say a form has a number input.

```vue
<template>
    <form>
        <legend>Complete your order</legend>
        <label for="quantityInput">Change the quantity</legend>
        <input id="quantityInput"/>
    </form>
</template>
```

In this example, we have an input `quantityInput` but it is a string. As a result, the user cannot increase the quantity by 1 or enter a new value using the keypad. This can be addressed by using the correct `type`.

```diff
- <input id="ageInput"/>
+ <input id="ageInput" type="number" />
```

Assistive devices (and all devices) will now interpret this input as a numeric input and allow users to access the number input on their device. If you were on a mobile device, the number keyboard would display first. If the type was left as is, then the user would have to manually open the keypad via the keyboard. Using the correct input type will also give us access to validation features such as `min`, `max`, `step`, etc. It is also important to point out that the number input should not be used as an input for any numeric values. Instead, use the input type that is appropriate for the input (e.g., `date`, `month`, `time`, `tel`, etc.).

Not only is it important to use the correct HTML element and configure it properly, it is necessary to provide an indication that an HTML element manipulates other HTML elements. For example, let's say our UI has a button that opens a form.

```vue
<template>
  <button @click="openForm()">Open form</button>
  <form>
    <legend>My form</legend>
  </form>
</template>
```

Even though this *works*, it only works *visually*. This means that an assistive device does not know that 1) which elements are controlled by the button, 2) the specific element that is to be opened (in event there are more than one forms), and 3) the current state of the form (open or closed). To fix this, this component would need-

1. Add IDs to the button and form elements
2. Initialize a variable that manages the current state of the form, i.e., open or closed
3. Link the button to the form using `aria-controls` and enter the ID of the form
4. Bind the current state of the form to the button using `aria-expanded`
5. Show and hide the form using CSS

After revising the component, it will now look like this-

```vue
<script setup lang="ts">
import { ref } from "vue";
const formIsOpen = ref<boolean>(false);
</script>

<template>
  <button
    id="openFormButton"
    aria-controls="myForm"
    :aria-expanded="formIsOpen"
    @click="formIsOpen = !formIsOpen"
  >
    Open form
  </button>

  <form id="myForm" :class={ hidden: !formIsOpen }>
    <legend>My Form</legend>
  </form>
</template>
```

These examples should give you insight into good semantic HTML practices that should be followed when developing the MOLGENIS EMX2 frontend. Developers should be aware of-

1. Selecting the appropriate HTML element
2. Configuring the HTML element for a specific use case
3. Connecting HTML elements if they interact with each other

## Web Content Accessibility Guidelines (WCAG)

Even though following semantic HTML practices will help make a page accessible, there are other aspects to consider. There are a number of additional aspects to consider. Some examples are-

- Elements have enough color contrast and follow good visual hierarchy
- Animations can be disabled based on the user's settings (`prefers-reduced-motion`)
- Text is clear, concise and can be understood by non-technical people
- Interactive elements can be accessed using a keyboard and mouse, and they have strong visual distinction when hovered or focused
- Content is responsive and can be viewed on many devices

These aspects and many others are described in the [Web Content Accessibility Guidelines (WCAG)](https://www.w3.org/WAI/standards-guidelines/wcag/). The WCAG is a set of international standards that provide guidance making web-based content accessible. These standards have been adopted by many organizations world wide including governments, universities, and international organizations.

We (MOLGENIS) are aiming to comply with [WCAG 2.2 Level AA](https://www.w3.org/WAI/WCAG22/Understanding/intro). For more information, please view our [Accessibility statement](....).

## How to review a PR for compliance

To ensure all frontend code follows good semantic HTML practices and meetings WCAG 2.2 Level AA criteria, follow these steps.

1. Install the [WAVE Browser Extension](https://wave.webaim.org)
2. Go to the preview and start the WAVE browser extension. Review errors and warnings. Address errors that are related to the aim of the PR. Larger issues should be added to the appropriate epic, story, or in a new issue.
3. Open the vue file(s) in your IDE and check for semantic HTML issues. Some of these many be flagged by the WAVE tool, but it's good to read through the code to make sure.
    - Buttons:
        - Use a button if you need to perform an action such as saving a form or signing in.
        - If a button controls one or more elements, then the aria attributes are defined
    - Links:
        - a link is used if you need to redirect users to another page&mdash;nothing else.
        - If a link needs to look like a button, then style it using CSS instead of using a button component
    - Text: text elements must be written in an HTML text element (e.g., `<h*>`, `<p>`, `<span>`, etc.)
        - Headings: Do not place a heading in an empty div and style it like a button. Use a heading element and follow proper page hierarchy.
        - Pages should not have more than one `<h1>` element.
    - Lists: Lists should be used to provide some organization to a group of related elements. If the order matters, then use the ordered list element (`<ol>`). Otherwise, use an unordered list.
    - State: if a button or input can be disabled, the attribute `disabled` is defined and the relevant styles are applied
    - Interactive elements-
        - can be accessed using a keyboard and mouse
        - have a strong visual distinction when hovered or focused
4. Make sure there are no typescript errors
5. If anything is unclear or if you are unsure if a component meets guidelines, ask for an additional review by a developer who is familiar with the guidelines.

**Note**: By using these tools, it is likely that you may receive unrelated compliance issues or issues that cannot be resolved at this time. In these situations, it is recommended to open a new issue if one doesn't exist.

## Additional Resources

For further information and examples, please consult the following websites.

- [MOLGENIS EMX2 Accessibility Statement](....)
- [WAVE Browser Extension](https://wave.webaim.org)
- [Web Accessibility Initiative](https://www.w3.org/WAI/standards-guidelines/wcag/)
- [The ARIA Patterns guide](https://www.w3.org/WAI/ARIA/apg/patterns/)
- [a11y project](https://www.a11yproject.com)
- [Mozilla's guide on Semantic HTML](https://developer.mozilla.org/en-US/curriculum/core/semantic-html/)
- [Richtlijnen NL Design System (NL)](https://nldesignsystem.nl/richtlijnen/)
- [Digitaal toegankelijk (NL)](https://digitaaltoegankelijk.nl)

---

## Accessibility Statement for MOLGENIS

MOLGENIS is committed to ensuring digital accessibility for people with disabilities. We are continually improving the user experience for everyone, and applying the relevant accessibility standards.

### Conformance status

The [Web Content Accessibility Guidelines (WCAG)](https://www.w3.org/WAI/standards-guidelines/wcag/) defines requirements for designers and developers to improve accessibility for people with disabilities. It defines three levels of conformance: Level A, Level AA, and Level AAA. MOLGENIS is partially conformant with WCAG 2.2 level AA. Partially conformant means that some parts of the content do not fully conform to the accessibility standard.

### Feedback

We welcome your feedback on the accessibility of MOLGENIS. Please let us know if you encounter accessibility barriers on MOLGENIS:

- E-mail: [support@molgenis.org](mailto:support@molgenis.org)
- Postal Address: Genomics Coordination Center, UMCG / University of Groningen, Dept. of Genetics, Antonius Deusinglaan 1, 9713 AV Groningen, The Netherlands

We try to respond to feedback within 5 business days.

### Compatibility with browsers and assistive technology

MOLGENIS is not compatible with:

- browsers older than 2 major versions

### Technical specifications

Accessibility of MOLGENIS relies on the following technologies to work with the particular combination of web browser and any assistive technologies or plugins installed on your computer:

- HTML
- WAI-ARIA
- CSS
- JavaScript

These technologies are relied upon for conformance with the accessibility standards used.

### Limitations and alternatives

Despite our best efforts to ensure accessibility of MOLGENIS, there may be some limitations. Below is a description of known limitations, and potential solutions. Please contact us if you observe an issue not listed below.

1. **MOLGENIS Redesign**: We are currently in the process of redesigning the MOLGENIS. As this process is ongoing, not all features may be available or some aspects may not work as expected. We welcome any feedback if you encounter any issues at [support@molgenis.org](mailto:support@molgenis.org).

### Assessment approach

The MOLGENIS software team assessed the accessibility of MOLGENIS by the following approaches:

- Internal evaluation

### Date

This statement was created on 23 July 2026 using the [W3C Accessibility Statement Generator Tool](https://www.w3.org/WAI/planning/statements/).
