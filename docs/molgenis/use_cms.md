# Using the MOLGENIS CMS

> NOTE
> The MOLGENIS CMS is currently in development. In this guide, we've provided an overview of the CMS, but things may change or may not work as described. If you notice any issues, please let us know.

The MOLGENIS CMS&mdash;or _content management system_&mdash;allows users to create custom interfaces within a molgenis instance. Behind the scenes, the CMS is a central schema, which is called `cms` and comes pre-loaded with a few example pages. In the CMS, there are two types of pages that can be created, which can only be created if a user has manager-level permissions.

1. __Landing page__: a simple, drag-and-drop interface where users can create content from a set of standard components (e.g., paragraphs, lists, images, visualisations, etc.)
2. __Developer page__ (advanced): create a page from the scratch using HTML, CSS, and JavaScript.

All CMS pages, by default, are connected to the local MOLGENIS theme (e.g., "light", "dark", etc.), so you do not need to worry about styling pages.

## How to use the MOLGENIS CMS

MOLGENIS CMS pages are always visible in the __page gallery__. To edit a page-

1. Go to your molgenis instance and sign in (must have manager rights)
2. Go to the page gallery at `https://<host>/apps/ui/cms/pages`
3. Find the page you wish to edit and click the corresponding pencil icon.
    - If are editing a landing page, you will be redirected to the __page builder__, which is the drag-and-drop interface
    - For developer pages, the __page editor__ will open and an IDE-like interface will be displayed
4. Start building your page and save changes

In the page builder, all changes are saved automatically. However, you must routinely save changes when using the page editor.

## Troubleshooting

### How do I add data to a chart?

### How do I create a page with my own theme?

You will need to create a new MOLGENIS-tailwind theme and add it to the GitHub repository. It would be best to get in touch with us at [support@molgenis.org](mailto:support@molgenis.org).

