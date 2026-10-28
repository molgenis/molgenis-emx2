# Local Matomo for development

The docker compose file in this directory starts a Matomo instance with a MariaDB database. Use it during development
to check that EMX2 sends AI chatbot visits to Matomo. Once started, Matomo is available on <http://localhost:8081>.

## 1. Start Matomo

Spin up the containers in the docker compose file in this directory:

```bash
docker compose up -d
```

## 2. Install Matomo

The first time Matomo starts, it has to be installed through its setup wizard.

1. Wait until <http://localhost:8081> shows the Matomo installation page.
2. Open <http://localhost:8081> and go through the wizard
    - **Database**: the database settings are filled in from the docker compose file, so these can be left as is.
    - **Superuser**: for login and password, use for example `admin` and `secret`. Email can be made up: test@foo.bar
    - **Website**: website name can be emx2 and url http://localhost:8080. Not an Ecommerce site.
3. After the installation wizard, you can log in using the superuser. After that, skip choosing a tracking method.

The website created in the wizard gets site ID `1`.

## 3. Connect EMX2 to Matomo

Start EMX2 with the following settings (as environment variables or JVM options):

```bash
MATOMO_URL=http://localhost:8081
# Optionally
MATOMO_SITE_ID=1
```

Only requests with the User-Agent of a known AI chatbot are tracked. To try it out, send a request that pretends to be
one, for example:

```bash
curl -A "Mozilla/5.0 (compatible; ChatGPT-User/1.0; +https://openai.com/bot)" http://localhost:8080/
```

The visit shows up in Matomo under **AI Assistants -> AI Chatbots Real-time**.
