#!/bin/bash
# Starts the Smart Canteen server. Run ./build.sh first.

if [ ! -f "out/com/smartcanteen/Main.class" ]; then
  echo "ERROR: Project is not built yet. Run ./build.sh first."
  exit 1
fi

java -cp "out:lib/mysql-connector-j-26.7.0.jar" com.smartcanteen.Main
