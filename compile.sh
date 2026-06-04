#!/bin/bash
set -e
echo "Creating output directory..."
mkdir -p build/classes

echo "Compiling Java files..."
/usr/lib/jvm/java-1.21.0-openjdk-amd64/bin/javac \
  --module-path /usr/share/openjfx/lib \
  --add-modules javafx.controls,javafx.fxml \
  -d build/classes \
  -cp dist/lib/postgresql-42.7.11.jar \
  $(find src -name "*.java")

echo "Copying resources (FXML, CSS, etc.) to build/classes..."
cd src
find . -type f ! -name "*.java" -exec cp --parents {} ../build/classes/ \;
cd ..

echo "Compilation and resource bundling complete!"
