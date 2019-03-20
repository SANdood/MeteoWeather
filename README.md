## ***Meteobridge Weather Station v1.1.10 released on March 20, 2019***

### Overview
Meteobridge (www.meteobridge.com) is an inexpensive Linux-based server that is used by many to collect data from your local weather station and distribute it to one or more public weather sources (including Weather Underground, for example). It supports data collection from a large number of popular weather stations, and it offers a local API that allows clients/applications to retrieve the latest weather station data on demand.

This weather station DTH uses local hubAction() calls to provide pretty much all information exposed by The Meteobridge Template HTML API, which it retrieves as a JSON formatted map. While the DTH is optimized for Davis Vantage Pro2 Plus stations, it will report whatever subset Meteobridge provides for any connected weather station.

This device also (optionally) calculates and displays Purple Air Air Quality Index from a specified PurpleAir sensor. If you don't have one of your own, you will need to do a little research on the PurpleAir.com website to find a sensor nearby and then find its device ID from their JSON repository (search the repository for the name displayed for the sensor on the sensor map, then enter the numerical ID of that sensor into Preferences).

As most weather stations do no provide forecast details (hi/lo temps, humidity, probability of precipitation, etc.), this DTH allows selection from 1 of 3 sources for forecast data:
* **Dark Sky**: probably the best forecast source for PWS owners, as they provide hyper-local forecast data that *probably* includes data from your weather station (if you send to CWOP). *Note that to use Dark Sky, you will need an API key, available on their web site - this DTH will use about 96 Dark Sky API calls per day*;
* **The Weather Company**: now owned by IBM, and now the official SmartThings weather service, with location-based forecast data that *probably does not* include any input from your weather station;
* **MeteoBridge/Davis**: Davis Vantage Pro weather stations do provide a calculated forecast string; the code will drop back to use TWC for the rest of the forecast data if you choose this option
* ***Weather Underground support has been removed***, because SmartThings has replaced the getWeatherFeature() linkage to WU in favor of the above-mentioned TWC support.

Significantly, EVERYTHING that this DTH displays in Tiles is also available programmatically to other SmartApps and WebCoRE pistons. You can thus build apps and integrations based off of changes in any attribute, from temperature and humidity to air quality, lunar phase, wind speed and the like. If interested, have a look at the `attributes` listed at the top of the source file for the names of the available data points.

### Hubitat Compatibility
Thanks to the assistance of @staze and the Hubitat Community plus some code borrowed from Echo Speaks (thanks @tonesto7) this DTH can be used on Hubitat *without modification!* It will auto-detect the hub platform, and adjust its calls, classes and parameters appropriately for each platform. There are lots of incompatibilities between the two platforms, but clever utilization of Groovy allows a single code base to support both. Of course, on Hubitat there is no UI, but all of the attributes are programmatically available to apps and Rules Engine on that platform.

### Important Note for All Users
Since this DTH uses `hubAction()` for the MeteoBridge data, it will work only when your SmartThings/Hubitat hub and your MeteoBridge/WeatherBridge server are on the same (local) IP network. This approach keeps your password safe(r), but doesn't handle remote IP addresses. On the plus side, it is pretty lightweight to get updates from your meteobridge every minute, unlike using the WeatherUnderground data source.


The GiThub repository for MeteoWeather is here: 
https://github.com/SANdood/MeteoWeather 

### Installation
You can either manually copy/save the DTH code, or you can use the following for SmartThings' Github integration:

> Owner:  SANdood
> Name:   MeteoWeather
> Branch: master

After loading and publishing the DTH, you will need to manually create the device from the Devices page in your IDE. Once created, use your Mobile App to configure the preferences for your new Meteobridge-based Weather Station!

### Donations
This work is fully Open Source, and available for use at no charge.

While not required, I do humbly accept donations. If you would like to make an *optional* donation, I will be most grateful. You can make donations to me on PayPal at <https://paypal.me/BarryABurke>

### Change Log:

|*|1.1.10 - Initial Release (March 20, 2019)|
|---|---|

### Screen Shot:
<img src="https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/MeteoWeatherStation.png" border="1"  />
