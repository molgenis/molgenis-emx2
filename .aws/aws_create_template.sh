#!/bin/bash
# init script for quickly setting up an emx2 on a vanilla ubuntu, tested on xenial-16.04
# we use this template in the aws_start_preview_from_build_script

## create an instance
aws ec2 run-instances --image-id ami-09042b2f6d07d164a --count 1 --instance-type t2.micro --key-name mswertz --security-group-ids sg-034a0713c918bb42f --subnet-id subnet-093e36ba6d1b5f420 --region eu-central-1 --instance-initiated-shutdown-behavior terminate

# add repositories for postgresql and java
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
echo "deb http://apt.postgresql.org/pub/repos/apt/ `lsb_release -cs`-pgdg main" |sudo tee  /etc/apt/sources.list.d/pgdg.list
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

# install postgresql and java
sudo apt update
sudo apt install postgresql-13 postgresql-client-13 temurin-17-jdk -y

# config postgresql
sudo -u postgres psql -c "create database molgenis;"
sudo -u postgres psql -c "create user molgenis with login nosuperuser inherit createrole encrypted password 'molgenis';"
sudo -u postgres psql -c "grant all privileges on database molgenis to molgenis;"