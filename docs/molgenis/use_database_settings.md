# Settings

See setting menu.

## Members

## Layout

## Menu


### New ui (tailwind based) menu
With no configuration, the new ui shows a fix of internal and external links .
Both the default menu and the user defined menu use the following configuration:

```ts
type Menu = MenuItem[];

interface MenuItem {
  "label": string;
  link: string;
  role?: string;
  key?: string;
  submenu?: Menu;
  isSpaLink?: boolean;
}
```

The main system menu is defined ui app code

The default schema menu is also defined in the ui app code,
but can be overridden by the user defined menu. 
The user defined menu is stored in the database in the schema settings.
 
**Important**: 
 - The settings key used is ```tw-menu```
 - The settings value should be a valid JSON array
 - Internal links (links to other pages in the ui) should have the property ```isSpaLink``` set to true, otherwise the link will be treated as an external link and opened in a new tab. and so should not have a schema prefix
 - External links should have the property ```isSpaLink``` set to false, and should have a schema prefix if they are related to a specific schema. The schema prefix is the name of the schema followed by a slash, for example: ```pet%20store/schema```. If the link is not related to a specific schema, it should not have a schema prefix, for example: ```https://www.google.com```.

Example: 
```json 
[
  { "label": "Tables", "link": "", "isSpaLink": true },
  {
    "label": "Schema",
    "link": "pet%20store/schema",
    "isSpaLink": false,
    "role": "Viewer"
  },
  {
    "label": "Up/Download",
    "link": "pet%20store/updownload",
    "isSpaLink": false,
    "role": "Viewer"
  },
  {
    "label": "Reports",
    "link": "pet%20store/reports",
    "isSpaLink": false,
    "role": "Viewer"
  },
  {
    "label": "Jobs & Scripts",
    "link": "pet%20store/tasks",
    "isSpaLink": false,
    "role": "Manager"
  },

  {
    "label": "Help",
    "link": "pet%20store/docs",
    "isSpaLink": false
  }
]
```