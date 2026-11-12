# Admin settings

You can find the 'admin' menu when on the MOLGENIS start page, when you view the list of databases (click the MOLGENIS
logo to get there). Only when signed in as 'admin' user will this menu item be shown.

Database settings currently supported:
* LANDING_PAGE - to change landing page from default /apps/central to something else
* locales - to enable internationalization (i18n), experimental. Should be javascript array, default ```["en"]```

Schema settings currently supported:
* menu - will be set by the 'settings' app on database and schema level
* pages - will be set by the pages app, access via settings app
* reports - will be set by the reports app

### New ui (tailwind-based) settings (Alpha non-stable feature)

#### Theme
Go to the 'Theme' tab of the admin page to set the theme of the application. Each theme is a built-in theme plus any CSS variables you changed.

- **Built-in themes** (light, dark, molgenis, umcg, aumc, hdsu and uncan-connect) cannot be changed. Use 'Duplicate and customise' to start a custom theme from one of them.
- **Custom themes** have a name of your choice and can be edited or deleted. A theme that is in use cannot be deleted.
- **Precedence:** a theme set on a database wins over the application theme. Custom themes made here can also be selected in every database.
- 'Let visitors choose' removes the application theme. Visitors then get the light/dark toggle, which is hidden while a theme is set.

If a custom theme makes the page unreadable, add `?theme=light` to the URL to view the page with a built-in theme and change the selection.

The page stores its data in the settings ```tw-theme``` (id of the active theme) and ```tw-custom-themes``` (JSON list of custom themes). Values containing `url()`, `@`, `;`, braces, `<` or backslashes are ignored.

#### Landing page
Set the landing page with the optional SYSTEM_LANDING_PAGE setting

- key: ```SYSTEM_LANDING_PAGE```
- value: a string containing valid json object with the following properties:

```ts
interface Link {
    link: string;
    isSpaLink?: boolean;
}
```

example with external link ( i.e. all links outside of the 'ui' app)

```json
{"link": "/directory-demo/directory#/catalogue", "isSpaLink": false}
```

example with internal link ( i.e. all within of the 'ui' app)

```json
{"link": "pet%20store/Pet", "isSpaLink": true}
```
#### Banner
Set the application banner with the optional SYSTEM_BANNER_HTML setting:

- key: ```SYSTEM_BANNER_HTML```
- value: a string containing just simple text or valid HTML code to be displayed in the banner on top of the page. 

If not set or an empty string, no banner will be shown.

Please note that most standard HTML style tags are rendered without their default browser styling (e.g. h1 is not shown as a large bold heading by default). 
To apply custom styling, an explicit style element with CSS should be included in the value and applied to the text. 

for example:
```html
<style>
    .warning {
        width: 100%;
        background-color: #ec6707;
        line-height: 2;
        text-align: center;
        color: white;
        font-weight: bold;
        font-size: 20px;
            }
</style>
    <span class="warning">Warning banner example</span>
```

or just hook into the tailwind system:

```html
<div role="banner" class="p-7.5 w-[90%] bg-warning">
    <h2 class="text-body-base">Warning</h2>
    <p class="text-center">This is a tailwind styled banner</p>
</div>
```


## User management

Currently you can use the admin menu to view the users currently registered in the system. In addition you can create a
new user and/or set a user password.

## Disable the pet store database

By default, the _pet store_ demonstration database is loaded when MOLGENIS is started for the first time to ensure the database connection is working as intended.
Admins can disable this feature using the Java environment variable `MOLGENIS_EXCLUDE_PETSTORE_DEMO`.
This variable is `FALSE` by default.
Setting it to `TRUE` will disable the automatic loading of the _pet store_.
To disable, the environment variable should be set as follows: `MOLGENIS_EXCLUDE_PETSTORE_DEMO=TRUE`.
