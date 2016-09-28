#!/usr/bin/env bash
for file in $(ls *.jar)
do
        echo "dx $file"
#       dx --dex --core-library --output=classes.dex $file
        dx --dex --output=classes.dex $file
        echo "aapt $file"
        aapt add $file classes.dex
        rm classes.dex
done
