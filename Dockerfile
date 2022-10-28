####
## Build file for server side rendered (ssr) catalogue application for use with molgenis EMX2 backend
###

## Base image to have a node runtime
FROM node:16.18.0-alpine

# Expose $PORT on container.
# We use a varibale here as the port is something that can differ on the environment.
EXPOSE $PORT

# Set host to localhost / the docker image
ENV NUXT_HOST=0.0.0.0

ENV API_PROXY_TARGET=http://host.docker.internal:8080/

## Copy the files need from the contaxt into to image
COPY ./nuxt3-ssr /app/build

WORKDIR /app/build

## Clean files that where that should not have been copied
#RUN rm .env
RUN rm -rf .output
RUN rm -rf .nuxt

## Generate both server and client in production mode
RUN yarn install
RUN npx nuxi clean
RUN npx nuxi prepare
RUN yarn build

RUN mv /app/build/.output /app/.output
RUN mv /app/build/.nuxt /app/.nuxt
RUN rm -rf /app/build/

WORKDIR /app

## Start the server
CMD node .output/server/index.mjs

