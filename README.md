# Lyrics-display
A lyrics display featuring a Kotlin Android app that fetches JSON data, routes it through a Flask/ngrok API, and renders text on an Arduino Uno-powered LED screen.

Steps to use it:
1.form a virtual environment to download Flask in it and also add .gitignore so that it does not interfere with your github repo.

2.form your ngrok https website and make changes accordingly, ADD ngrok http (your pc developer port number) , In a seperate terminal to turn ngrok online

3.Turn on wireless debugging on your mobile and use android studio to add the app data on your phone , open terminal on it use the below commands to coonect and import the app to your phone:
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" pair IP:PAIRING_PORT
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" connect 2nd IP:PAIRING_PORT

4.turn on the notification for youtube music so it actively listen for playback location

5.use ardino ide to add all ardino related code in ardino uno

6.To wire your LCD screen to your board, connect the following pins:

LCD RS pin to digital pin 12
LCD Enable pin to digital pin 11
LCD D4 pin to digital pin 5
LCD D5 pin to digital pin 4
LCD D6 pin to digital pin 3
LCD D7 pin to digital pin 2
LCD R/W pin to GND
LCD VSS pin to GND
LCD VCC pin to 5V
LCD LED+ to 5V through a 220 ohm resistor
LCD LED- to GND
Additionally, wire a 10k potentiometer to +5V and GND, with it's wiper (output) to LCD screens VO pin (pin3).

7.now play music on your mobile phone , you should see the lyrics both on your pc terminal and liquid crystal display(LED).