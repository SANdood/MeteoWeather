/**
*  Copyright 2015 SmartThings
*  Copyright 2019 Barry A. Burke
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*  Meteobridge Weather Station
*
*  Author: SmartThings
*
*
*  VClouds Weather Icons©
*  Created and copyrighted© by VClouds - http://vclouds.deviantart.com/
* 
*  The icons are free to use for Non-Commercial use, but If you use want to use it with your art please credit me 
*  and put a link leading back to the icons DA page - http://vclouds.deviantart.com/gallery/#/d2ynulp
* 
*  *** Not to be used for commercial use without permission! 
*  if you want to buy the icons for commercial use please send me a note - http://vclouds.deviantart.com/ ***
*
*  Date: 2018-07-04
*
*	Updates by Barry A. Burke (storageanarchy@gmail.com)
*	Date: 2017 - 2019
*
*	1.0.00 - Initial Release
8	<snip>
*	1.1.01 - Now supports both SmartThings & Hubitat (automagically)
*	1.1.02 - Corrected contentType for hubAction call
*	1.1.03 - Removed wunderGround support (deprecated by SmartThings)
*	1.1.04 - Fixed ST/HE autodetect logic
*	1.1.05 - Cleaned up another ST/HE quirk
*	1.1.06 - Added Breezy and Foggy, cleaned up hubResponse returns
*	1.1.07 - Cleaned up isST()/isHE()
*	1.1.08 - Yet more fixes for application/json handling
*	1.1.10 - Initial general release of SmartThings+Hubitat version
*	1.1.11 - Added 'Possible Light Snow and Breezy/Windy', optimized icon calculations
*	1.1.12 - Added Air Quality, indoor Temperature, Humidity and Dewpoint attributes (not displayed yet)
*	1.1.13a- New SmartThings/Hubitat Portability Library
*	1.1.14 - Fully utilize SHPL
*	1.1.15 - Fixed cloud cover calculation
*	1.1.16 - Fixed TWC error (isHE)
*	1.1.17 - Major bug fixes, new icons, Hubitat myTile support
*	1.1.18 - Relocated iconStore
*	1.1.19 - More bug fixes & optimizations
*	1.1.20 - Improved handling of non-existent sensors (solar, UV, rain, wind)
*	1.1.21 - Added city/state/country on ST - PREFERS MeteoBridge Location!!!
*	1.1.22 - Minor bug fixes & improved error handling
*	1.1.23 - Major overhaul of MeteoBridge template generation & handling
*	1.1.24 - Warning message cleanup, expanded "windy/breezy and ..." icons
*	1.1.25 - Added preferences option to use averaged data pver the last update frequency period
*	1.1.26 - Cosmetic additional debug traces
*	1.1.27 - Fixed timestamp/timeGet, reduced chars in myTile
*	1.1.28 - Added wind speed attributes for small HE Dashboard tile, fixed day/night transition icons
*	1.1.29 - Added hubAction timeout, optionally skip creating tile on HE, misc weatherIcons cleanup
*	1.1.30 - Reschedule if MB server tells us it is busy/overloaded
*	1.1.31 - Changed all logging to use 'debugOn' preference
*
*/
import groovy.json.*
import java.text.SimpleDateFormat
import groovy.transform.Field

private getVersionNum() { return "1.1.31" }
private getVersionLabel() { return "Meteobridge Weather Station, version ${versionNum}" }
private getDebug() { false }
private getFahrenheit() { true }		// Set to false for Celsius color scale
private getCelsius() { !fahrenheit }
private getSummaryText() { true }
// private getShortStats() { true }

// **************************************************************************************************************************
// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019, Barry A. Burke (storageanarchy@gmail.com)
//
// The following 3 calls are safe to use anywhere within a Device Handler or Application
//  - these can be called (e.g., if (getPlatform() == 'SmartThings'), or referenced (i.e., if (platform == 'Hubitat') )
//  - performance of the non-native platform is horrendous, so it is best to use these only in the metadata{} section of a
//    Device Handler or Application
//
private getPlatform() { (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
private getIsST()     { (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
private getIsHE()     { (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...
//
// The following 3 calls are ONLY for use within the Device Handler or Application runtime
//  - they will throw an error at compile time if used within metadata, usually complaining that "state" is not defined
//  - getHubPlatform() ***MUST*** be called from the installed() method, then use "state.hubPlatform" elsewhere
//  - "if (state.isST)" is more efficient than "if (isSTHub)"
//
/* private String getHubPlatform() {
	
    if (state?.hubPlatform == null) {
        state.hubPlatform = getPlatform()						// if (hubPlatform == 'Hubitat') ... or if (state.hubPlatform == 'SmartThings')...
        state.isST = state.hubPlatform.startsWith('S')			// if (state.isST) ...
        state.isHE = state.hubPlatform.startsWith('H')			// if (state.isHE) ...
    }
    return state.hubPlatform
} */
private String getHubPlatform() {
	def pf = getPlatform()
	state?.hubPlatform = pf			// if (state.hubPlatform == 'Hubitat') ...
											// or if (state.hubPlatform == 'SmartThings')...
	state?.isST = pf.startsWith('S')	// if (state.isST) ...
	state?.isHE = pf.startsWith('H')	// if (state.isHE) ...
	return pf
}
private getIsSTHub() { (state.isST) }					// if (isSTHub) ...
private getIsHEHub() { (state.isHE) }					// if (isHEHub) ...
//
// **************************************************************************************************************************

metadata {
    definition (name: "Meteobridge Weather Station", namespace: "sandood", author: "sandood",
	        importUrl: "https://raw.githubusercontent.com/SANdood/MeteoWeather/master/devicetypes/sandood/meteobridge-weather-station.src/meteobridge-weather-station.groovy")
	{
    	capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Ultraviolet Index"
        capability "Illuminance Measurement"
		if (isST) capability "Air Quality Sensor"
		capability "Water Sensor"
        capability "Sensor"
        capability "Refresh"

		if (isHE) attribute "airQuality", "number"
		attribute "airQualityIndex", "number"
		attribute "aqi", "number"
		attribute "attribution", "string"
		attribute "avgHumForecast", "number"
		attribute "avgHumTomorrow", "number"
		attribute "city", "string"
		attribute "country", "string"
		attribute "currentDate", "string"
		attribute "currentTime", "string"
		if (debug) attribute "darkSkyWeather", "string"			// For debugging only
		attribute "dayHours", "string"
		attribute "dayMinutes", "number"
		attribute "dewpoint", "number"
		if (debug) attribute "etDisplay", "string"
		attribute "evapotranspiration", "number"
		attribute "forecast", "string"
		attribute "forecastCode", "string"
		attribute "heatIndex", "number"
		if (debug) attribute "heatIndexDisplay", "string"
		attribute "highHumYesterday", "number"
		attribute "highHumidity", "number"
		attribute "highTemp", "number"
		attribute "highTempForecast", "number"
		attribute "highTempTomorrow", "number"
		attribute "highTempYesterday", "number"
		if (debug) attribute "hubAction", "string"				// For debugging only
		if (debug) attribute "iconErr", "string"				// For debugging only
		attribute "indoorDewpoint", "number"
		attribute "indoorHumidity", "number"
		attribute "indoorTemperature", "number"
		attribute "isDay", "number"
		attribute "isNight", "number"
		attribute "lastSTupdate", "string"
		attribute "last_observation_Forecast", "string"
		attribute "last_observation_Station", "string"
		attribute "last_poll_Forecast", "string"
		attribute "last_poll_Station", "string"
		attribute "latitude", "number"
		attribute "localMoonrise", "string"
		attribute "localMoonset", "string"
		attribute "localSunrise", "string"
		attribute "localSunset", "string"
		attribute "location", "string"
		attribute "locationName", "string"
		attribute "longitude", "number"
		attribute "lowHumYesterday", "number"
		attribute "lowHumidity", "number"
		attribute "lowTemp", "number"
		attribute "lowTempForecast", "number"
		attribute "lowTempTomorrow", "number"
		attribute "lowTempYesterday", "number"
		attribute "lunarAge", "number"
		attribute "lunarPercent", "number"
		attribute "lunarSegment", "number"
		if (debug) attribute "meteoTemplate", "string"			// For debugging only
		if (debug) attribute "meteoWeather", "string"			// For debugging only
		attribute "moonAge", "number"
		if (debug) attribute "moonDisplay", "string"
		attribute "moonIllumination", "number"
		if (debug) attribute "moonInfo", "string"
		attribute "moonPhase", "string"
		if (debug) attribute "moonPercent", "number"
		attribute "moonrise", "string"
		attribute "moonriseAPM", "string"
		attribute "moonriseEpoch", "number"
		attribute "moonset", "string"
		attribute "moonsetAPM", "string"
		attribute "moonsetEpoch", "number"
		attribute "myTile", "string"
		attribute "pop", "number"					// Probability of Precipitation (in %)
		if (debug) attribute "popDisplay", "string"
		if (debug) attribute "popFcstDisplay", "string"
		attribute "popForecast", "number"
		if (debug) attribute "popTomDisplay", "string"
		attribute "popTomorrow", "number"
		if (debug) attribute "precipFcstDisplay", "string"
		attribute "precipForecast", "number"
		if (debug) attribute "precipLastHourDisplay", "string"
		attribute "precipLastHour", "number"
		attribute "precipRate", "number"
		if (debug) attribute "precipRateDisplay", "string"
		attribute "precipToday", "number"
		if (debug) attribute "precipTodayDisplay", "string"
		if (debug) attribute "precipTomDisplay", "string"
		attribute "precipTomorrow", "number"
		attribute "precipYesterday", "number"
		if (debug) attribute "precipYesterdayDisplay", "string"
		attribute "pressure", "number"
		if (debug) attribute "pressureDisplay", "string"
		attribute "pressureTrend", "string"
		if (debug) attribute "purpleAir", "string"				// For debugging only
		attribute "respAvg", "number"
		attribute "respTime", "number"
		attribute "solarRadiation", "number"
		attribute "state", "string"
		if (summaryText) attribute "summaryList", "string"
		if (summaryText) attribute "summaryMap", "string"
		attribute "sunrise", "string"
		attribute "sunriseAPM", "string"
		attribute "sunriseEpoch", "number"
		attribute "sunset", "string"
		attribute "sunsetAPM", "string"
		attribute "sunsetEpoch", "number"
		attribute "timestamp", "string"
		attribute "timezone", "string"
		if (debug) attribute "twcConditions", "string"			// For debugging only
		if (debug) attribute "twcForecast", "string"			// For debugging only
		attribute "tz_id", "string"
		attribute "uvIndex", "number"				// Also 'ultravioletIndex' per ST capabilities 07/19/2018
		attribute "water", "string"
		attribute "weather", "string"
		attribute "weatherIcon", "string"
		attribute "weatherIcons", "string"		// OpenWeatherMap icon
		attribute "wind", "number"
		attribute "windChill", "number"
		if (debug) attribute "windChillDisplay", "string"
		attribute "windDirection", "string"
		attribute "windDirectionText", "string"
		if (debug) attribute "windinfo", "string"
		attribute "windGust", "number"
		attribute "windSpeed", "number"
		attribute "wind_degree", "string"
		attribute "wind_dir", "string"
		attribute "wind_direction", "string"
		attribute "wind_gust", "number"
		attribute "wind_speed", "number"
		attribute "wind_string", "string"
		attribute "wundergroundObs", "string"		// For debugging only
		attribute 'feelsLike', 'number'
		
		command "refresh", []
		// command "getWeatherReport"
    }

    preferences {
    	input(name: 'updateMins', type: 'enum', description: "Select the update frequency", 
        	title: "${getVersionLabel()}\n\nUpdate frequency (minutes)", displayDuringSetup: true, defaultValue: '5', options: ['1', '2', '3', '5','10','15','30'], required: true)
        input(name: 'avgUpdates', type: 'bool', title: 'Use averaged updates?', defaultValue: false, displayDuringSetup: true, required: true)
        
        // input(name: "zipCode", type: "text", title: "Zip Code or PWS (optional)", required: false, displayDuringSetup: true, description: 'Specify Weather Underground ZipCode or pws:')
        input(name: "twcLoc", type: "text", title: "TWC Location code (optional)\n(US ZipCode or Lat,Lon)", required: false, displayDuringSetup: true, description: "Leave blank for MeteoBridge's location")
        
        if (isST) { input (description: "Setup Meteobridge access", title: "Meteobridge Setup", displayDuringSetup: true, type: 'paragraph', element: 'MeteoBridge') }
        input "meteoIP", "string", title:"Meteobridge IP Address", description: "Enter your Meteobridge's IP Address", required: true, displayDuringSetup: true
 		input "meteoPort", "string", title:"Meteobridge Port", description: "Enter your Meteobridge's Port", defaultValue: 80 , required: true, displayDuringSetup: true
    	input "meteoUser", "string", title:"Meteobridge User", description: "Enter your Meteobridge's username", required: true, defaultValue: 'meteobridge', displayDuringSetup: true
    	input "meteoPassword", "password", title:"Meteobridge Password", description: "Enter your Meteobridge's password", required: true, displayDuringSetup: true
		input "shortStats", "bool", title:"Use optimized MeteoBridge requests?", required: true, defaultValue: false, displayDuringSetup: true
        
        input ("purpleID", "string", title: 'Purple Air Sensor ID (optional)', description: 'Enter your PurpleAir Sensor ID', required: false, displayDuringSetup: true)

        input ("darkSkyKey", "string", title: 'DarkSky Secret Key (optional)', description: 'Enter your DarkSky key (from darksky.net)', defaultValue: '', required: false, 
        		displayDuringSetup: true, submitOnChange: true)
        
        input ("fcstSource", "enum", title: 'Select weather forecast source', description: "Select the source for your weather forecast (default=Meteobridge)", required: false, displayDuringSetup: true,
        		options: ['darksky':'Dark Sky', 'meteo': 'Meteobridge', 'twc': 'The Weather Company'])
                
        input ("pres_units", "enum", title: "Barometric Pressure units (optional)", required: false, displayDuringSetup: true, description: "Select desired units:",
			options: [
		        "press_in":"Inches",
		        "press_mb":"milli bars"
            ])
        input ("dist_units", "enum", title: "Distance units (optional)", required: false, displayDuringSetup: true, description: "Select desired units:", 
			options: [
		        "dist_mi":"Miles",
		        "dist_km":"Kilometers"
            ])
        input("height_units", "enum", title: "Height units (optional)", required: false, displayDuringSetup: true, description: "Select desired units:",
			options: [
                "height_in":"Inches",
                "height_mm":"Millimeters"
            ])
        input("speed_units", "enum", title: "Speed units (optional)", required: false, displayDuringSetup: true, description: "Select desire units:",
			options: [
                "speed_mph":"Miles per Hour",
                "speed_kph":"Kilometers per Hour"
            ])
        input("lux_scale", "enum", title: "Lux Scale (optional)", required: false, displayDuringSetup: true, description: "Select desired scale:",
        	options: [
            	"default":"0-1000 (Aeon)",
                "std":"0-10,000 (ST)",
                "real":"0-100,000 (actual)"
            ])
        if (isHE) { input "skipMyTile", "bool", title: "Skip generation of myTile weather page HTML?", required: true, defaultValue: false, displayDuringSetup: true }
		
		input(name: 'debugOn', type: 'bool', title: 'Enable debug logging?', defaultValue: false, displayDuringSetup: true)
        
        // input "weather", "device.smartweatherStationTile", title: "Weather...", multiple: true, required: false
    }
    
    tiles(scale: 2) {
        multiAttributeTile(name:"temperatureDisplay", type:"generic", width:6, height:4, canChangeIcon: false) {
            tileAttribute("device.temperatureDisplay", key: "PRIMARY_CONTROL") {
                attributeState("temperatureDisplay", label:'${currentValue}°', defaultState: true,
					backgroundColors: (temperatureColors)
                )
            }
            tileAttribute("device.weatherIcon", key: "SECONDARY_CONTROL") {
            	//attributeState 'default', label: '${currentValue}', defaultState: true
				attributeState "chanceflurries", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: " Chance of Flurries"
				attributeState "chancelightsnow", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: " Possible Light Snow"
				attributeState "chancelightsnowbreezy", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41b.png", 	label: " Possible Light Snow and Breezy"
				attributeState "chancelightsnowwindy", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41b.png", 	label: " Possible Light Snow and Windy"
				attributeState "chancerain", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: " Chance of Rain"
				attributeState "chancedrizzle", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: " Chance of Drizzle"
				attributeState "chancelightrain", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: " Chance of Light Rain"
				attributeState "chancesleet", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: " Chance of Sleet"
				attributeState "chancesnow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: " Chance of Snow"
				attributeState "chancetstorms", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/38.png", 	label: " Chance of Thunderstorms"
				attributeState "clear", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/32.png", 	label: " Clear"
				attributeState "humid", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/36.png", 	label: " Humid"
				attributeState "sunny", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/36.png", 	label: " Sunny"
				attributeState "clear-day",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/32.png", 	label: " Clear"
				attributeState "cloudy", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/26.png", 	label: " Overcast"
				attributeState "humid-cloudy",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/26.png", 	label: " Humid and Overcast"
				attributeState "flurries", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/13.png", 	label: " Snow Flurries"
				attributeState "scattered-flurries", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: " Scattered Snow Flurries"
				attributeState "scattered-snow", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: " Scattered Snow Showers"
				attributeState "lightsnow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/14.png", 	label: " Light Snow"
				attributeState "frigid-ice", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/10.png", 	label: " Frigid / Ice Crystals"
				attributeState "fog", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/20.png", 	label: " Foggy"
				attributeState "hazy", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/21.png", 	label: " Hazy"
				attributeState "smoke",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/22.png", 	label: " Smoke"
				attributeState "mostlycloudy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: " Mostly Cloudy"
				attributeState "mostly-cloudy", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: " Mostly Cloudy"
				attributeState "mostly-cloudy-day",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: " Mostly Cloudy"
				attributeState "humid-mostly-cloudy", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: " Humid and Mostly Cloudy"
				attributeState "humid-mostly-cloudy-day", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: " Humid and Mostly Cloudy"
				attributeState "mostlysunny", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/34.png", 	label: " Mostly Sunny"
				attributeState "partlycloudy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30.png", 	label: " Partly Cloudy"
				attributeState "partly-cloudy", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30.png", 	label: " Partly Cloudy"
				attributeState "partly-cloudy-day",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30.png", 	label: " Partly Cloudy"
				attributeState "humid-partly-cloudy", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30.png", 	label: " Humid and Partly Cloudy"
				attributeState "humid-partly-cloudy-day", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30.png", 	label: " Humid and Partly Cloudy"
				attributeState "partlysunny", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: " Partly Sunny"
				attributeState "rain", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/12.png", 	label: " Rain"
				attributeState "rain-breezy",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: " Rain and Breezy"
				attributeState "rain-windy",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: " Rain and Windy"
				attributeState "rain-windy!",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: " Rain and Dangerously Windy"
				attributeState "heavyrain", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/12.png", 	label: " Heavy Rain"
				attributeState "heavyrain-breezy",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: " Heavy Rain and Breezy"
				attributeState "heavyrain-windy", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: " Heavy Rain and Windy"
				attributeState "heavyrain-windy!", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: " Heavy Rain and Dangerously Windy"
				attributeState "drizzle",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/9.png", 	label: " Drizzle"
				attributeState "lightdrizzle",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/9.png", 	label: " Light Drizzle"
				attributeState "heavydrizzle",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/9.png", 	label: " Heavy Drizzle"
				attributeState "lightrain",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: " Light Rain"
				attributeState "scattered-showers",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: " Scattered Showers"
				attributeState "lightrain-breezy",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: " Light Rain and Breezy"
				attributeState "lightrain-windy",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: " Light Rain and Windy"
				attributeState "lightrain-windy!",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: " Light Rain and Dangerously Windy"
				attributeState "sleet",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/10.png", 	label: " Sleet"
				attributeState "lightsleet",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/8.png", 	label: " Light Sleet"
				attributeState "heavysleet",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/10.png", 	label: " Heavy Sleet"
				attributeState "rain-sleet",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/6.png", 	label: " Rain and Sleet"
				attributeState "winter-mix",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/7.png", 	label: " Wintery Mix of Snow and Sleet"
				attributeState "freezing-drizzle",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/8.png", 	label: " Freezing Drizzle"
				attributeState "freezing-rain",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/10.png", 	label: " Freezing Rain"
				attributeState "snow", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/15.png", 	label: " Snow"
				attributeState "heavysnow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/16.png", 	label: " Heavy Snow"
				attributeState "blizzard", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/42.png", 	label: " Blizzard"
				attributeState "rain-snow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/7.png", 	label: " Rain to Snow Showers"
				attributeState "tstorms", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/0.png", 	label: " Thunderstorms"
				attributeState "tstorms-iso", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/37.png", 	label: " Isolated Thunderstorms"
				attributeState "thunderstorm", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/0.png", 	label: " Thunderstorm"
				attributeState "windy",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Windy"
				attributeState "wind",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Windy"
				attributeState "sandstorm",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/19b.png", 	label: " Blowing Dust / Sandstorm"
				attributeState "blowing-spray",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Windy / Blowing Spray"
				attributeState "wind!",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Dangerously Windy"
				attributeState "wind-foggy",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Windy and Foggy"
				attributeState "wind-overcast",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24c.png", 	label: " Windy and Overcast"
				attributeState "wind-overcast!",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24c.png", 	label: " Dangerously Windy and Overcast"
				attributeState "wind-partlycloudy",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30b.png", 	label: " Windy and Partly Cloudy"
				attributeState "wind-partlycloudy!", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30b.png", 	label: " Dangerously Windy and Partly Cloudy"
				attributeState "wind-mostlycloudy",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28b.png", 	label: " Windy and Mostly Cloudy"
				attributeState "wind-mostlycloudy!",		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28b.png", 	label: " Dangerously Windy and Mostly Cloudy"
				attributeState "breezy",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Breezy"
				attributeState "breezy-overcast",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24b.png", 	label: " Breezy and Overcast"
				attributeState "breezy-partlycloudy", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30b.png", 	label: " Breezy and Partly Cloudy"
				attributeState "breezy-mostlycloudy", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28b.png", 	label: " Breezy and Mostly Cloudy"
				attributeState "breezy-foggy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/20b.png", 	label: " Breezy and Foggy"
				attributeState "tornado",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/to.png",	label: " Tornado"
				attributeState "hail",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/18.png",	label: " Hail Storm"
				attributeState "thunder-hail",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png",	label: " Thunder and Hail Storm"
				attributeState "rain-hail",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/7.png",	label: " Mixed Rain and Hail"
				attributeState "nt_chanceflurries", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: " Chance of Flurries"
				attributeState "chancelightsnow-night", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png", 	label: " Possible Light Snow"
				attributeState "chancelightsnowbz-night", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46b.png", 	label: " Possible Light Snow and Breezy"
				attributeState "chancelightsnowy-night", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46b.png", 	label: " Possible Light Snow and Windy"
				attributeState "nt_chancerain", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: " Chance of Rain"
				attributeState "chancerain-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: " Chance of Rain"
				attributeState "chancelightrain-night", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/45.png", 	label: " Chance of Light Rain"
				attributeState "nt_chancesleet", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: " Chance of Sleet"
				attributeState "chancesleet-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: " Chance of Sleet"
				attributeState "nt_chancesnow", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png", 	label: " Chance of Snow"
				attributeState "chancesnow-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png", 	label: " Chance of Snow"
				attributeState "nt_chancetstorms", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/47.png",	label: " Chance of Thunderstorms"
				attributeState "chancetstorms-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/47.png",	label: " Chance of Thunderstorms"
				attributeState "nt_clear", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31.png", 	label: " Clear"
				attributeState "clear-night",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31.png", 	label: " Clear"
				attributeState "humid-night",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/33.png", 	label: " Humid"
				attributeState "nt_sunny", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31.png", 	label: " Clear"
				attributeState "nt_cloudy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/26.png", 	label: " Overcast"
				attributeState "cloudy-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/26.png", 	label: " Overcast"
				attributeState "humid-cloudy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/26.png", 	label: " Humid and Overcast"	
				attributeState "nt_fog", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31b.png", 	label: " Foggy"
				attributeState "fog-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31b.png", 	label: " Foggy"
				attributeState "nt_hazy", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31b.png", 	label: " Hazy"
				attributeState "nt_mostlycloudy", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27.png",	label: " Mostly Cloudy"
				attributeState "mostly-cloudy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27.png",	label: " Mostly Cloudy"
				attributeState "humid-mostly-cloudy-night",	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27.png", 	label: " Humid and Mostly Cloudy"
				attributeState "nt_mostlysunny", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/33.png",	label: " Mostly Clear"
				attributeState "nt_partlycloudy", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29.png",	label: " Partly Cloudy"
				attributeState "partly-cloudy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29.png",	label: " Partly Cloudy"
				attributeState "humid-partly-cloudy-night",	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29.png", 	label: " Humid and Partly Cloudy"
				attributeState "nt_partlysunny", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27.png",	label: " Partly Clear"
				attributeState "nt_scattered-flurries", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/13.png", 	label: " Flurries"
				attributeState "nt_flurries", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: " Scattered Flurries"
				attributeState "flurries-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/13.png", 	label: " Flurries"
				attributeState "lightsnow-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/14.png", 	label: " Light Snow"
				attributeState "nt_rain", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: " Rain"
				attributeState "rain-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: " Rain"
				attributeState "rain-breezy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: " Rain and Breezy"
				attributeState "rain-windy-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: " Rain and Windy"
				attributeState "rain-windy-night!", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: " Rain and Dangerously Windy"
				attributeState "heavyrain-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/12.png", 	label: " Heavy Rain"
				attributeState "heavyrain-breezy-night", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png",	label: " Heavy Rain and Breezy"
				attributeState "heavyrain-windy-night",		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: " Heavy Rain and Windy"
				attributeState "heavyrain-windy-night!", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png",	label: " Heavy Rain and Dangerously Windy"
				attributeState "nt_drizzle", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/9.png", 	label: " Drizzle"
				attributeState "drizzle-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/9.png", 	label: " Drizzle"
				attributeState "nt_lightrain", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: " Light Rain"
				attributeState "lightrain-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: " Light Rain"	
				attributeState "nt_scattered-rain", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: " Scattered Showers"
				attributeState "lightrain-breezy-night", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: " Light Rain and Breezy"
				attributeState "lightrain-windy-night", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: " Light Rain and Windy"
				attributeState "lightrain-windy-night!", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: " Light Rain and Dangerously Windy"
				attributeState "nt_sleet", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: " Sleet"
				attributeState "sleet-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: " Sleet"
				attributeState "lightsleet-night",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: " Sleet"
				attributeState "nt_rain-sleet",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: " Rain and Sleet"
				attributeState "nt_thunder-hail",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/47.png",	label: " Thunder and Hail Storm"
				attributeState "nt_winter-mix",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/7.png",	label: " Winter Mix of Sleet and Snow"
				attributeState "nt_freezing-drizzle", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/8.png",	label: " Freezing Drizzle"
				attributeState "nt_freezing-rain", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/8.png",	label: " Freezing Rain"
				attributeState "nt_snow", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png,",	label: " Snow"
				attributeState "nt_rain-snow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png,",	label: " Rain and Snow Showers"
				attributeState "snow-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png,",	label: " Snow"
				attributeState "nt_heavysnow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/42.png,",	label: " Heavy Snow"
				attributeState "nt_heavysnow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/42.png,",	label: " Heavy Snow"
				attributeState "nt_tstorms", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/47.png",	label: " Thunderstorms"
				attributeState "nt_blizzard", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/42.png",	label: " Blizzard"
				attributeState "nt_thunderstorm", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/0.png",	label: " Thunderstorm"
				attributeState "thunderstorm-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/0.png",	label: " Thunderstorm"
				attributeState "nt_windy",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Windy"
				attributeState "windy-night",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Windy"
				attributeState "wind-night",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Windy"
				attributeState "wind-night!",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Dangerously Windy"
				attributeState "wind-foggy-night",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/20b.png", 	label: " Windy and Foggy"
				attributeState "wind-overcast-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24b.png", 	label: " Windy and Overcast"
				attributeState "wind-overcast-night!", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24b.png", 	label: " Dangerously Windy and Overcast"
				attributeState "wind-partlycloudy-night", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29b.png", 	label: " Windy and Partly Cloudy"
				attributeState "wind-partlycloudy-night!", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29b.png", 	label: " Dangerously Windy and Partly Cloudy"
				attributeState "wind-mostlycloudy-night", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27b.png", 	label: " Windy and Mostly Cloudy"
				attributeState "wind-mostly-cloudy-night!",	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27b.png", 	label: " Dangerously Windy and Mostly Cloudy"
				attributeState "breezy-night",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: " Breezy"
				attributeState "breezy-overcast-night",		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24b.png", 	label: " Breezy and Overcast"
				attributeState "breezy-partlycloudy-night",	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29b.png", 	label: " Breezy and Partly Cloudy"
				attributeState "breezy-mostlycloudy-night",	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27b.png", 	label: " Breezy and Mostly Cloudy"
				attributeState "breezy-foggy-night",		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/20b.png", 	label: " Breezy and Foggy"
				attributeState "nt_tornado",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/to.png",	label: " Tornado"
				attributeState "tornado-night",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/to.png",	label: " Tornado"
				attributeState "nt_hail",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46b.png",	label: " Hail"
				attributeState "hail-night",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46b.png",	label: " Hail"
                attributeState "na",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/na.png",	label: " Not Available"
				attributeState "unknown",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/na.png",	label: " Unknown"
				attributeState "hurricane",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/hc.png",	label: " Hurricane"
				attributeState "tropical-storm",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/38.png",	label: " Tropical Storm"
			}
        }    
        standardTile('moonPhase', 'device.moonPhase', decoration: 'flat', inactiveLabel: false, width: 1, height: 1) {
        	state "New", 			 label: '', icon: "https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Lunar0.png"
            state "Waxing Crescent", label: '', icon: "https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Lunar1.png"
            state "First Quarter", 	 label: '', icon: "https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Lunar2.png"
            state "Waxing Gibbous",  label: '', icon: "https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Lunar3.png"
            state "Full", 			 label: '', icon: "https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Lunar4.png"
            state "Waning Gibbous",  label: '', icon: "https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Lunar5.png"
            state "Third Quarter", 	 label: '', icon: "https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Lunar6.png"
            state "Waning Crescent", label: '', icon: "https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Lunar7.png"          
        }
        standardTile('moonDisplay', 'device.moonDisplay', decoration: 'flat', inactiveLabel: false, width: 1, height: 1) {
        	state 'Moon-waning-000', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-000.png'
        	state 'Moon-waning-005', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-005.png'
            state 'Moon-waning-010', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-010.png'
        	state 'Moon-waning-015', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-015.png'
            state 'Moon-waning-020', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-020.png'
        	state 'Moon-waning-025', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-025.png'
            state 'Moon-waning-030', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-030.png'
        	state 'Moon-waning-035', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-035.png'
            state 'Moon-waning-040', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-040.png'
        	state 'Moon-waning-045', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-045.png'
            state 'Moon-waning-050', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-050.png'
        	state 'Moon-waning-055', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-055.png'
            state 'Moon-waning-060', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-060.png'
        	state 'Moon-waning-065', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-065.png'
            state 'Moon-waning-070', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-070.png'
        	state 'Moon-waning-075', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-075.png'
        	state 'Moon-waning-080', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-080.png'
        	state 'Moon-waning-085', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-085.png'
            state 'Moon-waning-090', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-090.png'
        	state 'Moon-waning-095', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-095.png'
            state 'Moon-waning-100', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waning-100.png'
            state 'Moon-waxing-000', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-000.png'
        	state 'Moon-waxing-005', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-005.png'
            state 'Moon-waxing-010', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-010.png'
        	state 'Moon-waxing-015', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-015.png'
            state 'Moon-waxing-020', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-020.png'
        	state 'Moon-waxing-025', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-025.png'
            state 'Moon-waxing-030', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-030.png'
        	state 'Moon-waxing-035', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-035.png'
            state 'Moon-waxing-040', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-040.png'
        	state 'Moon-waxing-045', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-045.png'
            state 'Moon-waxing-050', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-050.png'
        	state 'Moon-waxing-055', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-055.png'
            state 'Moon-waxing-060', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-060.png'
        	state 'Moon-waxing-065', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-065.png'
            state 'Moon-waxing-070', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-070.png'
        	state 'Moon-waxing-075', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-075.png'
        	state 'Moon-waxing-080', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-080.png'
        	state 'Moon-waxing-085', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-085.png'
            state 'Moon-waxing-090', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-090.png'
        	state 'Moon-waxing-095', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-095.png'
            state 'Moon-waxing-100', label: '', icon: 'https://raw.githubusercontent.com/SANdood/Icons/master/Moon/Moon-waxing-100.png'
        }
        valueTile("mooninfo", "device.moonInfo", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}'
        }
        valueTile("lastSTupdate", "device.lastSTupdate", inactiveLabel: false, width: 3, height: 1, decoration: "flat", wordWrap: true) {
            state("default", label: 'Updated\nat ${currentValue}')
        }
        valueTile("attribution", "device.attribution", inactiveLabel: false, width: 3, height: 1, decoration: "flat", wordWrap: true) {
        	state("default", label: 'Powered by: ${currentValue}')
        }
        valueTile("heatIndex", "device.heatIndexDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Heat\nIndex\n${currentValue}'
        }
        valueTile("windChill", "device.windChillDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Wind\nChill\n${currentValue}'
        }
        valueTile("weather", "device.weather", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'${currentValue}'
        }
        valueTile("todayTile", "device.locationName", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'TDY\n(act)'
        }
        valueTile("todayFcstTile", "device.locationName", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'TDY\n(fcst)'
        }
        valueTile("yesterdayTile", "device.locationName", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'YDA\n(act)'
        }
        valueTile("tomorrowTile", "device.locationName", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'TMW\n(fcst)'
        }
        valueTile("precipYesterday", "device.precipYesterdayDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Precip\nYDA\n${currentValue}'
        }
        valueTile("precipToday", "device.precipTodayDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Precip\nTDY\n${currentValue}'
        }
        valueTile("precipFcst", "device.precipFcstDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Precip\nTDY\n~${currentValue}'
        }
        valueTile("precipTom", "device.precipTomDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Precip\nTMW\n~${currentValue}'
        }
        valueTile("precipLastHour", "device.precipLastHourDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Precip\nlast hr\n${currentValue}'
        }
        valueTile("precipRate", "device.precipRateDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Precip\nper hr\n${currentValue}'
        }
        standardTile("refresh", "device.weather", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: "", action: "refresh", icon:"st.secondary.refresh"
        }
        valueTile("forecast", "device.forecast", inactiveLabel: false, width: 5, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'${currentValue}'
        }
        valueTile("sunrise", "device.sunriseAPM", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Sun\nRises\n${currentValue}'
        }
        valueTile("sunset", "device.sunsetAPM", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Sun\nSets\n${currentValue}'
        }
        valueTile("moonrise", "device.moonriseAPM", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Moon\nRises\n${currentValue}'
        }
        valueTile("moonset", "device.moonsetAPM", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Moon\nSets\n${currentValue}'
        }
        valueTile("daylight", "device.dayHours", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Daylight\nHours\n${currentValue}'
        }
        valueTile("light", "device.illuminance", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Illum\n${currentValue}\nlux'
        }
        valueTile("pop", "device.popDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'${currentValue}'
        }
        valueTile("popFcst", "device.popFcstDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'${currentValue}'
        }
        valueTile("popTom", "device.popTomDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'${currentValue}'
        }
        valueTile("evo", "device.etDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'ET\nlast hr\n${currentValue}'
        }
        valueTile("uvIndex", "device.uvIndex", inactiveLabel: false, decoration: "flat") {
            state "uvIndex", label: 'UV\nIndex\n${currentValue}'
        }
        standardTile("water", "device.water", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'updating...', 	icon: "st.unknown.unknown.unknown"
            state "wet",  	 label: 'wet',			icon: "st.alarm.water.wet",        backgroundColor:"#00A0DC"
            state "dry",     label: 'dry',			icon: "st.alarm.water.dry",        backgroundColor:"#FFFFFF"
        }
        valueTile("dewpoint", "device.dewpoint", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Dew\nPoint\n${currentValue}°'
        }
        valueTile("pressure", "device.pressureDisplay", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}'
        }
        valueTile("solarRadiation", "device.solarRadiation", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "solarRadiation", label: 'SolRad\n${currentValue}\nW/m²'
        }
        valueTile("windinfo", "device.windinfo", inactiveLabel: false, width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "windinfo", label: '${currentValue}'
        }
        valueTile('aqi', 'device.airQualityIndex', inactiveLabel: false, width: 1, height: 1, decoration: 'flat', wordWrap: true) {
        	state 'default', label: 'AQI\n${currentValue}',
            	backgroundColors: [
                	[value:   0, color: '#44b621'],		// Green - Good
                    [value:  50, color: '#44b621'],
                    [value:  51, color: '#f1d801'],		// Yellow - Moderate
                    [value: 100, color: '#f1d801'],
                    [value: 101, color: '#d04e00'],		// Orange - Unhealthy for Sensitive groups
                    [value: 150, color: '#d04e00'],
                    [value: 151, color: '#bc2323'],		// Red - Unhealthy
                    [value: 200, color: '#bc2323'],
                    [value: 201, color: '#800080'],		// Purple - Very Unhealthy
                    [value: 300, color: '#800080'],
                    [value: 301, color: '#800000']		// Maroon - Hazardous
                ]
        }
        valueTile("temperature2", "device.temperature", width: 1, height: 1, canChangeIcon: true) {
            state "temperature", label: '${currentValue}°', icon: 'st.Weather.weather2',
				backgroundColors: (temperatureColors)
        }
        valueTile("highTempYday", "device.highTempYesterday", width: 1, height: 1, canChangeIcon: true) {
            state "temperature", label: '${currentValue}°',
				backgroundColors: (temperatureColors)
        }
        valueTile("lowTempYday", "device.lowTempYesterday", width: 1, height: 1, canChangeIcon: true) {
            state "temperature", label: '${currentValue}°',
				backgroundColors: (temperatureColors)
        }
        valueTile("highTemp", "device.highTemp", width: 1, height: 1, canChangeIcon: true) {
            state "temperature", label: '${currentValue}°',
				backgroundColors: (temperatureColors)
        }
        valueTile("lowTemp", "device.lowTemp", width: 1, height: 1, canChangeIcon: true) {
            state "temperature", label: '${currentValue}°',
				backgroundColors: (temperatureColors)
        }
        valueTile("highTempFcst", "device.highTempForecast", width: 1, height: 1, canChangeIcon: true) {
            state "temperature", label: '${currentValue}°',
				backgroundColors: (temperatureColors)
        }
        valueTile("lowTempFcst", "device.lowTempForecast", width: 1, height: 1, canChangeIcon: true) {
            state "temperature", label: '${currentValue}°',
				backgroundColors: (temperatureColors)
        }
        valueTile("highTempTom", "device.highTempTomorrow", width: 1, height: 1, canChangeIcon: true) {
            state "temperature", label: '${currentValue}°',
				backgroundColors: (temperatureColors)
        }
        valueTile("lowTempTom", "device.lowTempTomorrow", width: 1, height: 1, canChangeIcon: true) {
            state "temperature", label: '${currentValue}°',
				backgroundColors: (temperatureColors)
        }
        valueTile("humidity", "device.humidity", decoration: "flat", width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
          		[value: 10, color: "#00BFFF"],
                [value: 100, color: "#ff66ff"]
            ] )
		}
        valueTile("lowHumYday", "device.lowHumYesterday", decoration: "flat", width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
          		[value: 10, color: "#00BFFF"],
                [value: 100, color: "#ff66ff"]
            ] )
		}
        valueTile("highHumYday", "device.highHumYesterday", decoration: "flat", width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
          		[value: 10, color: "#00BFFF"],
                [value: 100, color: "#ff66ff"]
            ] )
		}
        valueTile("lowHumidity", "device.lowHumidity", decoration: "flat", width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
          		[value: 10, color: "#00BFFF"],
                [value: 100, color: "#ff66ff"]
            ] )
		}
        valueTile("highHumidity", "device.highHumidity", decoration: "flat", width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
          		[value: 10, color: "#00BFFF"],
                [value: 100, color: "#ff66ff"]
            ] )
		}
        valueTile("avgHumFcst", "device.avgHumForecast", decoration: "flat", width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
          		[value: 10, color: "#00BFFF"],
                [value: 100, color: "#ff66ff"]
            ] )
		}
        valueTile("avgHumTom", "device.avgHumTomorrow", decoration: "flat", width: 1, height: 1) {
			state("default", label: '${currentValue}%', unit: "%", defaultState: true, backgroundColors: [ //#d28de0")
          		[value: 10, color: "#00BFFF"],
                [value: 100, color: "#ff66ff"]
            ] )
		}
        valueTile("respTime", "device.respTime", inactiveLabel: false, width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Response Time\n${currentValue} seconds'
        }
		valueTile("respAvg", "device.respAvg", inactiveLabel: false, width: 2, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: 'Avg Resp Time\n${currentValue} seconds'
        }
        standardTile('oneByTwo', 'device.logo', width: 1, height: 2, decoration: 'flat') {
        	state "default", defaultState: true
        }
        standardTile('oneByOne', 'device.logo', width: 1, height: 1, decoration: 'flat') {
        	state "default", defaultState: true
        }
        main([/* "temperature2", */'temperatureDisplay'])
        details([	"temperatureDisplay",  
        			'aqi', "heatIndex", "dewpoint", 'windChill', "pressure", 'humidity', 
                    "moonDisplay", "mooninfo", "windinfo", "evo", "water", 
                    "solarRadiation", "light", "uvIndex", "precipToday",  "precipRate", "precipLastHour", 
                    "forecast", 'pop',  
                    "sunrise", "sunset", "daylight", "moonrise", "moonset",  'refresh', 
                    'yesterdayTile', 'lowTempYday', 'highTempYday', 'lowHumYday', 'highHumYday', "precipYesterday",
                    "todayTile", 'lowTemp', 'highTemp', 'lowHumidity', 'highHumidity', 'precipToday',
                    'todayFcstTile', 'lowTempFcst', 'highTempFcst', 'avgHumFcst', 'popFcst', 'precipFcst',
                    'tomorrowTile', 'lowTempTom', 'highTempTom', 'avgHumTom', 'popTom', 'precipTom',
                    'lastSTupdate', 'attribution',
                    'respTime', 'respAvg'
               ])
    }
}
def noOp() {}
// ****************************************************************************************************
// Initialization, Setup and Operation
// ***********************************
def parse(String description) {
    if (debugOn) log.debug "Parsing '${description}'"
}
def installed() {
	if (!state.hubPlatform) log.info "Installing on ${getHubPlatform()}"
	state.meteoWeather = [:]
    state.darkSkyWeather = [:]
    state.twcConditions = [:]
    state.twcForecast = [:]
	initialize()
}
def uninstalled() {
	unschedule()
}
def updated() {
	log.info "Updated, settings: ${settings}"
    state.hubPlatform = null; getHubPlatform();		// Force hub update if we are updated...just in case
    state.meteoWeatherVersion = getVersionLabel()
    unschedule()
    initialize()
}
def rescheduleMeteoWeather() {
	unschedule(getMeteoWeather)
	
	def t = settings.updateMins ?: '5'
    if (t == '1') {
    	log.info "Rescheduling for every minute"
    	runEvery1Minute(getMeteoWeather)
    } else {
    	log.info "Rcheduling for every ${t} minutes"
    	"runEvery${t}Minutes"(getMeteoWeather)
    }
}
def initialize() {
	log.info getVersionLabel() + " on ${state.hubPlatform} Initializing..."
    def poweredBy = "MeteoBridge"
	if (settings.shortStats) log.info "Using optimized MeteoBridge template"
    def endBy = ''
    state.respTotal = 0
    state.respCount = 0
    
    // Create the template using the latest preferences values (components defined at the bottom)
	send(name:'hubAction', value: null, displayed: false)
	send(name:'purpleAir', value: null, displayed: false)
	send(name:'darkSkyWeather', value: null, displayed: false)
	
    state.meteoTemplate = ((fcstSource && (fcstSource == 'meteo'))? forecastTemplate : '') + currentTemplate
    state.templateReload = true
    if (debugOn) log.debug "initial meteoTemplate: " + state.meteoTemplate
    
    if (debugOn) send(name: 'meteoTemplate', value: state.meteoTemplate, displayed: false, isStateChange: true)
    
	// state.iconStore = "https://raw.githubusercontent.com/SANdood/Icons/master/Weather/"
    state.iconStore = "https://github.com/SANdood/Icons/blob/master/Weather/"
	
    def userpassascii = meteoUser + ':' + meteoPassword
	if (state.isST) {
    	state.userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    } else {
    	state.userpass = "Basic " + userpassascii.bytes.encodeBase64().toString()
    }
    
    // Schedule the updates
    state.today = null
    runIn(5,getMeteoWeather)						// Have to wait to let the state changes settle
    
    def t = settings.updateMins ?: '5'
    if (t == '1') {
    	log.info "scheduling for every minute"
    	runEvery1Minute(getMeteoWeather)
    } else {
    	log.info "scheduling for every ${t} minutes"
    	"runEvery${t}Minutes"(getMeteoWeather)
    }
    
    state.twcForTomorrow = false
    state.wunderForTomorrow = false
    if ((fcstSource && (fcstSource != 'darksky')) || (darkSkyKey == null)){
    	if (fcstSource && (fcstSource == 'wunder')) {
			state.wunderForTomorrow = false 					// (fcstSource && (fcstSource == 'meteo')) ? true : false
    		//runEvery10Minutes(updateWundergroundTiles)		// This doesn't change all that frequently
    		//updateWundergroundTiles()
            send(name: 'twcConditions', value: null, displayed: false)
    		send(name: 'twcForecast', value: null, displayed: false)
            send(name: 'wundergroundObs', value: null, displayed: false)
            log.error "WeatherUnderground (getWeatherFeature) has been dreprecated by SmartThings and is no longer supported!"
            // endBy= ' and Weather Underground'
        } else if ((fcstSource && ((fcstSource == 'twc') || (fcstSource == 'meteo'))) || (darkSkyKey == null)) {
    		state.twcForTomorrow = (fcstSource && (fcstSource == 'meteo')) // ? true : false
            runEvery10Minutes(updateTwcTiles)
            updateTwcTiles()
            send(name: 'wundergroundObs', value: null, displayed: false)
            endBy += ' and The Weather Company'
    	}
    }
    if (fcstSource && (fcstSource == 'darksky') && (darkSkyKey != null)) {
    	endBy = (endBy == '') ? ' and Dark Sky' : ', Dark Sky' + endBy
    	runEvery15Minutes(getDarkSkyWeather)			// Async Dark Sky current & forecast weather
        send(name: 'twcConditions', value: null, displayed: false)
    	send(name: 'twcForecast', value: null, displayed: false)
        send(name: 'wundergroundObs', value: null, displayed: false)
		log.info "Using DarkSky"
        getDarkSkyWeather()
    } 
    if (purpleID) {
    	endBy = (endBy == '') ? ' and PurpleAir' : ', PurpleAir' + endBy
    	runEvery3Minutes(getPurpleAirAQI)				// Async Air Quality
    	// getPurpleAirAQI()
    }
    
    poweredBy += endBy
    send(name: 'attribution', value: poweredBy, displayed: false, isStateChange: true)
    log.info "Initialization complete..."
}
def runEvery2Minutes(handler) {
	Random rand = new Random()
    //log.debug "Random2: ${rand}"
    int randomSeconds = rand.nextInt(59)
	schedule("${randomSeconds} 0/2 * * * ?", handler)
}
def runEvery3Minutes(handler) {
	Random rand = new Random()
    //log.debug "Random3: ${rand}"
    int randomSeconds = rand.nextInt(59)
	schedule("${randomSeconds} 0/3 * * * ?", handler)
}
def poll() { refresh() }
def refresh() { 
	state.today = null
	getMeteoWeather( false )
    if (darkSkyKey != null) {
    	getDarkSkyWeather()
    } 
    if (fcstSource) {
		if (state.twcForTomorrow || (fcstSource == 'twc')) updateTwcTiles()
    }
    getPurpleAirAQI() 
}
// def getWeatherReport() { return state.meteoWeather }
def configure() { updated() }
// ***********************************************************************************************************************
// Weather Data Handlers
// *********************
def getMeteoWeather( yesterday = false) {
	log.info "Polling..."
    if (debugOn) log.trace "getMeteoWeather( yesterday = ${yesterday} )"
    if (!state.meteoWeatherVersion || (state.meteoWeatherVersion != getVersionLabel())) {
    	// if the version level of the code changes, silently run updated() and initialize()
        log.info 'Version changed, updating...'
    	runIn(2, updated, [overwrite: true])
        //updated()
        return
    }
    if (device.currentValue('highTempYesterday') == null) { log.info 'Forcing yesterday'; yesterday = true; }
    
    // Create the hubAction request based on updated preferences
    def hubAction
    if (state.isST) {
        hubAction = physicalgraph.device.HubAction.newInstance(
            method: 'GET',
            path: '/cgi-bin/template.cgi',
            headers: [ HOST: "${settings.meteoIP}:${settings.meteoPort}", 'Authorization': state.userpass ],
            query: ['template': "{\"timeGet\":${now()}," + MBSystemTemplate + (yesterday ? yesterdayTemplate : state.meteoTemplate ), 'contenttype': 'application/json' ],
            null,
            [callback: meteoWeatherCallback]
        )
    } else {
        hubAction = hubitat.device.HubAction.newInstance(
            method: 'GET',
            path: '/cgi-bin/template.cgi',
            headers: [ HOST: "${settings.meteoIP}:${settings.meteoPort}", 'Authorization': state.userpass ],
            query: ['template': "{\"timeGet\":${now()}," + MBSystemTemplate + (yesterday ? yesterdayTemplate : state.meteoTemplate), 'contenttype': 'application/json' ],
            null,
            [callback: meteoWeatherCallback, timeout: 20]
        )
    }
    if (debugOn) send(name: 'hubAction', value: hubAction, displayed: false, isStateChange: true)
	if (debugOn) log.debug "hubAction (${hubAction.toString().size()}): ${hubAction.toString()}"

	// log.debug "query (raw): ${hubAction}"
        
    try {
    	state.callStart = yesterday ? null : now()
        sendHubCommand(hubAction)
    } catch (Exception e) {
    	log.error "getMeteoWeather() sendHubCommand Exception ${e} on ${hubAction}"
    }
    if (debugOn) log.info 'getMeteoWeather() update request sent'
}
def meteoWeatherCallback(hubResponse) {
	if (state.callStart) state.callEnd = now() 
	if (debugOn) log.trace "meteoWeatherCallback() response recieved: " + hubResponse.status
    if (debugOn) log.debug "meteoWeatherCallback() headers: " + hubResponse.headers
    if (hubResponse.status == 200) {
    	if (debugOn) log.debug "hubResponse.body: ${hubResponse.body}"
	    if (hubResponse.json) {
			state.meteoWeather = hubResponse.json
	        if (debugOn) send(name: 'meteoWeather', value: hubResponse.json, displayed: false, isStateChange: true)
        } else if (hubResponse.body) {
        	// Hubitat doesn't do the json conversion for us
            log.warn "Only got hubResponse.body (isHE: ${state.isHE})"
			state.meteoWeather = new JsonSlurper().parseText(hubResponse.body)
        	if (debugOn) send(name: 'meteoWeather', value: state.meteoWeather, displayed: false, isStateChange: true)
        }
        if (debugOn) log.debug "state.meteoWeather: ${state.meteoWeather}"
        if (state.templateReload && state.meteoWeather?.version) {
        	// We have to reload the template after the first call, so that we can utilize the version # to determine sensor ID (0 or *)
        	state.meteoTemplate = ((fcstSource && (fcstSource == 'meteo'))? forecastTemplate : '') + currentTemplate
    		state.templateReload = false
    		log.info "updated meteoTemplate: " + state.meteoTemplate
            if (debugOn) send(name: 'meteoTemplate', value: state.meteoTemplate, displayed: false, isStateChange: true)
        }
        def isDayNow = device.currentValue('isDay')		// the value BEFORE we apply this new data
        updateWeatherTiles()							// This will update the new isDay value
        if (isDayNow != device.currentValue('isDay')) getDarkSkyWeather()	// redo icons when day/night changes
        
        def elapsed = (state.callStart && state.callEnd) ? state.callEnd - state.callStart : null
        if (debugOn) log.debug "start: ${state.callStart}, end: ${state.callEnd}, elapsed: ${elapsed}"
        if (elapsed != null) {
			state.respTotal = state.respTotal + elapsed
			state.respCount = state.respCount + 1
			def respAvg = state.respTotal / state.respCount
            def rt = roundIt((elapsed/1000.0),3)
            def ra = roundIt((respAvg/1000.0),3)
			send(name: 'respTime', value: rt, displayed: false)
			send(name: 'respAvg', value: ra, displayed: false)
            log.info "...done! Response Time/Avg: ${rt}/${ra} seconds"
		}
        if (debugOn) log.trace "meteoWeatherCallback() finished"
        return
    } else {
		if ((hubResponse.status == 503) || (hubResponse.status == 504)) {
			log.warn "meteoWeatherCallback() - MeteoBridge server is busy or overloaded, rescheduling for a new time slot (${hubResponse.status})"
			rescheduleMeteoWeather()
		} else {
    		log.error "meteoWeatherCallback() - Invalid response from MeteoBridge server, will retry next cycle (${hubResponse.status})"
		}		
        return
    }
}
def updateWeatherTiles() {
	if (debugOn) log.trace "updateWeatherTiles() entered"
    if (state.meteoWeather != [:]) {
        if (debugOn) log.debug "meteoWeather: ${state.meteoWeather}"
		String unit = getTemperatureScale()
        String h = (height_units && (height_units == 'height_in')) ? '"' : 'mm'
        int hd = (h = '"') ? 2 : 1		// digits to store & display
        int ud = unit=='F' ? 0 : 1
		String s = (speed_units && (speed_units == 'speed_mph')) ? 'mph' : 'kph'
		String pv = (pres_units && (pres_units == 'press_in')) ? 'inHg' : 'mmHg'
        def val, t, td, hum, uv, sr, pr
        
	// myTile defines
		// state.MTcity = device.currentValue('city')
		// state.MTstate = device.currentValue('state')
		// state.MTcountry = device.currentValue('country')
        state.MTlocation = location.name
		state.MTtemperature = device.currentValue('temperature')										// Keep
		//state.MThumidity = device.currentValue('humidity')
		state.MTwind = 0
		state.MTgust = 0
		state.MTwindDir = 0
        //state.MTsolarPwr = ""
        // state.MTuvi = null
		state.MTpressure = device.currentValue('pressure')
        state.MTpressTrend
		state.MTsunrise = device.currentValue('sunriseAPM')
		state.MTsunset = device.currentValue('sunsetAPM')
		state.MTpop = device.currentValue('pop')
		state.MTicon = device.currentValue('weatherIcon')
		state.MTweather = device.currentValue('weather')
		
		// This is the OpenWeatehrMap icon - used by HE Dashboard weather tile
		// send(name: "weatherIcons", value: getOwmIcon(state.MTicon), displayed: false)
			 
	// Forecast Data
        if (!fcstSource || (fcstSource == 'meteo')) {
        	if (debugOn) log.debug "updateWeatherTiles() - updating meteo forecast()"
            if (state.meteoWeather.fcast?.text != "") {
                send(name: 'forecast', value: state.meteoWeather.fcast?.text, descriptionText: "Davis Forecast: " + state.meteoWeather.fcast?.text)
                send(name: "forecastCode", value: state.meteoWeather.fcast?.code, descriptionText: "Davis Forecast Rule #${state.meteoWeather.fcast?.code}")
            } else {
                // If the Meteobridge isn't providing a forecast (only provided for SOME Davis weather stations), use the one from The Weather Channel
                state.twcForTomorrow = true
            }
        }
        
    // Yesterday data
        if (state.meteoWeather?.yesterday) {
        	if (debugOn) log.debug "updateWeatherTiles() - handling yesterday's data"
            if (debugOn && settings.shortStats) log.debug "state.meteoWeather.yesterday: ${state.meteoWeather.yesterday}"
            
            val = settings.shortStats ? state.meteoWeather.yesterday[0] : state.meteoWeather.yesterday.highTemp
            if ((val != null) && (val == "")) val = null
        	t = roundIt(val, ud)
        	send(name: 'highTempYesterday', value: t, unit: unit, descriptionText: "High Temperature yesterday was ${t}°${unit}")
            
            val = settings.shortStats ? state.meteoWeather.yesterday[1] : state.meteoWeather.yesterday.lowTemp
            if ((val != null) && (val == "")) val = null
        	t = roundIt(val, ud)
            send(name: 'lowTempYesterday', value: t, unit: unit, descriptionText: "Low Temperature yesterday was ${t}°${unit}")
            
            val = settings.shortStats ? state.meteoWeather.yesterday[2] : state.meteoWeather.yesterday.highHum
            if ((val != null) && (val == "")) val = null
        	hum = roundIt(val, 0)
            send(name: 'highHumYesterday', value: hum, unit: "%", descriptionText: "High Humidity yesterday was ${hum}%")
            
            val = settings.shortStats ? state.meteoWeather.yesterday[3] : state.meteoWeather.yesterday.lowHum
            if ((val != null) && (val == "")) val = null
        	hum = roundIt(val, 0)
            send(name: 'lowHumYesterday', value: hum, unit: "%", descriptionText: "Low Humidity yesterday was ${hum}%")
            
            def ry = settings.shortStats ? state.meteoWeather.yesterday[4] : state.meteoWeather.yesterday.rainfall
            if ((ry != null) && (ry == "")) ry = null
            def ryd = roundIt(ry, hd + 1)		// Internally keep 1 more digit of precision than we display
            def rydd = roundIt(ry, hd)
            if (ryd != null) {
				send(name: 'precipYesterday', value: ryd, unit: "${h}", descriptionText: "Precipitation yesterday was ${ryd}${h}")
                send(name: 'precipYesterdayDisplay', value: "${rydd}${h}", displayed: false)
            } else send(name: 'precipYesterdayDisplay', value: '--', displayed: false)
        }

    // Today data
		if (state.meteoWeather?.current) { 
        	if (debugOn) log.debug "updateWeatherTiles() - handling today's data"
            val = settings.shortStats ? state.meteoWeather.current[36] : state.meteoWeather.current.isDay
        	if ((val != null) && (val == "")) val = null
            if ((val != null) && (val != device.currentValue('isDay'))) {
            	if (debugOn) log.debug "updateWeatherTiles() - updating day/night"
				if (fcstSource) {
					if (state.twcForTomorrow || (fcstSource == 'twc')) updateTwcTiles()
				}
                if (val == 1) {
                	send(name: 'isDay', value: 1, displayed: true, descriptionText: 'Daybreak (civil sunrise)' )
                	send(name: 'isNight', value: 0, displayed: true, descriptionText: 'Dawn begins' )
                } else {
					send(name: 'isDay', value: 0, displayed: true, descriptionText: 'Nightfall (civil sunset)')
                    send(name: 'isNight', value: 1, displayed: true, descriptionText: 'Dusk begins')
                }
            }
            
        // Temperatures
        	def theTemp = null
            state.MTfeelsLike = ""
            val = settings.shortStats ? state.meteoWeather.current[4] : state.meteoWeather.current.tempOut
            if ((val != null) && (val == "")) val = null
            if (val != null) {
            	if (debugOn) log.debug "updateWeatherTiles() - updating temperatures"
            // Outdoor temp
                theTemp = val
                td = roundIt(val, ud+1)
                send(name: "temperature", value: td, unit: unit, descriptionText: "Temperature is ${td}°${unit}")
                state.MTtemperature = roundIt(val, 1)
                if (state.isST) {
                    send(name: "temperatureDisplay", value: td.toString(), unit: unit, displayed: false, descriptionText: "Temperature display is ${td}°${unit}")
                }
            // High temp so far today
                val = settings.shortStats ? state.meteoWeather.current[10] : state.meteoWeather.current.highTemp
            	if ((val != null) && (val == "")) val = null
                t = roundIt(val, ud)
                send(name: "highTemp", value: t, unit: unit, descriptionText: "High Temperature so far today is ${t}°${unit}")
            // Low temp so far today
                val = settings.shortStats ? state.meteoWeather.current[11] : state.meteoWeather.current.lowTemp
            	if ((val != null) && (val == "")) val = null
                t = roundIt(val, ud)
                send(name: "lowTemp", value: t , unit: unit, descriptionText: "Low Temperature so far today is ${t}°${unit}")
            // Heat Index
                val = settings.shortStats ? state.meteoWeather.current[6] : state.meteoWeather.current.heatIndex
            	if ((val != null) && (val == "")) val = null
                t = roundIt(val, ud+1)
                if ((t != null) && (theTemp != val)) {
                    send(name: "heatIndex", value: t , unit: unit, displayed: true, descriptionText: "Heat Index is ${t}°${unit}")
                    state.MTfeelsLike = roundIt(t, 1)
                    if (state.isST) send(name: "heatIndexDisplay", value: "${t}°", unit: unit, displayed: false, descriptionText: '')
                } else if (t != null) {
                    send(name: 'heatIndex', value: t, unit: unit, descriptionText: "Heat Index is ${t}°${unit} - same as current temperature")
                    if (state.isST) send(name: 'heatIndexDisplay', value: '=', displayed: false)
                } else {
                	send(name: 'heatIndex', value: null, unit: unit, displayed: false)
                }
            // Indoor Temp
                val = settings.shortStats ? state.meteoWeather.current[8] : state.meteoWeather.current.tempIn
            	if ((val != null) && (val == "")) val = null
                td = roundIt(val, 2)
                send(name: "indoorTemperature", value: td, unit: unit, descriptionText: "Indoor Temperature is ${td}°${unit}")
			// Outdoor Dewpoint
            	val = settings.shortStats ? state.meteoWeather.current[5] : state.meteoWeather.current.dewOut
            	if ((val != null) && (val == "")) val = null
                // if (val == null) val = calculateDewpoint()
                t = roundIt(val, ud+1)
                send(name: "dewpoint", value: t , unit: unit, descriptionText: "Dew Point is ${t}°${unit}")
            // Indoor Dewpoint
            	val = settings.shortStats ? state.meteoWeather.current[9] : state.meteoWeather.current.dewIn
            	if ((val != null) && (val == "")) val = null
                t = roundIt(val, 2)
                send(name: "indoorDewpoint", value: t, unit: unit, descriptionText: "Indoor Dewpoint is ${t}°${unit}")
            // Wind Chill
            	val = settings.shortStats ? state.meteoWeather.current[7] : state.meteoWeather.current.windChill
            	if ((val != null) && (val == "")) val = null
                t = roundIt(val, ud+1)
                if (t != null) {
                	if (isStateChange( device, 'windChill', t as String) || (state.MTfeelsLike == "")) {
                        if (theTemp != val) {			
                            if (state.MTfeelsLike == "") state.MTfeelsLike = roundIt(t, 1)
                            send(name: "windChill", value: t, unit: unit, displayed: true, descriptionText: "Wind Chill is ${t}°${unit}", isStateChange: true)
                            if (state.isST) send(name: "windChillDisplay", value: "${t}°", unit: unit, displayed: false, isStateChange: true, descriptionText: '')
                        } else {
                            send(name: 'windChill', value: t, unit: unit, descriptionText: "Wind Chill is ${t}°${unit} - same as current temperature", isStateChange: true)
                            if (state.isST) send(name: 'windChillDisplay', value: '=', displayed: false, isStateChange: true)
                        }
                    }
                } else {
                    // if the Meteobridge weather station doesn't have an anemometer, we won't get a wind chill value
                    send(name: 'windChill', value: null, displayed: false)
                    if (state.isST) send(name: 'windChillDisplay', value: null, displayed: false)
                }
                if (state.MTfeelsLike == "") state.MTfeelsLike = state.MTtemperature
                send(name: 'feelsLike', value: state.MTfeelsLike, unit: unit, displayed: true, descriptionText: "Feels Like temperature is ${t}°${unit}")
            }
			
        // Humidity
        	val = settings.shortStats ? state.meteoWeather.current[2] : state.meteoWeather.current.humOut
            if ((val != null) && (val == "")) val = null
            if (val != null) {
                hum = roundIt(val, ud)
                state.MThumidity = hum
                if (isStateChange(device, 'humidity', hum as String)) {
                    if (debugOn) log.debug "updateWeatherTiles() - updating humidity"
                    send(name: "humidity", value: hum, unit: "%", descriptionText: "Humidity is ${hum}%", isStateChange: true)
                    val = settings.shortStats ? state.meteoWeather.current[24] : state.meteoWeather.current.highHum
            		if ((val != null) && (val == "")) val = null
                    hum = roundIt(val, 0)
                    send(name: "highHumidity", value: hum, unit: "%", descriptionText: "High Humidity so far today is ${hum}%")
                    val = settings.shortStats ? state.meteoWeather.current[25] : state.meteoWeather.current.lowHum
            		if ((val != null) && (val == "")) val = null
                    hum = roundIt(val, 0)
                    send(name: "lowHumidity", value: hum, unit: "%", descriptionText: "Low Humidity so far today is ${hum}%")                
                }
            } else {
            	send(name: 'humidity', value: null, unit: '%')
                send(name: 'highHumidity', value: null, unit: '%')
                send(name: 'lowHumidity', value: null, unit: '%')
                state.MThumidity = null
            }
        // Indoor Humidity
            val = settings.shortStats ? state.meteoWeather.current[3] : state.meteoWeather.current.humIn
            if ((val != null) && (val == "")) val = null
			hum = roundIt(val, 0)
        	send(name: "indoorHumidity", value: hum, unit: "%", descriptionText: "Indoor Humidity is ${hum}%")
            
        // Ultraviolet Index
        	val = settings.shortStats ? state.meteoWeather.current[29] : state.meteoWeather.current.uvIndex
            if ((val != null) && (val == "")) val = null
            if (val != null) {
            	if (debugOn) log.debug "updateWeatherTiles() - updating UV Index from MeteoBridge"
				uv = roundIt(val, 1)
                state.MTuvi = uv
                if (isStateChange(device, 'uvIndex', uv as String)) {
            		send(name: "uvIndex", value: uv, unit: 'uvi', descriptionText: "UV Index is ${uv}", displayed: false, isStateChange: true)
            		send(name: 'ultravioletIndex', value: uv, unit: 'uvi', descriptionText: "Ultraviolet Index is ${uv}", isStateChange: true)
                }
            } else if (state.darkSkyWeather?.uvIndex) {
            // use the DarkSky data, if available
            	if (debugOn) log.debug "updateWeatherTiles() - updating UV Index from DarkSky"
            	uv = roundIt(state.darkSkyWeather.uvIndex, 1)		// UVindex can be null
                state.MTuvi = uv
                if (isStateChange(device, 'uvIndex', uv as String)) {
            		send(name: "uvIndex", value: uv, unit: 'uvi', descriptionText: "UV Index is ${uv}", displayed: false, isStateChange: true)
            		send(name: 'ultravioletIndex', value: uv, unit: 'uvi', descriptionText: "Ultraviolet Index is ${uv}", isStateChange: true)
                }
            } else if (state.isST && state.twcConditions?.uvIndex) {
            // use the TWC data, if available
            	if (debugOn) log.debug "updateWeatherTiles() - updating UV Index from TWC"
            	uv = roundIt(state.twcConditions.uvIndex, 1)		// UVindex can be null
                state.MTuvi = uv
                if (isStateChange(device, 'uvIndex', uv as String)) {
            		send(name: "uvIndex", value: uv, unit: 'uvi', descriptionText: "UV Index is ${uv}", displayed: false, isStateChange: true)
            		send(name: 'ultravioletIndex', value: uv, unit: 'uvi', descriptionText: "Ultraviolet Index is ${uv}", isStateChange: true)
                }
            } else {
            	send(name: "uvIndex", value: null, unit: 'uvi', displayed: false)
            	send(name: 'ultravioletIndex', value: null, unit: 'uvi', displayed: false)
                state.MTuvi = null
            }           
           
        // Solar Radiation
        	val = settings.shortStats ? state.meteoWeather.current[30] : state.meteoWeather.current.solRad
            if ((val != null) && (val == "")) val = null
            if (val != null) {
            	if (debugOn) log.debug "updateWeatherTiles() - updating solarRadiation"
            	sr = roundIt(val, 0)
				send(name: "solarRadiation", value: sr, unit: 'W/m²', descriptionText: "Solar radiation is ${val} W/m²")
                state.MTsolarPwr = sr
            } else {
            	send(name: "solarRadiation", value: "", displayed: false)
                state.MTsolarPwr = null
            }
        
		// Barometric Pressure  
        	val = settings.shortStats ? state.meteoWeather.current[16] : state.meteoWeather.current.pressure
            if ((val != null) && (val == "")) val = null
            if (val != null) {
            	pr = roundIt(val, 2)
            	if (debugOn) log.debug "updateWeatherTiles() - updating barometric pressure()"
                def pressure_trend_text
                val = settings.shortStats ? state.meteoWeather.current[17] : state.meteoWeather.current.pTrend
            	if ((val != null) && (val == "")) val = null
                if ((pr != state.MTpressure) || (val != device.currentValue('pressureTrend'))) { 
                    state.MTpressure = pr
                    switch (val) {
                        case "FF": pressure_trend_text = "➘ Fast";	break;
                        case "FS": pressure_trend_text = "➘ Slow";	break;
                        case "ST": pressure_trend_text = "Steady";	break;
                        case "--": pressure_trend_text = '➙';		break;
                        case "RS": pressure_trend_text = '➚ Slow';	break;
                        case "RF": pressure_trend_text = "➚ Fast";	break;
                        default:   pressure_trend_text = null; 		break;
                    }
                    state.MTpressTrend = (pressure_trend_text != 'Steady') ? pressure_trend_text : '➙'
                }
                send(name: 'pressure', value: pr, unit: pv, displayed: false, descriptionText: "Barometric Pressure is ${pr} ${pv}", isStateChange: true)
                if (state.isST) send(name: 'pressureDisplay', value: "${pr}\n${pv}\n${pressure_trend_text}", descriptionText: "Barometric Pressure is ${pr} ${pv} - ${pressure_trend_text}", isStateChange: true)
                send(name: 'pressureTrend', value: pressure_trend_text, displayed: false, descriptionText: "Barometric Pressure trend over last 10 minutes is ${pressure_trend_text}")
            } else {
                send(name: 'pressure', value: null, displayed: false)
                send(name: 'pressureTrend', value: "", displayed: false)
                if (state.isST) send(name: 'pressureDisplay', value: "--", displayed: false)
                state.MTpressure = ""
                state.MTpressTrend = ""
            }
            
		// Rainfall
        	def rlh = null
        	val = settings.shortStats ? state.meteoWeather.current[12] : state.meteoWeather.current.rainfall
            if ((val != null) && (val == "")) val = null
            if (val != null) {
            	if (debugOn) log.debug "updateWeatherTiles() - updating rainfall"
            	def rtd = roundIt(val, hd+1)
                def rtdd = state.isST ? roundIt(val, hd) : null
                if (rtd != null) {
                    if (state.isST) send(name: 'precipTodayDisplay', value:  "${rtdd}${h}", displayed: false)
                    send(name: 'precipToday', value: rtd, unit: "${h}", descriptionText: "Precipitation so far today is ${rtd}${h}")
                    state.MTprecipToday = rtd
                } else {
                    if (state.isST) send(name: 'precipTodayDisplay', value:'0.00', displayed: false)
                    state.MTprecipToday = 0.0
                }
            // Rainfall last hour
            	val = settings.shortStats ? state.meteoWeather.current[13] : state.meteoWeather.current.rainLastHour
                if ((val != null) && (val == "")) val = null
                if (val != null) {
                    rlh = roundIt(val, hd+1)
                	if (device.currentValue('precipLastHour') != rlh) {
                        def rlhd = state.isST ? roundIt(val, hd) : null
                        if (state.isST) send(name: 'precipLastHourDisplay', value: "${rlhd}${h}", displayed: false)
                        send(name: 'precipLastHour', value: rlh, unit: "${h}", descriptionText: "Precipitation in the Last Hour was ${rlh}${h}")
                    }
                } else {
                    if (state.isST) send(name: 'precipLastHourDisplay', value: '0.00', displayed: false)
                    send(name: 'precipLastHour', value: null, unit: "${h}", displayed: false)
                }
            // Rainfall Rate
                val = settings.shortStats ? state.meteoWeather.current[15] : state.meteoWeather.current.rainRate
                if ((val != null) && (val == "")) val = null
                if (val != null) {
                    def rrt = roundIt(val, hd)
                    if (device.currentValue('precipRate') != rrt) {
                        if (debugOn) log.debug "updateWeatherTiles() - updating rain rate"
                        if (state.isST) send(name: 'precipRateDisplay', value:  "${rrt}${h}", displayed: false)
                        send(name: 'precipRate', value: rrt, unit: "${h}/hr", descriptionText: "Precipitation rate is ${rrt}${h}/hour")
                    }
                } else {
                	send(name: 'precipRate', value:  null, displayed: false)
                    send(name: 'precipRateDisplay', value:  null, displayed: false)
                }


            // Wet/dry indicator - wet if there has been measurable rainfall within the last hour...
                if ((rlh != null) && (rlh > 0.0)) {
                    sendEvent( name: 'water', value: "wet" )
                } else {
                    sendEvent( name: 'water', value: "dry" )
                }
            }
            
        // Evapotranspiration - Note: requires Wind + Solar + UV + Temp + Humidity/dewpoint
        	val = settings.shortStats ? state.meteoWeather.current[14] : state.meteoWeather.current.evo
            if ((val != null) && (val == "")) val = null
            if (val != null) {
                def et = roundIt(val, hd+1)
                if (et != device.currentValue('evapotranspiration')) {
                	if (debugOn) log.debug "updateWeatherTiles() - updating evapotranspiration"
                	send(name: "evapotranspiration", value: et, unit: "${h}", descriptionText: "Evapotranspiration rate is ${et}${h}/hour")
                	if (state.isST) send(name: "etDisplay", value: "${roundIt(et,hd)}${h}", displayed: false)
                }
            } else {
            	send(name: 'evapotranspiration', value: null, displayed: false)
                if (state.isST) send(name: "etDisplay", value: '--', displayed: false)
            }

		// Wind
        	def ws, wg, wdt, wd
        	val = settings.shortStats ? state.meteoWeather.current[21] : state.meteoWeather.current.wind
            if ((val != null) && (val == "")) val = null
            if (val != null) {
            // Wind Speed
            	if (debugOn) log.debug "updateWeatherTiles() - updating wind"
            	ws = roundIt(val,1)
				state.MTwind = ws
            // Wind Gust
                val = settings.shortStats ? state.meteoWeather.current[19] : state.meteoWeather.current.windGust
                if ((val != null) && (val == "")) val = null
                if (val != null) {
           			wg = roundIt(val,1)
					state.MTgust = wg
                }
            // Beaufort index
                val = settings.shortStats ? state.meteoWeather.current[22] : state.meteoWeather.current.windBft
                if ((val != null) && (val == "")) val = null
                if (val != null) {
					state.MTwindBft = val
                    state.MTwindBftIcon = "wb${roundIt(val,0)}.png"
                    state.MTwindBftText = getBeaufortText(val)
                } else {
                	state.MTwindBft = ""
                    state.MTwindBftIcon = ""
                    state.MTwindBftText = ""
                }
            // Wind Degrees
                val = settings.shortStats ? state.meteoWeather.current[20] : state.meteoWeather.current.windDir
                if ((val != null) && (val == "")) val = null
                wd = roundIt(val,0)				
            // Wind Dir Text
                // wdt = getDirText(val)
                val = settings.shortStats ? state.meteoWeather.current[18] : state.meteoWeather.current.windTxt
                if ((val != null) && (val == "")) val = null
                wdt = val
            // Wind Info
				String winfo
                if (state.MTwindBftText != "") {
                	winfo = "${state.MTwindBftText.capitalize()}\n${ws} ${s} from ${wdt}\ngusting to ${wg} ${s}"
                } else {
                	winfo = "${ws} ${s} winds from ${wdt}\ngusting to ${wg} ${s}"
                }
                String winfoDesc 
                if (wdt && wd) {
                	winfoDesc= "Winds are ${ws} ${s} from the ${wdt} (${wd}°), gusting to ${wg} ${s}"
                } else {
                	winfoDesc = "Winds are ${ws} ${s}, gusting to ${wg} ${s}"
                }
				send(name: "windinfo", value: winfo, displayed: true, descriptionText: winfoDesc)
				send(name: "windGust", value: "${wg}", unit: "${s}", displayed: false, descriptionText: "Winds gusting to ${wg} ${s}")
				send(name: "windDirection", value: "${wd}", unit: 'degrees', displayed: false, descriptionText: "Winds from ${wd}°")
				send(name: "windDirectionText", value: "${wdt}", displayed: false, descriptionText: "Winds are from the ${wdt}")
				send(name: "wind", value: "${ws}", unit: "${s}", displayed: false, descriptionText: "Wind speed is ${ws} ${s}")
				send(name: "windSpeed", value: "${ws}", unit: "${s}", displayed: false, descriptionText: "Wind speed is ${ws} ${s}")
				
			// Hubitat Dashboard / Tile Info
				def wind_direction = getWindDirText( wdt )
				send(name: "wind_string", value: winfo, displayed: false)
				send(name: 'wind_gust', value: wg, unit: "${s}", displayed: false)
				send(name: "wind_dir", value: wdt, displayed: false)
				send(name: "wind_direction", value: wind_direction, displayed: false)
				send(name: "wind_degree", value: "${wd}", unit: '°', displayed: false)
				send(name: "wind_speed", value: "${ws}", unit: "${s}", displayed: false)
				state.MTwindDir = wind_direction
            } else {
            	def isChange = isStateChange( device, 'wind', null)
                if (isChange) {
                // Lost our anemometer somehow, null out the display
                    send(name: "windinfo", value: null, displayed: false)
                    send(name: "windGust", value: null, displayed: false)
                    send(name: "windDirection", value: null, displayed: false)
                    send(name: "windDirectionText", value: null, displayed: false)
                    send(name: "wind", value: null, displayed: false)
					send(name: 'windSpeed', value: null, displayed: false)
					send(name: 'wind_string', value: null, displayed: false)
					send(name: 'wind_gust', value: null, displayed: false)
					send(name: 'wind_dir', value: null, displayed: false)
					send(name: 'wind_direction', value: null, displayed: false)
					send(name: 'wind_degree', value: null, displayed: false)
					send(name: 'wind_speed', value: null, displayed: false)
                }
            }
            
        // Odds 'n' Ends
			if ((state.isST && ((state.MTcity == null) || (state.MTcity == ""))) || (location.name != device.currentValue("locationName"))) {
				send(name: "locationName", value: location.name, isStateChange: true, descriptionText: "Location is ${loc}")
				if (location.name != device.currentValue("location")) {
					send(name: "location", value: location.name, isStateChange: true, displayed: false, descriptionText: "")
				}
                // Hubitat Dashboard Weather Tile info
                val = state.meteoWeather.lat
            	if ((val != null) && (val == "")) val = null
            	def lat = (val != null) ? val : location.latitude
                val = state.meteoWeather.lon
            	if ((val != null) && (val == "")) val = null
                def lon = (val != null) ? val : location.longitude
                send(name: 'latitude', value: lat, descriptionText: "Weather Station latitude is ${lat}",  displayed: false)
                send(name: 'longitude', value: lon, descriptionText: "Weather Station longitude is ${lon}",  displayed: false)
                send(name: 'timezone', value: state.meteoWeather.tz, descriptionText: "Weather Station time zone is ${state.meteoWeather.tz}", displayed: false)
                send(name: 'tz_id', value: state.meteoWeather.tz, descriptionText: "Weather Station time zone is ${state.meteoWeather.tz}", displayed: false)
                if (state.isST) {
                	// use (MeteoBridge GPS coords, if available; else location's GPS coords) - UNLESS overridded with settings.twc
    				def gpsLoc = settings.twcLoc ?: "${lat},${lon}"
                	def twc = getTwcLocation(gpsLoc)
                    if (debugOn) log.debug "twc: ${twc}"
                    if (twc && twc.location) {
                    	state.MTcity = twc.location.city
                        state.MTstate = twc.location.adminDistrictCode
                        state.MTcountry = twc.location.countryCode
                		send(name: 'city', value: state.MTcity, descriptionText: "Weather Station city is ${city}", displayed: false)
                		send(name: 'state', value: state.MTstate, descriptionText: "Weather Station state is ${state}", displayed: false)
                		send(name: 'country', value: state.MTcountry, descriptionText: "Weather Station country is ${country}", displayed: false)
                    }
                }
            }

		// Date stuff
        	if ( ((settings.shortStats ? state.meteoWeather.current[0]  : state.meteoWeather.current.date) 		!= device.currentValue('currentDate')) || 
            	 ((settings.shortStats ? state.meteoWeather.current[26] : state.meteoWeather.current.sunrise) 	!= device.currentValue('sunrise')) ||
                 ((settings.shortStats ? state.meteoWeather.current[27] : state.meteoWeather.current.sunset) 	!= device.currentValue('sunset')) || 
                 ((settings.shortStats ? state.meteoWeather.current[23] : state.meteoWeather.current.dayHours)	!= device.currentValue('dayHours')) ||
                 ((settings.shortStats ? state.meteoWeather.current[34] : state.meteoWeather.current.moonrise) 	!= device.currentValue('moonrise')) || 
                 ((settings.shortStats ? state.meteoWeather.current[35] : state.meteoWeather.current.moonset) 	!= device.currentValue('moonset')) ) {
            	// If any Date/Time has changed, time to update them all
                if (debugOn) log.debug "updateWeatherTiles() - updating dates"
                
            // Sunrise / sunset
            	val = settings.shortStats ? state.meteoWeather.current[26] : state.meteoWeather.current.sunrise
                if ((val != null) && (val == "")) val = null
                if (val != null) updateMeteoTime(val, 'sunrise') else clearMeteoTime('sunrise')
				val = settings.shortStats ? state.meteoWeather.current[27] : state.meteoWeather.current.sunset
                if ((val != null) && (val == "")) val = null
                if (val != null) updateMeteoTime(val, 'sunset') else clearMeteoTime('sunset')
            // Daylight hours/minutes
            	val = settings.shortStats ? state.meteoWeather.current[23] : state.meteoWeather.current.dayHours
                if ((val != null) && (val == "")) val = null
                if (val != null) {
                	send(name: "dayHours", value: val, descriptionText: "${val} hours of daylight today")
                } else {
                	send(name: 'dayHours', value: "--:--", displayed: false)
                }
                val = settings.shortStats ? state.meteoWeather.current[28] : state.meteoWeather.current.dayMins
                if ((val != null) && (val == "")) val = null
                if (val != null) {
                	send(name: "dayMinutes", value: val, displayed: true, descriptionText: "${val} minutes of daylight today")
                }
            // Moonrise / moonset
            	val = settings.shortStats ? state.meteoWeather.current[34] : state.meteoWeather.current.moonrise
                if ((val != null) && (val == "")) val = null
                if (val != null) updateMeteoTime(val, 'moonrise') else clearMeteoTime('moonrise')
                val = settings.shortStats ? state.meteoWeather.current[35] : state.meteoWeather.current.moonset
                if ((val != null) && (val == "")) val = null
                if (val != null) updateMeteoTime(val, 'moonset') else clearMeteoTime('moonset')
                
            // update the date & time
                send(name: 'currentDate', value: (settings.shortStats ? state.meteoWeather.current[0] : state.meteoWeather.current.date), displayed: false)
                send(name: 'currentTime', value: (settings.shortStats ? state.meteoWeather.current[1] : state.meteoWeather.current.time), displayed: false)
			}
            
        // Lux estimator - get every time, even if we aren't using Meteobridge data to calculate the lux
            def lux = estimateLux()
            if (debugOn) log.debug "updateWeatherTiles() - updating lux"
			send(name: "illuminance", value: lux, unit: 'lux', descriptionText: "Illumination is ${lux} lux (est)")
            
        // Lunar Phases
        	if (debugOn) log.debug "updateWeatherTiles() - updating the moon"
        	String xn = 'x'				// For waxing/waning below
            String phase = null
            val = settings.shortStats ? state.meteoWeather.current[31] : state.meteoWeather.current.lunarAge
            if ((val != null) && (val == "")) val = null
            if (val != null) {
            	def l = val
                val = settings.shortStats ? state.meteoWeather.current[33] : state.meteoWeather.current.lunarSgt
                if ((val != null) && (val == "")) val = null
                if (val != null) {
                    switch (val) {
                        case 0: phase = 'New'; 				xn = (l >= 27) ? 'n' : 'x'; break;
                        case 1: phase = 'Waxing Crescent'; 								break;
                        case 2: phase = 'First Quarter'; 								break;
                        case 3: phase = 'Waxing Gibbous'; 								break;
                        case 4: phase = 'Full'; 			xn = (l <= 14) ? 'x' : 'n'; break;
                        case 5: phase = 'Waning Gibbous'; 	xn = 'n'; 					break;
                        case 6: phase = 'Third Quarter'; 	xn = 'n'; 					break;
                        case 7: phase = 'Waning Crescent'; 	xn = 'n'; 					break;
                        default: phase = '--'; 											break;
                    }
                    send(name: 'moonPhase', value: phase, descriptionText: 'The Moon\'s phase is ' + phase)
                    send(name: 'lunarSegment', value: val, displayed: false)
                    send(name: 'lunarAge', value: l, unit: 'days', displayed: false, descriptionText: "The Moon is ${l} days old" )        
                    send(name: 'moonAge', value: l, unit: 'days', displayed: false, descriptionText: "" )
                }
                val = settings.shortStats ? state.meteoWeather.current[32] : state.meteoWeather.current.lunarPct
                if ((val != null) && (val == "")) val = null
                if (val != null) {
                    def lpct = roundIt(val, 0)
                    if (device.currentValue('lunarPercent') != lpct) {
                        send(name: 'lunarPercent', value: lpct, displayed: false, unit: '%')
                        String pcnt = sprintf('%03d', (roundIt((val / 5.0),0) * 5).toInteger())
                        String pname = 'Moon-wa' + xn + 'ing-' + pcnt
                        //log.debug "Lunar Percent by 5s: ${pcnt} - ${pname}"
                        send(name: 'moonPercent', value: val, displayed: false, unit: '%')
                        send(name: 'moonIllumination', value: val, displayed: false, unit: '%', descriptionText: "The Moon is ${lpct}% lit")
                        send(name: 'moonDisplay', value: pname, displayed: false)
                        if (l != null) {
                            String sign = (xn == 'x') ? '+' : '-'
                            if ((phase == 'New') || (phase == 'Full')) sign = ''
                            String dir = (sign != '') ? "Wa${xn}ing" : phase
                            String info = "${dir}\n${val}%\nDay ${l}"
                            send(name: 'moonInfo', value: info, displayed: false)
                        }
                    }
                }
            }
            makeMyTile()
        }
		
    // update the timestamps last, after all the values have been updated
        if (state.meteoWeather?.current) {
            // No date/time when getting yesterday data
            if (debugOn) log.debug "updateWeatherTiles() - updating timestamps"
            def nowText = null
            def date, time
            val = settings.shortStats ? state.meteoWeather.current[0] : state.meteoWeather.current.date
            if ((val != null) && (val == "")) val = null
            date = val
            val = settings.shortStats ? state.meteoWeather.current[1] : state.meteoWeather.current.time
            if ((val != null) && (val == "")) val = null
            time = val
            if ((date != null) && (time != null)) {
                nowText = time + '\non ' + date
            } else {
                nowText = '~' + new Date(state.meteoWeather.timeGet).format("h:mm:ss a '\non' M/d/yyyy", location.timeZone).toLowerCase()
            }
            if (nowText != null) sendEvent(name:"lastSTupdate", value: nowText, descriptionText: "Last updated at ${nowText}", displayed: false)
            sendEvent(name:"timestamp", value: state.meteoWeather.timeGet, displayed: false)

            // Check if it's time to get yesterday's data
            if (debugOn) log.debug "Current date from MeteoBridge is: ${date}"
            if ((date != null) && ((state.today == null) || (state.today != date))) {
                state.today = date
                runIn( 2, getMeteoYesterday)	// request yesterday data
            }
        }
    }
    if (debugOn) log.trace "updateWeatherTiles() finished"
}
def getDarkSkyWeather() {
	if (debugOn) log.trace "getDarkSkyWeather() entered"
    if( darkSkyKey == null )
    {
        log.error "DarkSky Secret Key not found.  Please configure in preferences."
        return
    }
    def lat = device.currentValue('latitude')  ?: location.latitude				// prefer MeteoBridge's location coordinates
    def lon = device.currentValue('longitude') ?: location.longitude			// but use the hub's if that isn't set (yet)
    
	String excludes = (fcstSource && (fcstSource == 'darksky')) ? 'sources,minutely,flags' : 'sources,minutely,daily,flags'
    String units = getTemperatureScale() == 'F' ? 'us' : (speed_units=='speed_kph' ? 'ca' : 'uk2')
    def apiRequest = [
        uri : "https://api.darksky.net",
        path : "/forecast/${darkSkyKey}/${lat},${lon}",
        query : [ exclude : excludes, units : units ],
        contentType : "application/json"
    ]
    if (state.isST) {
    	include 'asynchttp_v1'
    	asynchttp_v1.get( darkSkyCallback, apiRequest )
    } else {
    	asynchttpGet( darkSkyCallback, apiRequest )
    }
}
def darkSkyCallback(response, data) {
	if (debugOn) log.trace "darkSkyCallback() status: " + response.status
    def scale = getTemperatureScale()
    if( response.hasError() )
    {
        log.error "darkSkyCallback: ${response.getErrorMessage()}"
        return
    }
   
    if( !response?.json )
    {
        log.error "darkSkyCallback: unable to retrieve data!"
        return
    }
    
    //log.info "currently icon: ${darkSky?.currently?.icon}, summary: ${darkSky?.currently?.summary}"
    //log.info "hourly icon: ${darkSky?.hourly?.icon}, summary: ${darkSky?.hourly?.summary}"
    def darkSky = response.json
    state.darkSkyWeather = response.json?.currently
    if (debugOn) send(name: 'darkSkyWeather', value: response.json, displayed: false, isStateChange: true)

	// current weather icon/state
    // DarkSky doesn't do "night" conditions, but we can make them if we know that it is night...
    def icon = darkSky?.currently?.icon
    def isNight = (device.currentValue('isDay') == 0)
    if (debugOn) log.debug "darkSkyCallback() - isNight: ${isNight}, icon: ${icon}"
    
    // Lots of comparisons to do - only do them if something changed
    def iconChanged = ((state.isNight == null) || (state.isNight != isNight) || (icon != state.darkSkyIcon) || (darkSky?.currently?.summary == null) || (darkSky.currently.summary != device.currentValue('weather')))
    if (iconChanged) {
    	state.isNight = isNight
        state.darkSkyIcon = icon					// Save the unembellished icon that DarkSky gave us
        if (debugOn) log.debug "icon changed"
        if (isNight) {
        	// Nighttime icons
            switch(icon) {
                case 'rain':
                    if (darkSky.currently.summary == 'Drizzle') {
                        icon = 'drizzle-night'
                    } else if (darkSky.currently.summary.startsWith('Light Rain')) { 
                        icon = 'lightrain'
                        if 		(darkSky.currently.summary.contains('Breezy')) icon += '-breezy'
                        else if (darkSky.currently.summary.contains('Windy'))  icon += '-windy'
                        icon += '-night'
                    } else if (darkSky.currently.startsWith('Heavy Rain')) {
                        icon = 'heavyrain'
                        if 		(darkSky.currently.summary.contains('Breezy')) icon += '-breezy'
                        else if (darkSky.currently.summary.contains('Windy'))  icon += '-windy'
                        icon += '-night'
                    } else if (darkSky.currently.summary == 'Possible Light Rain') {
                        icon = 'chancelightrain-night'
                    } else if (darkSky.currently.summary.startsWith('Possible')) {
                        icon = 'chancerain-night'
                    } else if (darksky.currently.summary.startsWith('Rain')) {
                        if 		(darkSky.currently.summary.contains('Breezy')) icon += '-breezy'
                        else if (darkSky.currently.summary.contains('Windy'))  icon += '-windy'
                        icon += '-night'
                    }
                    if (darkSky.currently.summary.contains('Dangerously Windy')) icon += '!'
                    break;
                case 'snow':
                    if 		(darkSky.currently.summary == 'Light Snow') icon = 'lightsnow-night'
                    else if (darkSky.currently.summary == 'Flurries') icon = 'flurries-night'
                    else if (darkSky.currently.summary == 'Possible Light Snow') icon = 'chancelightsnow-night'
                    else if (darkSky.current.cummary.startsWith('Possible Light Snow')) {
                        if 	    (darkSky.currently.contains('Breezy')) icon = 'chancelightsnowbz-night'
                        else if (darkSky.currently.contains('Windy')) icon = 'chancelightsnowwy-night'
                    } else if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancesnow-night'
                    break;
                case 'sleet':
                    if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancesleet-night'
                    else if (darkSky.currently.summary.startsWith('Light')) icon = 'lightsleet-night'
                    else icon = 'sleet-night'
                    break;
                case 'partly-cloudy':
                    if (darkSky.currently.summary.contains('Mostly Cloudy')) icon = 'mostly-cloudy'
                    if (darkSky.currently.summary.startsWith('Humid')) icon = 'humid-' + icon
                    icon += '-night'                    
                    break;
				case 'partly-cloudy-day':
					icon = 'partly-cloudy-night'
                case 'partly-cloudy-night':
                    if (darkSky.currently.summary.contains('Mostly Cloudy')) icon = 'mostly-cloudy-night'
                    if (darkSky.currently.summary.startsWith('Humid')) icon = 'humid-' + icon
                    break;
                case 'thunderstorm':
                    if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancetstorms-night'
                    break;
                case 'cloudy':
                if (darkSky.currently.summary.startsWith('Humid')) icon = 'humid-' + icon + '-night'
                    break;
				case 'cloudy-day':
					icon = 'cloudy-night'
                case 'cloudy-night':
                    if (darkSky.currently.summary.startsWith('Humid')) icon = 'humid-' + icon
                    break;
                case 'clear':
                case 'sunny':
				case 'clear-day':
                    icon = 'clear-night'
				case 'clear-night':
                    if (darkSky.currently.summary.contains('Humid')) icon = 'humid-night'
                    break;
                case 'wind':
                    if (darkSky.currently.summary.contains('Windy')) {
                        icon = 'wind-night'
                        if 		(darkSky.currently.summary.contains('Overcast')) 	  icon = 'wind-overcast-night'
                        else if (darkSky.currently.summary.contains('Mostly Cloudy')) icon = 'wind-mostlycloudy-night'
                        else if (darkSky.currently.summary.contains('Partly Cloudy')) icon = 'wind-partlycloudy-night'
                        else if (darksky.currently.summary.contains('Foggy'))		  icon = 'wind-foggy-night'
                        if 		(darkSky.currently.summary.startsWith('Danger')) 	  icon += '!'
                    } else if (darkSky.currently.summary.contains('Breezy')) {
                        icon = 'breezy-night'
                        if 		(darkSky.currently.summary.contains('Overcast')) 	  icon = 'breezy-overcast-night'
                        else if (darkSky.currently.summary.contains('Mostly Cloudy')) icon = 'breezy-mostlycloudy-night'
                        else if (darkSky.currently.summary.contains('Partly Cloudy')) icon = 'breezy-partlycloudy-night'
                        else if (darkSky.currently.summary.contains('Foggy'))		  icon = 'breezy-foggy-night'
                        // if 		(darkSky.currently.summary.startsWith('Danger')) 	  icon += '!'
                    }
                    break;
                case 'fog':
                case 'hail':
                case 'breezy':
                case 'tornado':
                    icon = icon + '-night'		// adjust icons for night time that DarkSky doesn't
                    break;
                case '':
                	icon = 'unknown'
                    break;
                default:
                    log.warn "Unknown DarkSky icon (${icon}), weather is: ${darkSky.currently.summary}"
                    icon = 'na'
            }
        } else { 
        	// Daytime Icons
            switch(icon) {
                case 'rain':
                    // rain=[Possible Light Rain, Light Rain, Rain, Heavy Rain, Drizzle, Light Rain and Breezy, Light Rain and Windy, 
                    //       Rain and Breezy, Rain and Windy, Heavy Rain and Breezy, Rain and Dangerously Windy, Light Rain and Dangerously Windy],
                    if (darkSky.currently.summary == 'Drizzle') {
                        icon = 'drizzle'
                    } else if 	(darkSky.currently.summary.startsWith('Light Rain')) { 
                        icon = 'lightrain'
                        if 		(darkSky.currently.summary.contains('Breezy')) icon += '-breezy'
                        else if (darkSky.currently.summary.contains('Windy'))  icon += '-windy'
                    } else if 	(darkSky.currently.summary.startsWith('Heavy Rain')) {
                        icon = 'heavyrain'
                        if 		(darkSky.currently.summary.contains('Breezy')) icon += '-breezy'
                        else if (darkSky.currently.summary.contains('Windy'))  icon += '-windy'
                    } else if 	(darkSky.currently.summary == 'Possible Light Rain') {
                        icon = 'chancelightrain'
                    } else if 	(darkSky.currently.summary.startsWith('Possible')) {
                        icon = 'chancerain'
                    } else if 	(darksky.currently.summary.startsWith('Rain')) {
                        if 		(darkSky.currently.summary.contains('Breezy')) icon += '-breezy'
                        else if (darkSky.currently.summary.contains('Windy'))  icon += '-windy'
                    }
                    if (darkSky.currently.summary.contains('Dangerously Windy')) icon += '!'
                    break;
                case 'snow':
                    if 		(darkSky.currently.summary == 'Light Snow')  icon = 'lightsnow'
                    else if (darkSky.currently.summary == 'Flurries') icon = 'flurries'
                    else if (darkSky.currently.summary == 'Possible Light Snow') icon = 'chancelightsnow'
                    else if (darkSky.currently.summary.startsWith('Possible Light Snow')) {
                        if      (darkSky.currently.summary.contains('Breezy')) icon = 'chancelightsnowbreezy'
                        else if (darkSky.currently.summary.contains('Windy')) icon = 'changelightsnowwindy'
                    } else if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancesnow'
                    break;
                case 'sleet':
                    if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancesleet'
                    else if (darkSky.currently.summary.startsWith('Light')) icon = 'lightsleet'
                    break;
                case 'thunderstorm':
                    if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancetstorms'
                    break;
				case 'partly-cloudy-night':
					icon = 'partly-cloudy-day'
                case 'partly-cloudy':
                case 'partly-cloudy-day':
                    if (darkSky.currently.summary.contains('Mostly Cloudy')) icon = 'mostly-cloudy'
                    if (darkSky.currently.summary.startsWith('Humid')) icon = 'humid-' + icon
                    break;
				case 'cloudy-night':
					icong = 'cloudy-day'
                case 'cloudy':
                case 'cloudy-day':
                    if (darkSky.currently.summary.startsWith('Humid')) icon = 'humid-' + icon
                    break;
				case 'clear-night':
					icon = 'clear-day'
                case 'clear':
                case 'clear-day':
                    if (darkSky.currently.summary == 'Humid') icon = 'humid'
                    break;
                case 'wind':
                // wind=[Windy and Overcast, Windy and Mostly Cloudy, Windy and Partly Cloudy, Breezy and Mostly Cloudy, Breezy and Partly Cloudy, 
                // Breezy and Overcast, Breezy, Windy, Dangerously Windy and Overcast, Windy and Foggy, Dangerously Windy and Partly Cloudy, Breezy and Foggy]}
                    if (darkSky.currently.summary.contains('Windy')) {
                        // icon = 'wind'
                        if 		(darkSky.currently.summary.contains('Overcast')) 	  icon = 'wind-overcast'
                        else if (darkSky.currently.summary.contains('Mostly Cloudy')) icon = 'wind-mostlycloudy'
                        else if (darkSky.currently.summary.contains('Partly Cloudy')) icon = 'wind-partlycloudy'
                        else if (darksky.currently.summary.contains('Foggy'))		  icon = 'wind-foggy'
                        if 		(darkSky.currently.summary.startsWith('Danger')) 	  icon += '!'
                    } else if (darkSky.currently.summary.contains('Breezy')) {
                        icon = 'breezy'
                        if 		(darkSky.currently.summary.contains('Overcast')) 	  icon = 'breezy-overcast'
                        else if (darkSky.currently.summary.contains('Mostly Cloudy')) icon = 'breezy-mostlycloudy'
                        else if (darkSky.currently.summary.contains('Partly Cloudy')) icon = 'breezy-partlycloudy'
                        else if (darkSky.currently.summary.contains('Foggy')) 		  icon = 'breezy-foggy'
                        //if 		(darkSky.currently.summary.startsWith('Danger')) 	  icon += '!'
                    }
                    break;
                case '':
                	icon = 'unknown'
                    break;
                default:
                    log.warn "Unrecognized DarkSky icon (${icon}), weather is: ${darkSky.currently.summary}"
                    icon = 'na'
            }

        }
        if (debugOn) log.debug "icon: ${icon}"
		if (icon) {
			send(name: "weatherIcon", value: icon, descriptionText: 'Conditions: ' + darkSky.currently.summary, isStateChange: true)
			send(name: 'weatherIcons', value: getOwmIcon(icon), displayed: false)
		}
        send(name: "weather", value: darkSky.currently.summary, displayed: false)
    }
    
    // Forecasts
    if (fcstSource && (fcstSource == 'darksky')) {
    	String h = (height_units && (height_units == 'height_in')) ? '"' : 'mm'
        int hd = (h == '"') ? 2 : 1		// digits to store & display
        
    	// Today's Forecast
        def forecast = darkSky.hourly?.summary
        
        if (summaryText) {
        	// Collect all the Summary variations per icon
        	def summaryList = state.summaryList ?: []
            def summaryMap = state.summaryMap ?: [:]
            int i = 0
            def listChanged = false
            def mapChanged = false
            while (darkSky.hourly?.data[i]?.summary != null) {
            	if (!summaryList.contains(darkSky.hourly.data[i].summary)) {
                	summaryList << darkSky.hourly.data[i].summary
                    listChanged = true
                }
                if (!summaryMap.containsKey(darkSky.hourly.data[i].icon)) {
                	if (debugOn) log.debug "Adding key ${darkSky.hourly.data[i].icon}"
                	summaryMap."${darkSky.hourly.data[i].icon}" = []
                }
                if (!summaryMap."${darkSky.hourly.data[i].icon}".contains(darkSky.hourly.data[i].summary)) {
                	if (debugOn) log.debug "Adding value '${darkSky.hourly.data[i].summary}' to key ${darkSky.hourly.data[i].icon}"
                	summaryMap."${darkSky.hourly.data[i].icon}" << darkSky.hourly.data[i].summary
                    mapChanged = true
                }
                i++
            }
            if (listChanged) {
            	state.summaryList = summaryList
                send(name: 'summaryList', value: summaryList, isStateChange: true, displayed: false)
            }            
            if (mapChanged) {
            	if (debugOn) log.debug summaryMap
            	state.summaryMap = summaryMap
            	send(name: 'summaryMap', value: summaryMap, isStateChange: true, displayed: false)
            }
        }
        
        send(name: 'forecast', value: forecast, descriptionText: "DarkSky Forecast: " + forecast)
    	send(name: "forecastCode", value: darkSky.hourly.icon, displayed: false)
        
        def pop = darkSky.hourly?.data[0]?.precipProbability
        if (pop != null) {
        	pop = roundIt((pop * 100), 0)	
        	if (state.isST) send(name: "popDisplay", value: "PoP\nnext hr\n~${pop}%", descriptionText: "Probability of precipitation in the next hour is ${pop}%")
        	send(name: "pop", value: pop, unit: '%', displayed: false)
        } else {
        	if (state.isST) send(name: "popDisplay", value: null, displayed: false)
            send(name: "pop", value: null, displayed: false)
        }
        
        def rtd = darkSky.daily?.data[0]?.precipIntensity
        if (rtd != null) {
        	rtd = roundIt((rtd * 24.0), hd+1)
        	def rtdd = roundIt(rtd, hd)
        	send(name: "precipForecast", value: rtd, unit: h, descriptionText: "Forecasted precipitation today is ${rtd}${h}")
            if (state.isST) send(name: "precipFcstDisplay", value: "${rtdd}${h}", displayed: false)
        } else {
        	send(name: "precipForecast", value: null, displayed: false)
            if (state.isST) send(name: "precipFcstDisplay", value: null, displayed: false)
        }
        
        def hiTTda = roundIt(darkSky.daily?.data[0]?.temperatureHigh, 0)
        def loTTda = roundIt(darkSky.daily?.data[0]?.temperatureLow, 0)
        send(name: "highTempForecast", value: hiTTda, unit: scale, descriptionText: "Forecast high temperature today is ${hiTTda}°${scale}")
        send(name: "lowTempForecast", value: loTTda, unit: scale, descriptionText: "Forecast high temperature today is ${loTTda}°${scale}")

        if (darkSky.daily?.data[0]?.humidity != null) {
        	def avHTda = roundIt((darkSky.daily.data[0].humidity * 100), 0)
        	send(name: "avgHumForecast", value: avHTda, unit: '%', descriptionText: "Forecast average humidity today is ${avHTda}%")
        } else {
        	send(name: "avgHumForecast", value: null, unit: '%', displayed: false)
        }
        
        if (darkSky.daily?.data[0]?.precipProbability != null) {
        	def popTda = roundIt((darkSky.daily.data[0].precipProbability * 100), 0)
        	if (state.isST) send(name: "popFcstDisplay", value: "PoP\nTDY\n~${popTda}%", descriptionText: "Probability of precipitation today is ${popTda}%")
        	send(name: "popForecast", value: popTda, unit: '%', displayed: false)
        } else {
        	if (state.isST) send(name: "popFcstDisplay", value: null, displayed: false)
            send(name: "popForecast", value: null, displayed: false)
        }
        
        // Tomorrow's Forecast
        def hiTTom = roundIt(darkSky.daily?.data[1]?.temperatureHigh, 0)
        def loTTom = roundIt(darkSky.daily?.data[1]?.temperatureLow, 0)
        send(name: "highTempTomorrow", value: hiTTom, unit: scale, descriptionText: "Forecast high temperature tomorrow is ${hiTTom}°${scale}")
        send(name: "lowTempTomorrow", value: loTTom, unit: scale, descriptionText: "Forecast high temperature tomorrow is ${loTTom}°${scale}")

		if (darkSky.daily?.data[1]?.humidity != null) {
        	def avHTom = roundIt((darkSky.daily.data[1].humidity * 100), 0)
        	send(name: "avgHumTomorrow", value: avHTom, unit: '%', descriptionText: "Forecast average humidity today is ${hiHTom}%")
        } else {
        	send(name: "avgHumTomorrow", value: null, unit: '%', displayed: false)
        }
      
      	if (darkSky.daily?.data[1]?.precipIntensity != null) {
		    def rtom = roundIt((darkSky.daily.data[1].precipIntensity * 24.0), hd+1)
        	def rtomd = roundIt(rtom, hd)
            if (state.isST) send(name: 'precipTomDisplay', value: "${rtomd}${h}", displayed: false)
            send(name: 'precipTomorrow', value: rtom, unit: h, descriptionText: "Forecast precipitation tomorrow is ${rtom}${h}")
        } else {
            if (state.isST) send(name: 'precipTomDisplay', value:  null, displayed: false)
            send(name: 'precipTomorrow', value: null, displayed: false)
        }
        
        if (darkSky.daily?.data[1]?.precipProbability != null) {
        	def popTom = roundIt((darkSky.daily.data[1].precipProbability * 100), 0)
            if (state.isST) send(name: "popTomDisplay", value: "PoP\nTMW\n~${popTom}%", descriptionText: "Probability of precipitation tomorrow is ${popTom}%")
            send(name: "popTomorrow", value: popTom, unit: '%', displayed: false)
        } else {
            if (state.isST) send(name: "popTomDisplay", value: null, displayed: false)
            send(name: "popTomorrow", value: null, displayed: false)
        }
    }
    if (debugOn) log.info "darkSkyCallback() finished"
}
// This updates the tiles with Weather Underground data (deprecated)
def updateWundergroundTiles() {
	if (hubPlatfrom == 'Hubitat') {
    	log.warn "Weather Underground data is not available on Hubitat - please configure a different weather source"
    } else {
		log.warn "Weather Underground data is deprecated on SmartThings - please configure a different weather source"
    }
}
// This updates the tiles with THe Weather Company data
def updateTwcTiles() {
	if (debugOn) log.trace "updateTwcTiles() entered"
    if (state.isHE) {
    	log.warn "TWC weather data is not available on Hubitat - please configure DarkSky or MeteoBridge for forecast data"
        return
    }
    
    def features = ''
    def twcConditions = [:]
    def twcForecast = [:]
    def lat = device.currentValue('latitude')  ?: location.latitude
    def lon = device.currentValue('longitude') ?: location.longitude
    def gpsLoc = settings.twcLoc ?: "${lat},${lon}"
    
    if (debugOn) twcConditions = getTwcConditions(gpsLoc)
    
    if (darkSkyKey == '') {
    	if (!debug) twcConditions = getTwcConditions(gpsLoc)
        if (state.twcForTomorrow || (fcstSource && (fcstSource == 'twc'))) {
        	twcForecast = getTwcForecast(gpsLoc)
        }
    } else if (state.twcForTomorrow || (fcstSource && (fcstSource == 'twc'))) {
    	twcForecast = getTwcForecast(gpsLoc)
    }
    state.twcConditions = twcConditions
    state.twcForecast = twcForecast
    
    if ((twcConditions == [:]) && (twcForecast == [:])) return
    if (debugOn) log.trace "updateTwcTiles()"
    
    if (fcstSource && (fcstSource == 'twc')) state.twcForTomorrow = false
    // if (debug) log.debug 'Features: ' + features

    // def obs = get(features)		//	?.current_observation
    if (debugOn) send(name: 'twcConditions', value: JsonOutput.toJson(twcConditions), displayed: false)
    if (debugOn) send(name: 'twcForecast', value: JsonOutput.toJson(twcForecast), displayed: false)
    
    if (twcConditions != [:]) {
        def weatherIcon = translateTwcIcon( twcConditions.iconCode.toInteger() )
        send(name: "weather", value: twcConditions.wxPhraseMedium, descriptionText: 'Conditions: ' + twcConditions.wxPhraseLong)
        send(name: "weatherIcon", value: weatherIcon, displayed: false)
		send(name: 'weatherIcons', value: getOwmIcon(weatherIcon), displayed: false)
	}

	if (twcForecast != [:] ) {
    	if (debugOn) log.trace "Parsing twcForecast"
        def scale = getTemperatureScale()
        String h = (height_units && (height_units == 'height_in')) ? '"' : 'mm'
        int hd = (h == '"') ? 2 : 1		// digits to store & display

        if (!state.twcForTomorrow) {
            // Here we are NOT using Meteobridge's Davis weather forecast text/codes
            if (twcForecast.daypart != null) {
            	// def when = twcForecast.daypart.daypartName + ': '
            	// def forecast = (twcForecast.narrative[0] != null) ? twcForecast.narrative[0] : 'N/A'
                def forecast = (twcForecast.daypart.narrative[0] as List)[0]
            	send(name: 'forecast', value: forecast, descriptionText: 'TWC forecast: ' + forecast)
                if (debugOn) log.debug 'TWC forecast: ' + forecast
                def twcIcon = (twcForecast.daypart.iconCode[0] as List)[0]
            	send(name: "forecastCode", value: 'TWC forecast icon: ' + twcIcon, displayed: false)
				if (debugOn) log.debug 'TWC forecast icon' + twcIcon

        		def when = ((twcForecast.daypart.dayOrNight[0] as List)[0].toString() == 'N') ? 'TNT' : 'TDY'
                if (debugOn) log.debug "When ${when}"
        		def pop = (twcForecast.daypart.precipChance[0] as List)[0] // .toNumber()							// next half-day (night or day)
                if (debugOn) log.debug "pop: ${pop}"
        		if (pop != null) {
            		if (state.isST) send(name: "popDisplay", value: "PoP\n${when}\n~${pop}%", descriptionText: "Probability of precipitation ${when} is ${pop}%")
            		send(name: "pop", value: pop, unit: '%', displayed: false)
        		} else {
            		if (state.isST) send(name: "popDisplay", value: null, displayed: false)
            		send(name: "pop", value: null, displayed: false)
        		}

        		def hiTTdy = twcForecast.temperatureMax[0]
        		def loTTdy = twcForecast.temperatureMin[0]
                def hTdy = twcForecast.daypart.relativeHumidity[0] as List
        		def avHTdy = roundIt((hTdy[0] + hTdy[1]) / 2.0, 0)
                def pTdy = twcForecast.daypart.precipChance[0]
        		def popTdy = roundIt((pTdy[0].toBigDecimal() / 2.0) + (pTdy[1]?.toBigDecimal() / 2.0), 0)		// next 24 hours
        		send(name: "highTempForecast", value: hiTTdy, unit: scale, descriptionText: "Forecast high temperature today is ${hiTTdy}°${scale}")
        		send(name: "lowTempForecast", value: loTTdy, unit: scale, descriptionText: "Forecast high temperature today is ${loTTdy}°${scale}")
        		send(name: "avgHumForecast", value: avHTdy, unit: '%', descriptionText: "Forecast average humidity today is ${avHTdy}%")

        		def rtd = roundIt( twcForecast.qpf[0], hd+1)
        		def rtdd = roundIt(rtd, hd)
        		if (rtdd != null) {
            		if (state.isST) send(name: 'precipFcstDisplay', value:  "${rtdd}${h}", displayed: false)
            		send(name: 'precipForecast', value: rtd, unit: h, descriptionText: "Forecast precipitation today is ${rtd}${h}")
        		} else {
            		if (state.isST) send(name: 'precipFcstDisplay', value:  null, displayed: false)
            		send(name: 'precipForecast', value: null, displayed: false)
        		}
                
        		if (popTdy != null) {
            		if (state.isST) send(name: "popFcstDisplay", value: "PoP\nTDY\n~${popTdy}%", descriptionText: "Probability of precipitation today is ${popTdy}%")
            		send(name: "popForecast", value: popTdy, unit: '%', displayed: false)
                } else {
            		if (state.isST) send(name: "popFcstDisplay", value: null, displayed: false)
            		send(name: "popForecast", value: null, displayed: false)
        		}

        		def hiTTom = twcForecast.temperatureMax[1]
        		def loTTom = twcForecast.temperatureMin[1]
                def si = 2
                if ((twcForecast.daypart.dayOrNight[0] as List)[0] == 'N') si = 1
                def hTom = twcForecast.daypart.relativeHumidity[0] as List
                def avHTom = roundIt((hTom[si].toBigDecimal() + hTom[si+1].toBigDecimal()) / 2.0, 0)
                def pTom = twcForecast.daypart.precipChance[0] as List
                def popTom = roundIt((pTom[si]?.toBigDecimal() / 2.0) + (pTom[si+1].toBigDecimal() / 2.0), 0)
        		send(name: "highTempTomorrow", value: hiTTom, unit: scale, descriptionText: "Forecast high temperature tomorrow is ${hiTTom}°${scale}")
        		send(name: "lowTempTomorrow", value: loTTom, unit: scale, descriptionText: "Forecast high temperature tomorrow is ${loTTom}°${scale}")
        		send(name: "avgHumTomorrow", value: avHTom, unit: '%', descriptionText: "Forecast average humidity tomorrow is ${hiHTom}%")

        		def rtom = roundIt(twcForecast.qpf[1], hd+1)
        		def rtomd = roundIt(rtom, hd)
        		if (rtom != null) {
            		if (state.isST) send(name: 'precipTomDisplay', value:  "${rtomd}${h}", displayed: false)
            		send(name: 'precipTomorrow', value: rtom, unit: "${h}", descriptionText: "Forecast precipitation tomorrow is ${rtd}${h}")
        		} else {
            		if (state.isST) send(name: 'precipTomDisplay', value:  null, displayed: false)
            		send(name: 'precipTomorrow', value: null, displayed: false)
        		}
        		if (popTom != null) {
            		if (state.isST) send(name: "popTomDisplay", value: "PoP\nTMW\n~${popTom}%", descriptionText: "Probability of precipitation tomorrow is ${popTom}%")
            		send(name: "popTomorrow", value: popTom, unit: '%', displayed: false)
        		} else {
            		if (state.isST) send(name: "popTomDisplay", value: null, displayed: false)
            		send(name: "popTomorrow", value: null, displayed: false)
        		}		
    		}
    	}
        if (debugOn) log.info "Finished parsing twcForecast"
    }
}
// ***********************************************************************************************************************
// Support Routines
// ****************
def getWindDirText( windText ) {
	def wind_direction = 'Unknown'
	switch(windText?.trim().toUpperCase()) {
        case 'N': 	wind_direction = 'North'; 			break;
        case 'NNE': wind_direction = 'North-Northeast'; break;
        case 'NE': 	wind_direction = 'Northeast'; 		break;
        case 'ENE': wind_direction = 'East-Northeast'; 	break;
        case 'E': 	wind_direction = 'East'; 			break;
        case 'ESE': wind_direction = 'East-Southeast'; 	break;
        case 'SE': 	wind_direction = 'Southeast'; 		break;
        case 'SSE': wind_direction = 'South-Southeast'; break;
        case 'S': 	wind_direction = 'South'; 			break;
        case 'SSW': wind_direction = 'South-Southwest'; break;
        case 'SW': 	wind_direction = 'Southwest'; 		break;
        case 'WSW': wind_direction = 'West-Southwest'; 	break;
        case 'W': 	wind_direction = 'West'; 			break;
        case 'WNW': wind_direction = 'West-Northwest'; 	break;
        case 'NW': 	wind_direction = 'Northwest'; 		break;
        case 'NNW': wind_direction = 'North-Northwest'; break;
        case 'N/A':
        case '--':	wind_direction = 'N/A';				break;
        default: 	wind_direction = 'Unknown'; 		break;
    }
    return wind_direction
}
private updateMeteoTime(timeStr, stateName) {
	def t = timeToday(timeStr, location.timeZone).getTime()
	def tAPM = new Date(t).format('h:mma', location.timeZone).toLowerCase()
    def stateCap = stateName.capitalize()
    def txt = (stateName == 'sunrise') ? 'Sunrise (astronimical)' : ( (stateName == 'sunset') ? 'Sunset (astronomical)' : stateCap )
   	send(name: stateName, value: timeStr, displayed: true, descriptionText: txt)
    send(name: stateName + 'APM', value: tAPM, descriptionText: stateCap + ' at ' + tAPM)
    send(name: stateName + 'Epoch', value: t, displayed: false)
	if (stateName.startsWith('sun') || stateName.startsWith('moon')) send(name: 'local' + stateCap, value: timeStr, displayed: false, descriptionText: '')
}
private clearMeteoTime(stateName) {
	send(name: stateName, value: null, displayed: true)
    send(name: stateName + 'APM', value: null, descriptionText: 'No ' + stateName + ' today')
    send(name: stateName + 'Epoch', value: null, displayed: false)
	if (stateName.startsWith('sun') || stateName.startsWith('moon')) send(name: 'local' + stateName.capitalize(), value: null, displayed: false)
}
def getMeteoYesterday() {
	getMeteoWeather( true )
}
private makeMyTile() {
    // myTile (for Hubitat Dashboard)
    if (state.isST || settings.skipMyTile) {
        if (debugOn) log.debug "Skipping 'myTile' update"
        if (!state.myTileWasCleared) { send(name:'myTile', value: 'null', displayed: false, descriptionText: ''); state.myTileWasCleared = true }
        return
    }
    if (debugOn) log.debug "updateWeatherTiles() - updating myTile - icon: ${state.MTicon}"
    String unit = getTemperatureScale()
    String h = (height_units && (height_units == 'height_in')) ? '"' : 'mm'
	String s = (speed_units && (speed_units == 'speed_mph')) ? 'mph' : 'kph'
	String pv = (pres_units && (pres_units == 'press_in')) ? 'inHg' : 'mmHg'
    def iconClose = "?raw=true"
    if (debugOn) log.debug "state.MTwindBftIcon: ${state.MTwindBftIcon}, state.MTwindBft: (${state.MTwindBft})"
    def mytext = '<div style=text-align:center;display:inline;margin:0px;>' + "${state.MTlocation}" +
                    // "${alertStyleOpen}" + "${condition_text}" + "${alertStyleClose}" + '<br>' +
                    ( ((state.MTcity!=null)&&(state.MTcity!="")) ? ( ' - ' + state.MTcity + ', ' + state.MTstate + '<br>') : '<br>' ) +
                    "</div><br>${state.MTtemperature}&deg;${unit}" + '<img style=height:2.0em src=' + "${getImgIcon(state.MTicon)}" + '>' + 
                        ( (state.MTweather&&(state.MTweather!="")) ? ('&nbsp; ' + state.MTweather + '<br>') : '<br>' ) +
                    '<span style=font-size:.75em;>' +
                        ( (((state.MTfeelsLike!=null)&&(state.MTfeelsLike!=""))&&(state.MTfeelsLike!=state.MTtemperature)) ? ('Feels like ' + "${state.MTfeelsLike}&deg;${unit}") : '&nbsp;' ) + '<br><br></span>' +
                    '<div style=font-size:.90em;line-height=100%;>' +
                        ( ((state.MTwindBft!=null)&&(state.MTwindBft!="")) ? ('<img src=' + state.iconStore + state.MTwindBftIcon + iconClose + "> ${state.MTwindBftText.capitalize()}${(state.MTwindBft != 0) ? (' from the ' + state.MTwindDir) : ''}<br>") : '') +
                    ( ((state.MTwindBft!=null)&&(state.MTwindBft!=0)) ? "at ${state.MTwind} ${s}, gusting to ${state.MTgust} ${s}<br><br>" : '<br>') +
                    ( ((state.MTpressure!=null)&&(state.MTpressure!="")) ? ('<img src=' + state.iconStore + "wb.png${iconClose}>" + "${state.MTpressure} ${pv} ${state.MTpressTrend}  ") : '') + 
                        '<img src=' + state.iconStore + "wh.png${iconClose}>" + "${state.MThumidity}" + '% &nbsp; ' + '<img src=' + 
                        state.iconStore + "wu.png${iconClose}>" + "${state.MTpop}" + '%' + (state.MTprecipToday > 0 ? '&nbsp; <img src=' + state.iconStore + "wr.png${iconClose}>" + "${state.MTprecipToday} ${h}" : '') + '<br>' +
                    ( ((state.MTsolarPwr!=null)&&(state.MTsolarPwr!="")) ? '<img src=' + state.iconStore + "wsp.png${iconClose}>" + " ${state.MTsolarPwr} W/m²  " : '' ) +
                        ( ((state.MTuvi!=null)&&(state.MTuvi!="")) ? '<img src=' + state.iconStore + "wuv.png${iconClose}>" + " ${state.MTuvi} uvi</div>" : '</div>') 
                   //( (state.MTsunrise != "") ? ('<img src=' + state.iconStore + "wsr.png${iconClose}>" + "${state.MTsunrise}" + ' &nbsp; <img src=' + state.iconStore + "wss.png${iconClose}>" + "${state.MTsunset}</div>") : '</div>' )
    int mysize = mytext.size()
    if (state.isHE && (mysize > 1024)) log.warn "myTile size is greater than 1024 characters! (${mysize})"
    if (debugOn) log.debug "mytext (${mysize}): ${mytext}"
    send(name: 'myTile', value: mytext, displayed: false, descriptionText: "")
}
private String translateTwcIcon( Integer iconNumber ) {
    def isNight = (device.currentValue('isDay') == 0)

	switch( iconNumber ) {
        case 0:							// Tornado
            return (isNight ? 'nt_tornado' : 'tornado')
            break;
        case 1:							// Tropical Storm (hurricane icon) ***NEW***
            return 'tropical-storm'
            break;
        case 2:							// Hurricane	***New***
            return 'hurricane'
            break;
        case 3:							// Strong Storms
            return (isNight ? 'nt_tstorms' : 'tstorms')
            break;
        case 4: 						// Thunder and Hail ***new text***
            return (isNight ? 'nt_thunder-hail' : 'thunder-hail')
            break;
        case 5:							// Rain to Snow Showers
            return (isNight ? 'nt_rain-snow' : 'rain-snow')
            break;
        case 6:							// Rain / Sleet
            return (isNight ? 'nt_rain-sleet' : 'rain-sleet')
            break;
        case 7: 						// Wintry Mix Snow / Sleet
            return (isNight ? 'nt_winter-mix' : 'winter-mix')
            break;
        case 8:							// Freezing Drizzle
            return (isNight ? 'nt_freezing-drizzle' : 'freezing-drizzle')
            break;
        case 9:							// Drizzle
            return (isNight ? 'nt_drizzle' : 'drizzle')
            break;
        case 10:						// Freezing Rain
            return (isNight ? 'nt_freezing-rain' : 'freezing-rain')
            break;
        case 11:						// Light Rain
            return (isNight ? 'nt_lightrain' : 'lightrain')
            break;
        case 12:						// Rain
            return (isNight ? 'nt_rain' : 'rain')
            break;
        case 13:						// Scattered Flurries
            return (isNight ? 'nt_scattered-flurries' : 'scattered-flurries')
            break;
        case 14:						// Light Snow
            return ( isNight ? 'lightsnow-night' : 'lightsnow' )
            break;
        case 15:						// Blowing / Drifting Snow ***NEW***
            return ( isNight ? 'nt_blowing-snow' : 'blowing-snow' )
            break;
        case 16:						// Snow
            return ( isNight ? 'nt_snow' : 'snow' )
            break;
        case 17:						// Hail
            return ( isNight ? 'nt_hail' : 'hail' )
            break;
        case 18:						// Sleet
            return ( isNight ? 'nt_sleet' : 'sleet' )
            break;
        case 19: 						// Blowing Dust / Sandstorm
            return 'sandstorm'
            break;
        case 20:						// Foggy
            return 'fog'
            break;
        case 21:						// Haze / Windy
            return 'hazy'
            break;
        case 22:						// Smoke / Windy
            return 'smoke'
            break;
        case 23:						// Breezy
            return ( isNight ? 'breezy-night' : 'breezy' )
            break;
        case 24:						// Blowing Spray / Windy
            icon = 'blowing-spray'
            //nt_blowing-spray
            break;
        case 25:						// Frigid / Ice Crystals
            icon = 'frigid-ice'
            //nt_frigid-ice
            break;
        case 26:						// Cloudy
            return (isNight ? 'nt_cloudy' : 'cloudy' )
            break;
        case 27: 						// Mostly Cloudy (Night)
            return 'nt_mostlycloudy'
            break;
        case 28:						// Mostly Cloudy (Day)
            return 'mostlycloudy'
            break;
        case 29:						// Partly Cloudy (Night)
            return 'nt_partlycloudy'
            break;
        case 30:						// Partly Cloudy (Day)
            return 'partlycloudy'
            break;
        case 31: 						// Clear (Night)
            return 'nt_clear'
            break;
        case 32:						// Sunny (Day)
            return 'sunny'
            break;
        case 33:						// Fair / Mostly Clear (Night)
            return 'nt_mostlysunny'
            break;
        case 34:						// Fair / Mostly Sunny (Day)
            return 'mostlysunny'
            break;
        case 35:						// Mixed Rain & Hail
            icon = 'rain-hail'
            //nt_rain-hail
            break;
        case 36:						// Hot
            return 'sunny'
            break;
        case 37: 						// Isolated Thunderstorms
            return 'tstorms-iso'
            break;
        case 38:						// Thunderstorms
            return ( isNight ? 'nt_tstorms' : 'tstorms' )
            break;
        case 39: 						// Scattered Showers (Day)
            return 'scattered-showers'
            break;
        case 40: 						// Heavy Rain
            return ( isNight ? 'heavyrain-night' : 'heavyrain' )
            break;
        case 41:						// Scattered Snow Showers (Day)
            return 'scattered-snow'
            break;
        case 42:						// Heavy Snow
            return ( isNight ? 'nt_heavysnow' : 'heavysnow' )
            break;
        case 43:						// Blizzard
            return ( isNight ? 'nt_blizzard' : 'blizzard' )
            break;
        case 44:						// Not Available (N/A)
            return 'unknown'
            break;
        case 45:						// Scattered Showers (Night)
            return 'nt_scattered-rain'
            break;
        case 46:						// Scattered Snow Showers (Night)
            return 'nt_scattered-snow'
            break;
        case 47:						// Scattered Thunderstorms (Night)
            return 'nt_scattered-tstorms'
            break;

    }
}
private estimateLux() {
	def isNight = (device.currentValue('isDay') == 0)
	// If we have it, use solarRadiation as a proxy for Lux
    def val = settings.shortStats ? state.meteoWeather.current[30] : state.meteoWeather?.current?.solRad
    if ((val != null) && (val == "")) val = null
    if (val != null) {
    	def lux
    	switch (settings.lux_scale) {
        	case 'std':
            	// 0-10,000 - SmartThings Weather Tile scale
                lux = isNight ? 10 : roundIt(((val / 0.225) * 10.0), 0)	// Hack to approximate SmartThings Weather Station
        		return (lux < 10) ? 10 : ((lux > 10000) ? 10000 : lux)
    			break;
                
        	case 'real':
            	// 0-100,000 - realistic estimated conversion from SolarRadiation
                lux = isNight ? 10 : roundIt((val / 0.0079), 0)		// Hack approximation of Davis w/m^2 to lx
                return (lux< 10) ? 10 : ((lux > 100000) ? 100000 : lux)
                break;
                
            case 'default':
            default:
            	lux = isNight ? 10 : roundIt((val / 0.225), 0)	// Hack to approximate Aeon multi-sensor values
        		return (lux < 10) ? 10 : ((lux > 1000) ? 1000 : lux)
                break;
        }
    }
    // handle other approximations here
    def lux = 10
    def now = new Date().time
    if (!isNight) {
        //day
        if (darkSkyKey != '') {
        	// Dark Sky: Use Cloud Cover
            def cloudCover = (state.darkSkyWeather?.cloudCover != null) ? state.darkSkyWeather.cloudCover : 0.0
            lux = roundIt(1000.0 - (1000.0 * cloudCover), 0)
            if (lux == 0) {
            	if (state.darkSkyWeather?.uvIndex != null) {
                	lux = (state.darkSkyWeather.uvIndex > 0) ? 100 : 50	// hack - it's never totally dark during the day
                }
            }
        } else {
        	// Weather Underground: use conditions
            def weatherIcon = device.currentValue('weatherIcon')
            switch(weatherIcon) {
                case 'tstorms':
                    lux = 50
                    break
                case ['cloudy', 'fog', 'rain', 'sleet', 'snow', 'flurries',
                    'chanceflurries', 'chancerain', 'chancesleet',
                    'chancesnow', 'chancetstorms']:
                    lux = 100
                    break
                case ['mostlycloudy', 'partlysunny']:
                    lux = 250
                    break
                case ['mostlysunny', 'partlycloudy', 'hazy']:
                    lux = 750
                    break
                default:
                    //sunny, clear
                    lux = 1000
            }
        }

        //adjust for dusk/dawn
        def afterSunrise = now - device.currentValue('sunriseEpoch')
        def beforeSunset = device.currentValue('sunsetEpoch') - now
        def oneHour = 3600000

        if(afterSunrise < oneHour) {
            //dawn
            lux = roundIt((lux * (afterSunrise/oneHour)), 0)
        } else if (beforeSunset < oneHour) {
            //dusk
            lux = roundIt((lux * (beforeSunset/oneHour)), 0)
        }
        if (lux < 10) lux = 10
        
        // Now, adjust the scale based on the settings
        if (settings.lux_scale) {
        	if (settings.lux_scale == 'std') {
            	lux = lux * 10 		// 0-10,000
            } else if (settings.lux_scale == 'real') {
            	lux = lux * 100		// 0-100,000
            }
       	}   	     
    } else {
        //night - always set to 10 for now
        //could do calculations for dusk/dawn too
        lux = 10
    }
    lux
}
def getPurpleAirAQI() {
	if (debugOn) log.trace "getPurpleAirAQI() entered"
    if (!settings.purpleID) {
    	send(name: 'airQualityIndex', value: null, displayed: false)
        send(name: 'airQuality', value: null, displayed: false)
        send(name: 'aqi', value: null, displayed: false)
        return
    }
    def params = [
        uri: 'https://www.purpleair.com',
        path: '/json',
        query: [show: settings.purpleID]
        // body: ''
    ]
    if (state.isST) {
    	include 'asynchttp_v1'
    	asynchttp_v1.get(purpleAirResponse, params)
    } else {
    	asynchttpGet(purpleAirResponse, params)
    }
    if (debugOn) log.trace "getPurpleAirAQI() finished"
}
def purpleAirResponse(resp, data) {
	if (debugOn) log.trace "purpleAirResponse() status: " + resp?.status 
	if (resp?.status == 200) {
		try {
			if (!resp.json) {
            	// FAIL - no data
                log.warn "purpleAirResponse() no JSON: ${resp.data}"
                return
            }
		} catch (Exception e) {
			log.error "purpleAirResponse() - General Exception: ${e}"
        	throw e
            return
        }
    } else {
    	return
    }
    
    def purpleAir = resp.json
	// good data, do the calculations
    if (debugOn) send(name: 'purpleAir', value: resp.json, displayed: false)
    def stats = [:]
    if (purpleAir.results[0]?.Stats) stats[0] = new JsonSlurper().parseText(purpleAir.results[0].Stats)
    if (purpleAir.results[1]?.Stats) stats[1] = new JsonSlurper().parseText(purpleAir.results[1].Stats)
   	
    // Figure out if we have both Channels, or only 1
    def single = null
	if (purpleAir.results[0].A_H) {
        if (purpleAir.results[1].A_H) {
        	// A bad, B bad
            single = -1
        } else {
        	// A bad, B good
        	single = 1
        }
    } else {
    	// Channel A is good
    	if (purpleAir.results[1].A_H) {
        	// A good, B bad
        	single = 0
        } else {
        	// A good, B good
            single = 2
        }
    }
    Long newest = null
    if (single == 2) {
    	newest = ((stats[0]?.lastModified?.toLong() > stats[1]?.lastModified?.toLong()) ? stats[0].lastModified.toLong() : stats[1].lastModified.toLong())
    } else if (single >= 0) {
    	newest = stats[single]?.lastModified?.toLong()
    }
	// check age of the data
    Long age = now() - (newest?:1000)
    def pm = null
    def aqi = null
    if (age <=  300000) {
    	if (single >= 0) {
    		if (single == 2) {
    			pm = (purpleAir.results[0]?.PM2_5Value?.toBigDecimal() + purpleAir.results[1]?.PM2_5Value?.toBigDecimal()) / 2.0
    		} else if (single >= 0) {
    			pm = purpleAir.results[single].PM2_5Value?.toBigDecimal()
    		}
    		aqi = roundIt((pm_to_aqi(pm)), 1)
        } else {
        	aqi = 'n/a'
        	log.warn 'parsePurpleAir() - Bad data...'
        }
    } else {
    	aqi = null
        log.warn 'parsePurpleAir() - Old data...'
    }
    if (aqi) {
    	def raqi = roundIt(aqi, 0)
    	send(name: 'airQualityIndex', value: raqi, descriptionText: "Air Quality Index is ${raqi}", displayed: false)
        send(name: 'airQuality', value: raqi, descriptionText: "Air Quality is ${raqi}", displayed: false)
        if (aqi < 1.0) aqi = roundIt(aqi, 0)
        //log.info "AQI: ${aqi}"
    	send(name: 'aqi', value: aqi, descriptionText: "AQI is ${aqi}", displayed: false)
    }
    if (debugOn) log.trace "purpleAirResponse() finished"
    // return true
}
private def pm_to_aqi(pm) {
	def aqi
	if (pm > 500) {
	  aqi = 500;
	} else if (pm > 350.5 && pm <= 500 ) {
	  aqi = remap(pm, 350.5, 500.5, 400, 500);
	} else if (pm > 250.5 && pm <= 350.5 ) {
	  aqi = remap(pm, 250.5, 350.5, 300, 400);
	} else if (pm > 150.5 && pm <= 250.5 ) {
	  aqi = remap(pm, 150.5, 250.5, 200, 300);
	} else if (pm > 55.5 && pm <= 150.5 ) {
	  aqi = remap(pm, 55.5, 150.5, 150, 200);
	} else if (pm > 35.5 && pm <= 55.5 ) {
	  aqi = remap(pm, 35.5, 55.5, 100, 150);
	} else if (pm > 12 && pm <= 35.5 ) {
	  aqi = remap(pm, 12, 35.5, 50, 100);
	} else if (pm > 0 && pm <= 12 ) {
	  aqi = remap(pm, 0, 12, 0, 50);
	}
	return aqi;
}
private def remap(value, fromLow, fromHigh, toLow, toHigh) {
    def fromRange = fromHigh - fromLow;
    def toRange = toHigh - toLow;
    def scaleFactor = toRange / fromRange;

    // Re-zero the value within the from range
    def tmpValue = value - fromLow;
    // Rescale the value to the to range
    tmpValue *= scaleFactor;
    // Re-zero back to the to range
    return tmpValue + toLow;
}
def getDirText( String degrees ){
	return (degrees == null) ? 'N/A' : getDirText( degrees.toBigDecimal() )
}
def getDirText( BigDecimal degrees ) {
	if 		(degrees == null) 								return 'N/A'
    if 		((degrees > 348.75) || (degrees <= 11.25)) 		return 'N'
    else if (degrees <= 101.25) {
    	if 		(degrees <= 33.75)							return 'NNE'
    	else if (degrees <= 56.25) 							return 'NE'
    	else if (degrees <= 78.75)							return 'ENE'
    	else /*if (degrees <= 101.25)*/						return 'E'
    } else if (degrees <= 191.25) {
    	if		(degrees <= 123.75)							return 'ESE'
        else if (degrees <= 146.25)							return 'SE'
        else if (degrees <= 168.75)							return 'SSE'
        else /*if (degrees <= 191.25)*/						return 'S'
    } else if (degrees <= 281.25) {
    	if 		(degrees <= 213.75)							return 'SSW'
        else if (degrees <= 236.25)							return 'SW'
        else if (degrees <= 258.75)							return 'WSW'
        else /*if (degrees <= 281.25)*/						return 'W'
    } else /*if (degrees <= 348.75)*/ {
    	if 		(degrees <= 303.75)							return 'WNW'
        else if (degrees <= 326.75)							return 'NW'
        else /*if (degrees <= 348.75)*/						return 'NNW'
    }
}
private localDate(timeZone) {
    def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
    df.setTimeZone(TimeZone.getTimeZone(timeZone))
    df.format(new Date())
}
private send(map) {
    sendEvent(map)
}
private attr( attribute ) {
	return device?.currentValue( attribute )
}
String getWeatherText() {
	return device?.currentValue('weather')
}
private roundIt( String value, decimals=0 ) {
	return (value == null) ? null : value.toBigDecimal().setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
private roundIt( BigDecimal value, decimals=0) {
    return (value == null) ? null : value.setScale(decimals, BigDecimal.ROUND_HALF_UP) 
}
String getMeteoSensorID() {
    // def mw = state.meteoWeather
    // def version = (mw?.containsKey('version')) ? mw.version : 1.0
    def version = state.meteoWeather?.version
    def sensorID = (version && ( version > 3.5 )) ? '*' : '0'
    // if (debug) log.debug "version: ${version}, sensor: ${sensorID}"
    return sensorID   
}
def getForecastTemplate() {
	return '"fcast":{"text":"[forecast-text:]","code":"[forecast-rule:]"},'
}
def getMBSystemTemplate() {
	return '"version":[mbsystem-swversion:1.0],"lat":[mbsystem-latitude:""],"lon":[mbsystem-longitude:""],"tz":"[mbsystem-timezone:]",'		// don't need this currently: "station":"[mbsystem-station:]",'
}
def getYesterdayTemplate() {
	String s = getTemperatureScale() 
	String d = getMeteoSensorID() 
    // String d = '0'
    if (settings.shortStats) {
    	// yesterday:[max temp, min temp, max hum, min hum, total rain]
    	return "\"yesterday\":[[th${d}temp-ydmax=${s}.2:\"\"],[th${d}temp-ydmin=${s}.2:\"\"],[th${d}hum-ydmax=.2:\"\"],[th${d}hum-ydmin=.2:\"\"]," + yesterdayRainfall + ']}'
    } else {
    	return "\"yesterday\":{\"highTemp\":[th${d}temp-ydmax=${s}.2:\"\"],\"lowTemp\":[th${d}temp-ydmin=${s}.2:\"\"],\"highHum\":[th${d}hum-ydmax=.2:\"\"],\"lowHum\":[th${d}hum-ydmin=.2:\"\"]," + yesterdayRainfall + '}}'
    }
}
def getCurrentTemplate() {
	String d = getMeteoSensorID()
    String r = (height_units && (height_units == 'height_in')) ? 'in' : ''
	String a = avgUpdates ? "avg${updateMins}" : 'act'
    // String d = '0'
    if (settings.shortStats) { //shortStats
        // 0:date, 1:time, 2:humidity (out), 3:humidity (in), 
        // 4:temp (out), 5:dewpoint (out), 6:heatIndex, 7:windChill, 
        // 8:temp (in), 9:dewpoint (in), 10:high temp, 11:low temp,
        // 12:rain today, 13:rain last hour, 14:evapostranspiration, 15:rain rate,
        // 16:pressure, 17:pressure trend, 
        // 18:windDirText, 19:wind gust, 20:wind degrees, 21:wind speed, 22:wind bft,
        // 23:dayHours, 24:high hum, 25:low hum, 
        // 26:sunrise, 27:sunset, 28:dayMinutes, 29:uvIndex,
        // 30:solRad, 31:lunar age, 32:lunar percent, 33:lunar segment,
        // 34:moonrise, 35: moonset, 36:isday
		return '"current":["[M]/[D]/[YY]","[H]:[mm]:[ss] [apm]",' + "[th${d}hum-${a}=.2:\"\"],[thb${d}hum-${a}=.2:\"\"]," +
				temperatureTemplate + currentRainfall + pressureTemplate + windTemplate +
                "\"[mbsystem-daylength:]\",[th${d}hum-dmax=.2:\"\"],[th${d}hum-dmin=.2:\"\"]," +
                "\"[mbsystem-sunrise:]\",\"[mbsystem-sunset:]\",[mbsystem-daylength=mins.0:\"\"],[uv${d}index-${a}:\"\"]," +
                "[sol${d}rad-${a}:\"\"],[mbsystem-lunarage:\"\"],[mbsystem-lunarpercent:\"\"],[mbsystem-lunarsegment:\"\"]," +
                '"[mbsystem-moonrise:]","[mbsystem-moonset:]",[mbsystem-isday=.0:""]]}'
                            
    } else {	
        return "\"current\":{\"date\":\"[M]/[D]/[YY]\",\"time\":\"[H]:[mm]:[ss] [apm]\",\"humOut\":[th${d}hum-${a}=.2:\"\"],\"humIn\":[thb${d}hum-${a}=.2:\"\"]," + 
        		temperatureTemplate + currentRainfall + pressureTemplate + windTemplate +
                "\"dayHours\":\"[mbsystem-daylength:]\",\"highHum\":[th${d}hum-dmax=.2:\"\"],\"lowHum\":[th${d}hum-dmin=.2:\"\"]," +
                "\"sunrise\":\"[mbsystem-sunrise:]\",\"sunset\":\"[mbsystem-sunset:]\",\"dayMins\":[mbsystem-daylength=mins.0:\"\"],\"uvIndex\":[uv${d}index-${a}:\"\"]," +
                "\"solRad\":[sol${d}rad-${a}:\"\"],\"lunarAge\":[mbsystem-lunarage:\"\"],\"lunarPct\":[mbsystem-lunarpercent:\"\"],\"lunarSgt\":[mbsystem-lunarsegment:\"\"]," +
                '"moonrise":"[mbsystem-moonrise:]","moonset":"[mbsystem-moonset:]","isDay":[mbsystem-isday=.0:""]}}'
	}
}
def getTemperatureTemplate() { 
	String s = getTemperatureScale() 
    String d = getMeteoSensorID()
	String a = avgUpdates ? "avg${updateMins}" : 'act'
    // String d = '0'
    if (settings.shortStats) {
    	return "[th${d}temp-${a}=${s}.2:\"\"],[th${d}dew-${a}=${s}.2:\"\"],[th${d}heatindex-${a}=${s}.2:\"\"],[wind${d}chill-${a}=${s}.2:\"\"]," +
    				"[thb${d}temp-${a}=${s}.2:\"\"],[thb${d}dew-${a}=${s}.2:\"\"],[th${d}temp-dmax=${s}.2:\"\"],[th${d}temp-dmin=${s}.2:\"\"],"
    } else {
		return "\"tempOut\":[th${d}temp-${a}=${s}.2:\"\"],\"dewOut\":[th${d}dew-${a}=${s}.2:\"\"],\"heatIndex\":[th${d}heatindex-${a}=${s}.2:\"\"],\"windChill\":[wind${d}chill-${a}=${s}.2:\"\"]," +
    			"\"tempIn\":[thb${d}temp-${a}=${s}.2:\"\"],\"dewIn\":[thb${d}dew-${a}=${s}.2:\"\"],\"highTemp\":[th${d}temp-dmax=${s}.2:\"\"],\"lowTemp\":[th${d}temp-dmin=${s}.2:\"\"],"
    }
}
def getPressureTemplate() {
	String p = (pres_units && (pres_units == 'press_in')) ? 'inHg' : 'mmHg'
    String d = getMeteoSensorID()
	String a = avgUpdates ? "avg${updateMins}" : 'act'
    // String d = '0'
    if (settings.shortStats) {
    	return "[thb${d}seapress-${a}=${p}.2:\"\"],\"[thb${d}seapress-delta10=enbarotrend:]\","
    } else {
		return "\"pressure\":[thb${d}seapress-${a}=${p}.2:\"\"],\"pTrend\":\"[thb${d}seapress-delta10=enbarotrend:]\","
    }
}
def getYesterdayRainfall() {
	String r = (height_units && (height_units == 'height_in')) ? 'in' : ''
    String d = getMeteoSensorID()
    // String d = '0'
    if (settings.shortStats) {
    	return "[rain${d}total-ydaysum=${r}.3:\"\"]"
    } else {
		return "\"rainfall\":[rain${d}total-ydaysum=${r}.3:\"\"]" 
    }
}
def getCurrentRainfall() {
	String r = (height_units && (height_units == 'height_in')) ? 'in' : ''
    String d = getMeteoSensorID()
	String a = avgUpdates ? "avg${updateMins}" : 'act'
    // String d = '0'
    if (settings.shortStats) {
    	return "[rain${d}total-daysum=${r}.3:\"\"],[rain${d}total-sum1h=${r}.3:\"\"],[sol${d}evo-${a}=${r}.3:\"\"],[rain${d}rate-${a}=${r}.3:\"\"],"
    } else {
		return "\"rainfall\":[rain${d}total-daysum=${r}.3:\"\"],\"rainLastHour\":[rain${d}total-sum1h=${r}.3:\"\"],\"evo\":[sol${d}evo-${a}=${r}.3:\"\"],\"rainRate\":[rain${d}rate-${a}=${r}.3:\"\"],"
    }
}
def getWindTemplate() {
    String s = (speed_units && (speed_units == 'speed_mph')) ? 'mph' : 'kmh'
    String d = getMeteoSensorID()
	String a = avgUpdates ? "avg${updateMins}" : 'act'
    // String d = '0'
    if (settings.shortStats) {
		return "\"[wind${d}dir-${a}=endir:N/A]\",[wind${d}wind-max10=${s}.2:\"\"],[wind${d}dir-${a}:\"\"],[wind${d}wind-${a}=${s}.2:\"\"],[wind${d}wind-${a}=bft.0:\"\"],"
    } else {
		return "\"windTxt\":\"[wind${d}dir-${a}=endir:N/A]\",\"windGust\":[wind${d}wind-max10=${s}.2:\"\"],\"windDir\":[wind${d}dir-${a}:\"\"],\"wind\":[wind${d}wind-${a}=${s}.2:\"\"]," +
        		"\"windBft\":[wind${d}wind-${a}=bft.0:\"\"],"
    			// "\"windTxt\":\"[wind${d}dir-${a}=endir:]\"," "[wind*dir-${a}=endir:]" \"windTxt\":\"[wind${d}dir-${a}=endir.3:N/A]\",
    }
}
def getTemperatureColors() {
    ( (fahrenheit) ? ([
        [value: 31, color: "#153591"],
        [value: 44, color: "#1e9cbb"],
        [value: 59, color: "#90d2a7"],
        [value: 74, color: "#44b621"],
        [value: 84, color: "#f1d801"],
        [value: 95, color: "#d04e00"],
        [value: 96, color: "#bc2323"]
    ]) : ([
        [value:  0, color: "#153591"],
        [value:  7, color: "#1e9cbb"],
        [value: 15, color: "#90d2a7"],
        [value: 23, color: "#44b621"],
        [value: 28, color: "#f1d801"],
        [value: 35, color: "#d04e00"],
        [value: 37, color: "#bc2323"]
    ]) )
}

String getBeaufortText(bftForce) {
	// Finish this sentence: "Winds are ..."
	switch (roundIt(bftForce,0)) {
		case 0: return 'calm';
		case 1: return 'light air';
		case 2: return 'a light breeze';
		case 3: return 'a gentle breeze';
		case 4: return 'a moderate breeze';
		case 5: return 'a fresh breeze';
		case 6: return 'a strong breeze';
		case 7: return 'near gale winds';
		case 8: return 'gale force winds';
		case 9: return 'strong gale winds';
		case 10: return 'storm force winds';
		case 11: return 'violent storm winds';
		case 12: return 'hurricane winds';
		default: return ''
	}
}

String getImgIcon(String weatherCode){
	if (debugOn) log.debug "weatherCode: ${weatherCode}"
    def LUitem = LUTable.find{ it.name == weatherCode }
	return (LUitem ? LUitem.icon : "https://raw.githubusercontent.com/SANdood/Icons/master/Weather/na.png")
}

String getOwmIcon(String weatherCode) {
	def LUitem = LUTable.find{ it.name == weatherCode }
	return (LUitem ? LUitem.	owmIcon : "50d")
}
			
private getImgText(weatherCode){
    def LUitem = LUTable.find{ it.name == weatherCode }    
	return (LUitem ? LUitem.label : "Unknown")
}

// https://github.com/SANdood/Icons/blob/master/Weather/0.png
@Field final List LUTable = [
		[ name: "chanceflurries", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: "Chance of Flurries", 					owmIcon: "13d" ],
		[ name: "chancelightsnow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: "Possible Light Snow", 					owmIcon: "13d" ],
		[ name: "chancelightsnowbreezy", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41b.png", 	label: "Possible Light Snow and Breezy",		owmIcon: "13d" ],
		[ name: "chancelightsnowwindy", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41b.png", 	label: "Possible Light Snow and Windy", 		owmIcon: "13d" ],
		[ name: "chancerain", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: "Chance of Rain", 						owmIcon: "10d" ],
		[ name: "chancedrizzle", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: "Chance of Drizzle", 					owmIcon: "10d" ],
		[ name: "chancelightrain", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: "Chance of Light Rain", 					owmIcon: "10d" ],
		[ name: "chancesleet", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: "Chance of Sleet", 						owmIcon: "13d" ],
		[ name: "chancesnow", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: "Chance of Snow", 						owmIcon: "13d" ],
		[ name: "chancetstorms", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/38.png", 	label: "Chance of Thunderstorms", 				owmIcon: "11d" ],
		[ name: "clear", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/32.png", 	label: "Clear", 								owmIcon: "01d" ],
		[ name: "humid", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/36.png", 	label: "Humid", 								owmIcon: "01d" ],
		[ name: "sunny", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/36.png", 	label: "Sunny", 								owmIcon: "01d" ],
		[ name: "clear-day",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/32.png", 	label: "Clear", 								owmIcon: "01d" ],
		[ name: "cloudy", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/26.png", 	label: "Overcast", 								owmIcon: "04d" ],
		[ name: "humid-cloudy",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/26.png", 	label: "Humid and Overcast", 					owmIcon: "04d" ],
		[ name: "flurries", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/13.png", 	label: "Snow Flurries", 						owmIcon: "13d" ],
		[ name: "scattered-flurries", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: "Scattered Snow Flurries", 				owmIcon: "13d" ],	
		[ name: "scattered-snow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: "Scattered Snow Showers", 				owmIcon: "13d" ],
		[ name: "lightsnow", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/14.png", 	label: "Light Snow", 							owmIcon: "13d" ],
		[ name: "frigid-ice", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/10.png", 	label: "Frigid / Ice Crystals", 				owmIcon: "13d" ],
		[ name: "fog", 							icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/20.png", 	label: "Foggy", 								owmIcon: "50d" ],
		[ name: "hazy", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/21.png", 	label: "Hazy", 									owmIcon: "50d" ],
		[ name: "smoke",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/22.png", 	label: "Smoke", 								owmIcon: "50d" ],
		[ name: "mostlycloudy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: "Mostly Cloudy", 						owmIcon: "04d" ],
		[ name: "mostly-cloudy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: "Mostly Cloudy", 						owmIcon: "04d" ],
		[ name: "mostly-cloudy-day",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: "Mostly Cloudy", 						owmIcon: "04d" ],
		[ name: "humid-mostly-cloudy", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: "Humid and Mostly Cloudy", 				owmIcon: "04d" ],
		[ name: "humid-mostly-cloudy-day", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: "Humid and Mostly Cloudy", 				owmIcon: "04d" ],
		[ name: "mostlysunny", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/34.png", 	label: "Mostly Sunny", 							owmIcon: "02d" ],
		[ name: "partlycloudy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30.png", 	label: "Partly Cloudy", 						owmIcon: "03d" ],
		[ name: "partly-cloudy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30.png", 	label: "Partly Cloudy", 						owmIcon: "03d" ],
		[ name: "partly-cloudy-day",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30.png", 	label: "Partly Cloudy", 						owmIcon: "03d" ],
		[ name: "humid-partly-cloudy", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30.png", 	label: "Humid and Partly Cloudy", 				owmIcon: "03d" ],
		[ name: "humid-partly-cloudy-day", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30.png", 	label: "Humid and Partly Cloudy", 				owmIcon: "03d" ],
		[ name: "partlysunny", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28.png", 	label: "Partly Sunny", 							owmIcon: "02d" ],	
		[ name: "rain", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/12.png", 	label: "Rain", 									owmIcon: "10d" ],
		[ name: "rain-breezy",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: "Rain and Breezy", 						owmIcon: "10d" ],
		[ name: "rain-windy",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: "Rain and Windy", 						owmIcon: "10d" ],
		[ name: "rain-windy!",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: "Rain and Dangerously Windy", 			owmIcon: "10d" ],
		[ name: "heavyrain", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/12.png", 	label: "Heavy Rain", 							owmIcon: "10d" ],
		[ name: "heavyrain-breezy",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: "Heavy Rain and Breezy", 				owmIcon: "10d" ],
		[ name: "heavyrain-windy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: "Heavy Rain and Windy", 					owmIcon: "10d" ],
		[ name: "heavyrain-windy!", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: "Heavy Rain and Dangerously Windy", 		owmIcon: "10d" ],
		[ name: "drizzle",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/9.png", 	label: "Drizzle", 								owmIcon: "09d" ],
		[ name: "lightdrizzle",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/9.png", 	label: "Light Drizzle", 						owmIcon: "09d" ],
		[ name: "heavydrizzle",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/9.png", 	label: "Heavy Drizzle", 						owmIcon: "09d" ],
		[ name: "lightrain",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: "Light Rain", 							owmIcon: "10d" ],
		[ name: "scattered-showers",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: "Scattered Showers", 					owmIcon: "09d" ],
		[ name: "lightrain-breezy",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: "Light Rain and Breezy", 				owmIcon: "10d" ],
		[ name: "lightrain-windy",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: "Light Rain and Windy", 					owmIcon: "10d" ],
		[ name: "lightrain-windy!",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: "Light Rain and Dangerously Windy", 		owmIcon: "10d" ],	
		[ name: "sleet",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/10.png", 	label: "Sleet", 								owmIcon: "13d" ],
		[ name: "lightsleet",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/8.png", 	label: "Light Sleet", 							owmIcon: "13d" ],
		[ name: "heavysleet",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/10.png", 	label: "Heavy Sleet", 							owmIcon: "13d" ],
		[ name: "rain-sleet",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/6.png", 	label: "Rain and Sleet", 						owmIcon: "13d" ],
		[ name: "winter-mix",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/7.png", 	label: "Wintery Mix of Snow and Sleet", 		owmIcon: "13d" ],
		[ name: "freezing-drizzle",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/8.png", 	label: "Freezing Drizzle", 						owmIcon: "13d" ],
		[ name: "freezing-rain",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/10.png", 	label: "Freezing Rain", 						owmIcon: "13d" ],
		[ name: "snow", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/15.png", 	label: "Snow", 									owmIcon: "13d" ],
		[ name: "heavysnow", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/16.png", 	label: "Heavy Snow", 							owmIcon: "13d" ],
		[ name: "blizzard", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/42.png", 	label: "Blizzard", 								owmIcon: "13d" ],
		[ name: "rain-snow", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/7.png", 	label: "Rain to Snow Showers", 					owmIcon: "13d" ],
		[ name: "tstorms", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/0.png", 	label: "Thunderstorms", 						owmIcon: "11d" ],
		[ name: "tstorms-iso", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/37.png", 	label: "Isolated Thunderstorms", 				owmIcon: "11d" ],
		[ name: "thunderstorm", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/0.png", 	label: "Thunderstorm", 							owmIcon: "11d" ],
		[ name: "windy",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Windy", 								owmIcon: "50d" ],
		[ name: "wind",							icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Windy", 								owmIcon: "50d" ],
		[ name: "sandstorm",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/19b.png", 	label: "Blowing Dust / Sandstorm", 				owmIcon: "50d" ],
		[ name: "blowing-spray",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Windy / Blowing Spray", 				owmIcon: "50d" ],
		[ name: "wind!",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Dangerously Windy", 					owmIcon: "50d" ],
		[ name: "wind-foggy",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Windy and Foggy", 						owmIcon: "50d" ],
		[ name: "wind-overcast",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24c.png", 	label: "Windy and Overcast", 					owmIcon: "50d" ],
		[ name: "wind-overcast!",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24c.png", 	label: "Dangerously Windy and Overcast", 		owmIcon: "50d" ],
		[ name: "wind-partlycloudy",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30b.png", 	label: "Windy and Partly Cloudy", 				owmIcon: "50d" ],
		[ name: "wind-partlycloudy!", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30b.png", 	label: "Dangerously Windy and Partly Cloudy", 	owmIcon: "50d" ],
		[ name: "wind-mostlycloudy",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28b.png", 	label: "Windy and Mostly Cloudy", 				owmIcon: "50d" ],
		[ name: "wind-mostlycloudy!",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28b.png", 	label: "Dangerously Windy and Mostly Cloudy", 	owmIcon: "50d" ],
		[ name: "breezy",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Breezy", 								owmIcon: "50d" ],
		[ name: "breezy-overcast",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24b.png", 	label: "Breezy and Overcast", 					owmIcon: "04d" ],
		[ name: "breezy-partlycloudy", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/30b.png", 	label: "Breezy and Partly Cloudy", 				owmIcon: "03d" ],
		[ name: "breezy-mostlycloudy", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/28b.png", 	label: "Breezy and Mostly Cloudy", 				owmIcon: "04d" ],
		[ name: "breezy-foggy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/20b.png", 	label: "Breezy and Foggy", 						owmIcon: "50d" ],	
		[ name: "tornado",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/to.png",	label: "Tornado", 								owmIcon: "50d" ],
		[ name: "hail",							icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/18.png",	label: "Hail Storm", 							owmIcon: "09d" ],
		[ name: "thunder-hail",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png",	label: "Thunder and Hail Storm", 				owmIcon: "11d" ],
		[ name: "rain-hail",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/7.png",	label: "Mixed Rain and Hail", 					owmIcon: "09d" ],
		[ name: "nt_chanceflurries", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: "Chance of Flurries", 					owmIcon: "13d" ],
		[ name: "chancelightsnow-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png", 	label: "Possible Light Snow", 					owmIcon: "13n" ],
		[ name: "chancelightsnowbz-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46b.png", 	label: "Possible Light Snow and Breezy", 		owmIcon: "13n" ],
		[ name: "chancelightsnowy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46b.png", 	label: "Possible Light Snow and Windy", 		owmIcon: "13n" ],
		[ name: "nt_chancerain", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: "Chance of Rain", 						owmIcon: "09n" ],
		[ name: "chancerain-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: "Chance of Rain", 						owmIcon: "09n" ],
		[ name: "chancelightrain-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/45.png", 	label: "Chance of Light Rain", 					owmIcon: "09n" ],
		[ name: "nt_chancesleet", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: "Chance of Sleet", 						owmIcon: "13n" ],
		[ name: "chancesleet-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: "Chance of Sleet", 						owmIcon: "13n" ],
		[ name: "nt_chancesnow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png", 	label: "Chance of Snow", 						owmIcon: "13n" ],
		[ name: "chancesnow-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png", 	label: "Chance of Snow", 						owmIcon: "13n" ],
		[ name: "nt_chancetstorms", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/47.png",	label: "Chance of Thunderstorms", 				owmIcon: "11n" ],
		[ name: "chancetstorms-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/47.png",	label: "Chance of Thunderstorms", 				owmIcon: "11n" ],
		[ name: "nt_clear", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31.png", 	label: "Clear", 								owmIcon: "01n" ],
		[ name: "clear-night",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31.png", 	label: "Clear", 								owmIcon: "01n" ],
		[ name: "humid-night",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31.png", 	label: "Humid", 								owmIcon: "01n" ],
		[ name: "nt_sunny", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31.png", 	label: "Clear", 								owmIcon: "01n" ],
		[ name: "nt_cloudy", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/26.png", 	label: "Overcast", 								owmIcon: "04n" ],
		[ name: "cloudy-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/26.png", 	label: "Overcast", 								owmIcon: "04n" ],
		[ name: "humid-cloudy-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/26.png", 	label: "Humid and Overcast", 					owmIcon: "04n" ],	
		[ name: "nt_fog", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31b.png", 	label: "Foggy", 								owmIcon: "50n" ],
		[ name: "fog-night", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31b.png", 	label: "Foggy", 								owmIcon: "50n" ],
		[ name: "nt_hazy", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/31b.png", 	label: "Hazy", 									owmIcon: "50n" ],
		[ name: "nt_mostlycloudy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27.png",	label: "Mostly Cloudy", 						owmIcon: "04n" ],
		[ name: "mostly-cloudy-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27.png",	label: "Mostly Cloudy", 						owmIcon: "04n" ],
		[ name: "humid-mostly-cloudy-night",	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27.png", 	label: "Humid and Mostly Cloudy", 				owmIcon: "04n" ],
		[ name: "nt_mostlysunny", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/33.png",	label: "Mostly Clear", 							owmIcon: "02n" ],
		[ name: "nt_partlycloudy", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29.png",	label: "Partly Cloudy", 						owmIcon: "03n" ],
		[ name: "partly-cloudy-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29.png",	label: "Partly Cloudy", 						owmIcon: "03n" ],
		[ name: "humid-partly-cloudy-night",	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29.png", 	label: "Humid and Partly Cloudy", 				owmIcon: "03n" ],
		[ name: "nt_partlysunny", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27.png",	label: "Partly Clear", 							owmIcon: "02n" ],
		[ name: "nt_scattered-flurries", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/13.png", 	label: "Flurries", 								owmIcon: "13n" ],
		[ name: "nt_flurries", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/41.png", 	label: "Scattered Flurries", 					owmIcon: "13n" ],
		[ name: "flurries-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/13.png", 	label: "Flurries", 								owmIcon: "13n" ],
		[ name: "lightsnow-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/14.png", 	label: "Light Snow", 							owmIcon: "13n" ],
		[ name: "nt_rain", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: "Rain", 									owmIcon: "10n" ],
		[ name: "rain-night", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: "Rain", 									owmIcon: "10n" ],
		[ name: "rain-breezy-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: "Rain and Breezy", 						owmIcon: "10n" ],
		[ name: "rain-windy-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: "Rain and Windy", 						owmIcon: "10n" ],
		[ name: "rain-windy-night!", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/2.png", 	label: "Rain and Dangerously Windy", 			owmIcon: "10n" ],
		[ name: "heavyrain-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/12.png", 	label: "Heavy Rain", 							owmIcon: "10n" ],
		[ name: "heavyrain-breezy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png",	label: "Heavy Rain and Breezy", 				owmIcon: "10n" ],
		[ name: "heavyrain-windy-night",		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png", 	label: "Heavy Rain and Windy", 					owmIcon: "10n" ],
		[ name: "heavyrain-windy-night!", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/1.png",	label: "Heavy Rain and Dangerously Windy", 		owmIcon: "10n" ],
		[ name: "nt_drizzle", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/9.png", 	label: "Drizzle", 								owmIcon: "09n" ],
		[ name: "drizzle-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/9.png", 	label: "Drizzle", 								owmIcon: "09n" ],
		[ name: "nt_lightrain", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: "Light Rain", 							owmIcon: "09n" ],
		[ name: "lightrain-night", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: "Light Rain", 							owmIcon: "09n" ],	
		[ name: "nt_scattered-rain", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/39.png", 	label: "Scattered Showers", 					owmIcon: "09n" ],
		[ name: "lightrain-breezy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: "Light Rain and Breezy", 				owmIcon: "09n" ],
		[ name: "lightrain-windy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: "Light Rain and Windy", 					owmIcon: "09n" ],
		[ name: "lightrain-windy-night!", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/11.png", 	label: "Light Rain and Dangerously Windy", 		owmIcon: "09n" ],
		[ name: "nt_sleet", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: "Sleet", 								owmIcon: "13n" ],
		[ name: "sleet-night", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: "Sleet", 								owmIcon: "13n" ],
		[ name: "lightsleet-night",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: "Sleet", 								owmIcon: "13n" ],
		[ name: "nt_rain-sleet",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png",	label: "Rain and Sleet", 						owmIcon: "13n" ],
		[ name: "nt_thunder-hail",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/47.png",	label: "Thunder and Hail Storm", 				owmIcon: "11n" ],
		[ name: "nt_winter-mix",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/7.png",	label: "Winter Mix of Sleet and Snow", 			owmIcon: "13n" ],
		[ name: "nt_freezing-drizzle", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/8.png",	label: "Freezing Drizzle", 						owmIcon: "13n" ],
		[ name: "nt_freezing-rain", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/8.png",	label: "Freezing Rain", 						owmIcon: "13n" ],
		[ name: "nt_snow", 						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png,",	label: "Snow", 									owmIcon: "13n" ],
		[ name: "nt_rain-snow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png,",	label: "Rain and Snow Showers", 				owmIcon: "13n" ],
		[ name: "snow-night", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46.png,",	label: "Snow", 									owmIcon: "13n" ],
		[ name: "nt_heavysnow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/42.png,",	label: "Heavy Snow", 							owmIcon: "13n" ],
		[ name: "nt_heavysnow", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/42.png,",	label: "Heavy Snow", 							owmIcon: "13n" ],
		[ name: "nt_tstorms", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/47.png",	label: "Thunderstorms", 						owmIcon: "11n" ],
		[ name: "nt_blizzard", 					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/42.png",	label: "Blizzard", 								owmIcon: "13n" ],
		[ name: "nt_thunderstorm", 				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/0.png",	label: "Thunderstorm", 							owmIcon: "11n" ],
		[ name: "thunderstorm-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/0.png",	label: "Thunderstorm", 							owmIcon: "11n" ],
		[ name: "nt_windy",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Windy", 								owmIcon: "50n" ],
		[ name: "windy-night",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Windy", 								owmIcon: "50n" ],
		[ name: "wind-night",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Windy", 								owmIcon: "50n" ],
		[ name: "wind-night!",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Dangerously Windy", 					owmIcon: "50n" ],
		[ name: "wind-foggy-night",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/20b.png", 	label: "Windy and Foggy", 						owmIcon: "50n" ],
		[ name: "wind-overcast-night", 			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24b.png", 	label: "Windy and Overcast", 					owmIcon: "50n" ],
		[ name: "wind-overcast-night!", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24b.png", 	label: "Dangerously Windy and Overcast", 		owmIcon: "50n" ],
		[ name: "wind-partlycloudy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29b.png", 	label: "Windy and Partly Cloudy", 				owmIcon: "50n" ],
		[ name: "wind-partlycloudy-night!", 	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29b.png", 	label: "Dangerously Windy and Partly Cloudy", 	owmIcon: "50n" ],
		[ name: "wind-mostlycloudy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27b.png", 	label: "Windy and Mostly Cloudy", 				owmIcon: "50n" ],
		[ name: "wind-mostly-cloudy-night!",	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27b.png", 	label: "Dangerously Windy and Mostly Cloudy", 	owmIcon: "50n" ],
		[ name: "breezy-night",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/23.png", 	label: "Breezy", 								owmIcon: "50n" ],
		[ name: "breezy-overcast-night",		icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/24b.png", 	label: "Breezy and Overcast", 					owmIcon: "04n" ],
		[ name: "breezy-partlycloudy-night",	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/29b.png", 	label: "Breezy and Partly Cloudy", 				owmIcon: "03n" ],
		[ name: "breezy-mostlycloudy-night",	icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/27b.png", 	label: "Breezy and Mostly Cloudy", 				owmIcon: "04n" ],
		[ name: "breezy-foggy-night",			icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/20b.png", 	label: "Breezy and Foggy", 						owmIcon: "50n" ],
		[ name: "nt_tornado",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/to.png",	label: "Tornado", 								owmIcon: "50n" ],
		[ name: "tornado-night",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/to.png",	label: "Tornado", 								owmIcon: "50n" ],
		[ name: "nt_hail",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46b.png",	label: "Hail", 									owmIcon: "09n" ],
		[ name: "hail-night",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/46b.png",	label: "Hail", 									owmIcon: "09n" ],
		[ name: "na",							icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/na.png",	label: "Not Available", 						owmIcon: "50d" ],
        [ name: "unknown",						icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/na.png",	label: "Unknown", 								owmIcon: "50d" ],
		[ name: "hurricane",					icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/hc.png",	label: "Hurricane", 							owmIcon: "50d" ],
		[ name: "tropical-storm",				icon:"https://raw.githubusercontent.com/SANdood/Icons/master/Weather/38.png",	label: "Tropical Storm", 						owmIcon: "50d" ]
	] 
//******************************************************************************************
