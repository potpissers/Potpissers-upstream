#!/bin/bash

scp ./target/Potpissers-default-1.0-SNAPSHOT.jar fedora@158.69.23.87:/home/fedora/minez/
scp ./minez/server.properties fedora@158.69.23.87:/home/fedora/minez/
scp ./semicolon-separated-sqlite.sql fedora@158.69.23.87:/home/fedora/minez/
scp ./minez/startup.sh fedora@158.69.23.87:/home/fedora/minez/
