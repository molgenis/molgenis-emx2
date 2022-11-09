####
## Build file for server side rendered (ssr) catalogue application for use with molgenis EMX2 backend
###

## Base image to have a node runtime
FROM node:18.12.0-alpine

## Copy the files need from the contaxt into to image
COPY ./nuxt3-ssr/.nuxt /
COPY ./nuxt3-ssr/.output /

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
CMD node .output/server/index.mjs

