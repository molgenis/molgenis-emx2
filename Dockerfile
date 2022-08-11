####
## Build file for server side rendered (ssr) catalogue application for use with molgenis EMX2 backend
###

## Base image to have a node runtime ( todo replace with smaller/minimal image)
FROM node:lts-gallium

## Copy the files need from the contaxt into to image
COPY ./nuxt-ssr /app/nuxt-ssr
COPY ./molgenis-components /app/molgenis-components
COPY ssr-package.json /app
COPY yarn.lock /app

WORKDIR /app

## Rename to default name ( alt name needed to avoid multiple package.json in same gradle-docker context)
RUN mv ssr-package.json package.json

## Install the dependencies in the image
RUN yarn install

## Make the components css available to the nuxt server
COPY ./molgenis-components/dist/style.css /app/nuxt-ssr/assets/css

## Copy the same lock file used in main build to ssr build context
COPY yarn.lock /app/nuxt-ssr

WORKDIR /app/nuxt-ssr

## Generate both server and client in production mode
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

## Start the server
CMD yarn start

