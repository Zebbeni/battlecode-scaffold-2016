#!/bin/bash

mkdir src/team006$1;
newname=team006$1;
cp src/team006/* src/$newname;
for f in src/team006$1/*.java; do 
  sed -i ''  "s/team006/$newname/" "$f";
  echo edited $f;
done
