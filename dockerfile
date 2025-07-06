FROM liquibase/liquibase:latest

USER root
RUN apt-get update && apt-get install -y netcat && rm -rf /var/lib/apt/lists/*
USER liquibase
