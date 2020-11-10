# uad2midi - Universal Audio UAD to MIDI trigger

> A simple application that allows me to automatically select Sonarworks Reference 4 speaker profiles when I toggle studio monitors using my UAD interface.

<p align="center">
Download latest version: 
https://github.com/versionstudio/uad2midi/raw/main/uad2midi-1.0-release.zip
</p>

### What does this do?

UAD to MIDI *(uad2midi)* is a simple application that connects to your UAD interface and monitors any state changes, such as mutes, volumes or when you switch ALT monitoring. The application allows you to configure triggers on these events that can dispatch MIDI messages through any of your connected MIDI devices.

### Why on earth would you ever need this?

I am running UAD interfaces in my studio together with two sets of studio monitors/speakers which are controlled by ALT monitoring on the hardware. I am also running [Sonarworks Reference 4](https://www.sonarworks.com/reference) (systemwide) for speaker calibration, where each monitor pair has a different calibration profile in Sonarworks. Sadly, Sonarworks Reference 4 cannot have multiple profiles loaded at once which means you need to manually select calibration profile each time you toggle between the studio monitors.

Happily, Sonarworks Reference 4 supports binding calibration profiles to MIDI messages, and that's where this application comes in. The default configuration coming with UAD 2 MIDI comes pre-configured for just this scenario, where toggling ALT monitoring on your UAD interface will trigger MIDI messages that you can bind to the Sonarworks Reference 4 calibration profiles of you choice. 

Since UAD 2 MIDI is configurable, you can make use of it for other use cases as well that require UAD Console events to trigger MIDI messages.

### Credit where credit's due

This application was made possible thanks to the reverse engineering done by Radu Varga, author of [UA-Midi-Control](https://github.com/raduvarga/UA-Midi-Control). Thanks to the detailed [explanation](https://github.com/raduvarga/UA-Midi-Control#ok-so-how-did-you-do-it) I got good understanding of how to communicate with the UAD hardware.

### How do I set this up?

The application has been developed and tested on Mac OSX, and requires no special permissions to run. There's no user interface. Everything is configured in *uad2midi.properties*. Run the provided script *bin/startup.sh* to launch uad2midi.

###### Requirements:
- UAD hardware
- MIDI device
- Java runtime

###### Virtual MIDI device on Mac OSX
For my use case I used the virtual MIDI device *IAC Driver* for sending the MIDI messages. To do this:
- Search for the *Audio MIDI Setup* application in Spotlight
- In Audio MIDI Setup, click *Window* and then *Show MIDI Studio*
- Double-click IAC Driver
- Ensure that *Device is online* is clicked to make the device usable
- Click the *Ports* tab and note the name of the port.
- Open up *uad2midi.properties* and set the value of *uad2midi.midi.deviceName* to the name of the port

###### Hints:
- The UA Mixer Engine process must be running for this to work. This process starts up along with the UAD Console.
- Change *rootLogger.level* to *debug* in *log4j2.properties* to see more property names and values coming from your UAD interface.
- UAD to MIDI won't start if the MIDI device cannot be initialized
- UAD to MIDI will automatically reconnect if connection to the UAD Console is lost
- If you want to control Sonarworks Reference 4 like me, use *NOTE_OFF* as the MIDI command
