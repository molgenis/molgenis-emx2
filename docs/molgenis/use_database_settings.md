# Settings

See setting menu.

### New ui (tailwind-based) settings (Alpha non-stable feature)

#### Landing page
Set the landing page with the optional SCHEMA_LANDING_PAGE setting

 - key: ```SCHEMA_LANDING_PAGE```
 - value: a string containing valid json object with the following properties:

```ts
interface Link {
    location: string;
    linkType?: "spaLink" | "internalPageLink" | "externalLink"; // optional, default: "spaLink"
}
```

Link types (```linkType```):
 - ```spaLink``` (default): a page inside the 'ui' app, for example ```/pet%20store/Pet```.
 - ```internalPageLink```: a page on this server outside the 'ui' app, for example ```/directory-demo/directory#/catalogue```.
 - ```externalLink```: a full URL to another site, for example ```https://www.google.com```.

The older ```isSpaLink``` property is no longer read. Settings that use ```"isSpaLink": false``` must change it to ```"linkType": "internalPageLink"``` (or ```"externalLink"``` for a full URL to another site); otherwise the link is treated as a ```spaLink```. The ```link``` property has been renamed to ```location```; settings that still use ```"link"``` must be updated.

example with a page outside of the 'ui' app

```json
{"location": "/directory-demo/directory#/catalogue", "linkType": "internalPageLink"}
```

example with a page inside the 'ui' app

```json
{"location": "/pet%20store/Pet", "linkType": "spaLink"}
```


#### Main menu logo
Set a custom logo in the main menu with the optional logoURL setting

- key: ```logoURL```
- value: a string containing logo location

## Members

## Layout

## Menu


### New ui (tailwind-based) menu (Alpha non-stable feature)
With no configuration, the new ui shows a mix of internal and external links.
Both the default menu and the user-defined menu use the following configuration:

```ts
type Menu = MenuItem[];

interface MenuItem {
  "label": string;
  location: string;
  role?: string;
  key?: string;
  submenu?: Menu;
  linkType?: "spaLink" | "internalPageLink" | "externalLink"; // optional, default: "spaLink"
}
```

The main system menu is defined ui app code

The default schema menu is also defined in the ui app code,
but can be overridden by the user defined menu. 
The user defined menu is stored in the database in the schema settings.
 
**Important**: 
 - The settings key used is ```tw-menu```
 - The settings value should be a valid JSON array
 - Each item's ```linkType``` decides how its ```location``` is used:
   - ```spaLink``` (default): a page inside the 'ui' app. Include the schema, preceded by a slash, for example ```/pet%20store/Pet```.
   - ```internalPageLink```: a page on this server outside the 'ui' app. The app base path is put in front, so leave out the leading slash, for example ```pet%20store/schema```.
   - ```externalLink```: a full URL to another site, used as is, for example ```https://www.google.com```.
 - The older ```isSpaLink``` property is no longer read. Settings that use ```"isSpaLink": false``` must change it to ```"linkType": "internalPageLink"``` (or ```"externalLink"``` for a full URL to another site); otherwise the link is treated as a ```spaLink```. The ```link``` property has been renamed to ```location```; settings that still use ```"link"``` must be updated.

Example: 
```json 
[
  { "label": "Tables", "location": "" },
  {
    "label": "Schema",
    "location": "pet%20store/schema",
    "linkType": "internalPageLink",
    "role": "Viewer"
  },
  {
    "label": "Up/Download",
    "location": "pet%20store/updownload",
    "linkType": "internalPageLink",
    "role": "Viewer"
  },
  {
    "label": "Reports",
    "location": "pet%20store/reports",
    "linkType": "internalPageLink",
    "role": "Viewer"
  },
  {
    "label": "Jobs & Scripts",
    "location": "pet%20store/tasks",
    "linkType": "internalPageLink",
    "role": "Manager"
  },
  { "label": "My Pets", "location": "/pet%20store/Pet" },
  {
    "label": "Help",
    "location": "pet%20store/docs",
    "linkType": "internalPageLink"
  },
  {
    "label": "Manuals",
    "location": "https://www.google.com",
    "linkType": "externalLink"
  }
]
```
