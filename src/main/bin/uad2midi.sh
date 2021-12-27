#!/bin/sh

# set this to the root directory where you unpacked uad2midi
UAD2MIDI_HOME=/Users/versionstudio/uad2midi-1.2/

# uncomment this and point to java distribution as needed
#export JAVA_HOME=

apid=$(pgrep -f uad2midi-1.2.jar)

start() {
	if [ -n "$apid" ]; then
		echo "uad2midi is already running, pid is $apid"
	else
		cd $UAD2MIDI_HOME
		java -jar ./lib/uad2midi-1.2.jar &
	fi
}

stop() {
	kill -9 $apid
}


case "$1" in 
	start)	start ;;
	stop)	stop ;;
	*) echo "usage: $0 start|stop" >&2
	exit 1
	;;
esac