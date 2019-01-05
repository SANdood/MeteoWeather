# MeteoWeather
SmartThings Weather Station using Meteobridge Local Connection (via hubAction).

Meteobridge (www.meteobridge.com) is an inexpensive Linux-based server that is used by many to collect data from your local weather station and distribute it to one or more public weather sources (including Weather Underground, for example). It supports data collection from a large number of popular weather stations, and it offers a local API that allows clients/applications to retrieve the latest weather station data on demand.

This weather station uses local hubAction() calls to provide pretty much all information exposed by The Meteobridge Template HTML API, which it retrieves as a JSON formatted map. While the DTH is optimized for Davis Vantage Pro2 Plus stations, it will report whatever subset Meteobridge provides for any connected weather station.

This device also (optionally) calculates and displays Purple Air Air Quality Index from a specified PurpleAir sensor. If you don't have one of your own, you will need to do a little research on the PurpleAir.com website to find a sensor nearby and then find its device ID from their JSON repository (search the repository for the name displayed for the sensor on the sensor map).

Significantly, EVERYTHING that this DTH displays in Tiles is also available programmatically to other SmartApps and WebCore pistons. You can thus build apps and integrations based off of changes in any attribute, from temperature and humidity to air quality, lunar phase, wind speed and the like. If interested, have a look at the `attributes` listed at the top of the source file for the names of the available data points.

### New Features 29 July 2018
MeteoWeather now displays forecast details (hi/low temps & humidity, plus rainfall forecast) for the rest of today and tomorrow, along with historical data for yesterday. This data is sourced from Weather Underground via the built-in SmartThings integration, or (optionally) from  Dark Sky, if you have a Dark Sky api key. Open the preferences to configure these new options.

Also, you can choose the source for the daily forecast text (above sun/moon rise/set info). The default is to use whatever Meteobridge returns, but for non-Davis weather stations, this can be blank. Preferences options allow you to select the Weather Underground daily forecast, or the Dark Sky forecast (if Dark Sky is configured).

### Updates 05 January 2019
* Now gets the "yesterday" data separately from the "current' data, reducing the load on your MeteoBridge (which could occaisionally time out)
* Added more detail for the current weather indication on the main tile
* Several ther minor tweaks and optimizations

**Note:** since this uses `hubAction()` for the Meteobridge data, it will work only when your SmartThings hub and Meteobridge server are on the same (local) IP network. This approach keeps your password safe(r), but doesn't handle remote IP addresses. On the plus side, it is pretty lightweight to get updates from your meteobridge every minute, unlike using the WeatherUnderground data source.

**Warning:** *If you try to convert this code to support a Meteobridge on an external IP, you will have to accomodate any differences between the Timezone of your Hub and the Meteobridge, otherwise several values will be inaccurate.*

## Change Log:
*	1.0.00 - Initial Release
*	1.0.01 - Added PurpleAir Air Quality Index (AQI)
*	1.0.02 - Fixed New/Full moon dislays
*	1.0.03 - Cleanup of Preferences page
*	1.0.04 - More tweaking to New/Full moon transitions
*	1.0.05 - Fixed class casting errors
*	1.0.06 - Renamed some attributes for naming consistency
*	1.0.07 - Added pref setting for Lux scale
*	1.0.08 - Increased internal attribute precision for temps & precipitation
*	1.0.09 - Changed to use my Ecobee Suite weather icons (black circles)
*	1.0.10 - Minor display tweaks
*	1.0.11 - Optimized PurpleAir AQI calculations
*	1.0.12 - Converted to BigDecimal for maximum precision
*	1.0.13 - Adjust decimal precision for rainfall (e.g., inches.2 vs mm.1)
*	1.0.14 - Option to use Dark Sky conditions/forecast instead of Weather Underground
*	1.0.15 - Expanded almanac display to include weather forecast
*	1.0.16 - Added today's forecast data
*	1.0.17 - Extensive formatting changes
*	1.0.18 - Fixed units on forecasted temps
*	1.0.19 - Reduced normal-state log.info messages
*	1.0.20 - Changed "SolRad"
*	1.0.21 - Fixed temperature color on Android
*	1.0.22 - Get yesterday data separately, only when day/night changes
*	1.0.23 - Changed to get yesterday data when the date changes
*	1.0.24 - Optimizations
*	1.0.25 - Use update time from the Meteobridge instead of time we made the http request
*	1.0.26 - Support additional detail for current weather
*	1.0.27 - Added attribution label

### Donations
As always, my contributions to the SmartThings community are entirely free, but should you feel compelled to make a donation, you can do so here: https://paypal.me/BarryABurke

## Screen Shot:
<img src="https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/MeteoWeatherStation.png" border="1" height="1200" /> 
