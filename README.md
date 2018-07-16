# MeteoWeather
SmartThings Weather Station using Meteobridge Local Connection (via hubAction).

Meteobridge (www.meteobridge.com) is an inexpensive Linux-based server that is used by many to collect data from your local weather station and distribute it to one or more public weather sources (including Weather Underground, for example). It supports data collection from a large number of popular weather stations, and it offers a local API that allows clients/applications to retrieve the latest weather station data on demand.

This weather station uses local hubAction() calls to provide pretty much all information exposed by The Meteobridge Template HTML API, which it retrieves as a JSON formatted map. While the DTH is optimized for Davis Vantage Pro2 Plus stations, it will report whatever subset Meteobridge provides for any connected weather station.

This device also (optionally) calculates and displays Purple Air Air Quality Index from a specified PurpleAir sensor. If you don't have one of your own, you will need to do a little research on the PurpleAir.com website to find a sensor nearby and then find its device ID from their JSON repository (search the repository for the name displayed for the sensor on the sensor map).

Significantly, EVERYTHING that this DTH displays in Tiles is also available programmatically to other SmartApps and WebCore pistons. You can thus build apps and integrations based off of changes in any attribute, from temperature and humidity to air quality, lunar phase, wind speed and the like. If interested, have a look at the `attributes` listed at the top of the source file for the names of the available data points.

**Note:** since this uses `hubAction()` for the Meteobridge data, it will work only when your SmartThings hub and Meteobridge server are on the same (local) IP network. This approach keeps your password safe(r), but doesn't handle remote IP addresses. On the plus side, it is pretty lightweight to get updates from your meteobridge every minute, unlike using the WeatherUnderground data source.

**Warning:** *If you try to convert this code to support a Meteobridge on an external IP, you will have to accomodate any differences between the Timezone of your Hub and the Meteobridge, otherwise several values will be inaccurate.*

## Change Log:
`v1.0.01 - Adds support for Purple Air air quality sensors, via asyncHttp (does not use local hubAction)`<br>
`v1.0.02 - Fixes an issue with the display of New & Full Moon`<br>
`v1.0.03 - Cleanup of preferences page`<br>
`v1.0.04 - More tweaking to New/Full moon transitions`<br>
`v1.0.05 - Fixed class casting errors`<br>
`v1.0.06 - Renamed some attributes for consistency`<br>
`v1.0.07 - Added pref setting for Lux scale`<br>
`v1.0.08 - Increased internal attribute precision for temps & precipitation`<br>
`v1.0.09 - Changed to use my Ecobee Suite weather icons (black circles)`<br>

## Screen Shot:
<img src="https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/MeteoWeatherStation.png" border="1" height="1200" /> 
