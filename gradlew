#!/bin/bash

# Gradle wrapper script
# Gradle'ı çalıştır

APP_PATH="${0%/*}"
cd "$APP_PATH"

exec gradle "$@"
