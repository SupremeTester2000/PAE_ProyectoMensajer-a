#!/bin/bash
/usr/lib/jvm/java-1.21.0-openjdk-amd64/bin/java \
  --module-path /usr/share/openjfx/lib \
  --add-modules javafx.controls,javafx.fxml \
  -cp build/classes:dist/lib/postgresql-42.7.11.jar \
  View.Main
