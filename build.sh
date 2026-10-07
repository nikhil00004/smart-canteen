#!/bin/bash
# Compiles every .java file into the "out" folder.
# Run from the project root: ./build.sh

JAR="lib/mysql-connector-j-26.7.0.jar

if [ ! -f "$JAR" ]; then
  echo "ERROR: $JAR not found."
  echo "Download the MySQL Connector/J jar and place it in the lib folder (see README.md)."
  exit 1
fi

mkdir -p out
find src -name "*.java" > sources.txt
javac -cp "$JAR" -d out @sources.txt
rm sources.txt

echo "Build succeeded! Run ./start.sh to launch the server."
