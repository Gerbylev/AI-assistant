#!/bin/bash

ROOT=$(realpath $(dirname $0))
cd $ROOT

CP='./lib/*:./libm/*:'
java -cp $CP -Dconf=conf/voice-demo.yml cc.CCVoiceApplicationKt



