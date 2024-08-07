#!/bin/bash

scp ./target/Potpissers-default-1.0-SNAPSHOT.jar fedora@158.69.23.87:/home/fedora/kollusion/
scp ./kollusion/server.properties fedora@158.69.23.87:/home/fedora/kollusion/
scp ./semicolon-separated-sqlite.sql fedora@158.69.23.87:/home/fedora/kollusion/
scp ./kollusion/startup.sh fedora@158.69.23.87:/home/fedora/kollusion/
