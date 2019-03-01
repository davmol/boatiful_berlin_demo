# Boatiful Berlin Demo

An Android App thought for those who like to paddle on the water. The Boatiful Demo Berlin app is a PoC and provides a Berlin offline Map with offline Routing on the River and Canal Network of the City. Beyond the fastest route, time and distance, speed and accuracy bearing via gps will be displayed. Symbology and Usage will be explained within the app.
<br>
<br>
<p align="center">
  <img src="http://davmol.de/git_hub_data/Screenshot_20190120-130104.jpg" width="250">
  <img src="http://davmol.de/git_hub_data/Screenshot_20190120-130232.jpg" width="250">
  <img src="http://i67.tinypic.com/2iaztog.jpg" width="250">
</p>


# Quick Start
Simply download and install the "Boatiful Demo Berlin.apk" on your android device. Ensure GPS availability for user location usage. Supported android api levels are: 23 - 27.


<br>
<br>

<p>
  For Download scan or click here:<br>
  <a href="https://mega.nz/#!WUoC0YhS!3YCC3RQDD12Jhvm-sd8H8_8Ae-QZakPA5TvSG5tMxek">
  <img alt="Download here" src="https://raw.githubusercontent.com/davmol/boatiful_berlin_demo/master/qr.png" width="300">
</a>
</p>

 
# Background Story

# Map
The Basemap uses the .map format used with the Mapsforge VTM Render Engine:

https://github.com/mapsforge/vtm

The Basemap is created with Osmosis & Mapsforge-Writer Plugin:

https://wiki.openstreetmap.org/wiki/Osmosis

https://github.com/mapsforge/mapsforge/blob/master/docs/Getting-Started-Map-Writer.md

For the visualization a custom VTM Theme is applied with custom POIs for waterways.

# Routing
Custom routing profile on waterways via the open source graphhopper core api which can be found here:

https://github.com/graphhopper/graphhopper

Graphhopper is a routing enginge that works on OpenStreetMap data. The custom routing profile uses following tags:

<waterway="river">
<boat="yes">


