#!/usr/bin/env bash
java -Dtlauncher.bootstrap.targetLibFolder=/opt/tlauncher/lib -Dtlauncher.bootstrap.packageMode=true -Dtlauncher.bootstrap.targetJar=/opt/tlauncher/launcher.jar -jar /opt/tlauncher/bootstrap.jar "$@"
