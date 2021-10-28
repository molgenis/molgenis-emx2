/* use this init script if you want to test EMX2 with non-molgenis user */
CREATE USER azure WITH LOGIN NOSUPERUSER INHERIT CREATEROLE ENCRYPTED PASSWORD 'azure';
CREATE DATABASE azure;
GRANT ALL PRIVILEGES ON DATABASE azure TO azure;