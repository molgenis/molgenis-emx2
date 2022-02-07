FROM node:lts-gallium

COPY ./nuxt-ssr /app/nuxt-ssr
COPY ./molgenis-components /app/molgenis-components
COPY ssr-package.json /app

WORKDIR /app

RUN ls

RUN mv ssr-package.json package.json

RUN yarn install

WORKDIR /app/nuxt-ssr

RUN ls

RUN yarn build

# Expose $PORT on container.
# We use a varibale here as the port is something that can differ on the environment.
EXPOSE $PORT

# Set host to localhost / the docker image
ENV NUXT_HOST=0.0.0.0

# Set app port
ENV NUXT_PORT=$PORT

# Set the base url
ENV PROXY_API=$PROXY_API

# Set the browser base url
ENV PROXY_LOGIN=$PROXY_LOGIN

CMD yarn start

