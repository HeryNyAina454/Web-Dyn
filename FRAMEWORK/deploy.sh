#!/bin/bash

APP_NAME=FRAMEWORK
SRC_DIR=src/main/java
BUILD_DIR=build
LIB_DIR=lib
SERVLET_API_JAR=$LIB_DIR/servlet-api.jar
DEST=~/Cours_S4/WEB/FRAMEWORK/TEST/lib/

# Cleanup
if [ -d "$BUILD_DIR" ]; then
    rm -rf "$BUILD_DIR"
fi
mkdir -p "$BUILD_DIR"

# Compilation
echo "Compiling Java files..."
javac -cp "$SERVLET_API_JAR" -d "$BUILD_DIR" $(find "$SRC_DIR" -name "*.java")

if [ $? -ne 0 ]; then
    echo "ERREUR : Compilation échouée."
    exit 1
fi

# Create JAR
echo "Creating JAR file..."
jar -cvf "$APP_NAME.jar" -C "$BUILD_DIR" .

if [ $? -ne 0 ]; then
    echo "ERREUR : Création du JAR échouée."
    exit 1
fi

echo "JAR créé avec succès."

# Copy to TEST project
echo "Copying JAR to $DEST..."
mkdir -p "$DEST"
cp "$APP_NAME.jar" "$DEST"
echo "JAR copié vers $DEST"
