#FROM eclipse-temurin:21-jre-noble
#FROM ubuntu:24.10
FROM alpine:latest


RUN apk add --no-cache \
  fontconfig \
  ttf-dejavu\
  gnupg\
  ca-certificates \
  p11-kit-trust \
  musl-locales\
  musl-locales-lang \
  binutils \
  tzdata \
  coreutils \
  openssl \
  openjdk21 \
  python3 \
  py3-pip \
  py3-virtualenv 



#RUN apk add --no-cache ruby ruby-dev build-base && gem install sass
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8080
#RUN apt-get update && apt-get install openjdk-21-jre-headless python3 python3-pip python3-venv -y
RUN pip3 install setuptools --break-system-packages
#RUN useradd -m molgenis
RUN addgroup -S molgenis && adduser -S molgenis -G molgenis

#USER molgenis
ENTRYPOINT ["java","-jar","app.jar"]
