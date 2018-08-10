/**
*  Copyright 2015 SmartThings
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
*  Date: 2018-07-04
*
*	Updates by Barry A. Burke (storageanarchy@gmail.com)
*	Date: 2017 - 2018
*
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
*
*/
include 'asynchttp_v1'
import groovy.json.JsonSlurper

def getVersionNum() { return "1.0.21" }
private def getVersionLabel() { return "Meteobridge Weather Station, version ${getVersionNum()}" }
def getDebug() { false }
def getFahrenheit() { true }		// Set to false for Celsius color scale
def getCelsius() { !fahrenheit }
def getSummaryText() { true }


metadata {
    definition (name: "Meteobridge Weather Station", namespace: "sandood", author: "sandood") {
        capability "Illuminance Measurement"
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Water Sensor"
        capability "Ultraviolet Index"
        capability "Sensor"
        capability "Refresh"

        attribute "heatIndex", "number"
        if (debug) attribute "heatIndexDisplay", "string"
        attribute "uvIndex", "number"				// Also 'ultravioletIndex' per ST capabilities 07/19/2018
        attribute "dewpoint", "number"
        attribute "pressure", "number"
        if (debug) attribute "pressureDisplay", "string"
        attribute "pressureTrend", "string"
        attribute "solarRadiation", "number"
        attribute "evapotranspiration", "number"
        if (debug) attribute "etDisplay", "string"
        
        attribute "highTempYesterday", "number"
        attribute "lowTempYesterday", "number"
        attribute "highTemp", "number"
        attribute "lowTemp", "number"
        attribute "highTempForecast", "number"
        attribute "lowTempForecast", "number"
        attribute "highTempTomorrow", "number"
        attribute "lowTempTomorrow", "number"
        
		attribute "highHumYesterday", "number"
        attribute "lowHumYesterday", "number"
        attribute "highHumidity", "number"
        attribute "lowHumidity", "number"
        attribute "avgHumForecast", "number"
        attribute "avgHumTomorrow", "number"

        attribute "precipYesterday", "number"
        if (debug) attribute "precipYesterdayDisplay", "string"
        attribute "precipToday", "number"
        if (debug) attribute "precipTodayDisplay", "string"
        attribute "precipForecast", "number"
        if (debug) attribute "precipFcstDisplay", "string"
        attribute "precipLastHour", "number"
        if (debug) attribute "precipLastHourDisplay", "string"
        attribute "precipRate", "number"
        if (debug) attribute "precipRateDisplay", "string"
        attribute "precipTomorrow", "number"
        if (debug) attribute "precipTomDisplay", "string"
        attribute "pop", "number"					// Probability of Precipitation (in %)
        if (debug) attribute "popDisplay", "string"
        attribute "popForecast", "number"
        if (debug) attribute "popFcstDisplay", "string"
        attribute "popTomorrow", "number"
        if (debug) attribute "popTomDisplay", "string"
        attribute "water", "string"

		attribute "weather", "string"
        attribute "weatherIcon", "string"
        attribute "forecast", "string"
        attribute "forecastCode", "string"
        
        attribute "airQualityIndex", "number"
        attribute "aqi", "number"
        attribute "wind", "number"
        attribute "windDirection", "string"
        attribute "windGust", "number"
        attribute "windChill", "number"
        if (debug) attribute "windChillDisplay", "string"
        attribute "windDirectionDegrees", "number"
        if (debug) attribute "windinfo", "string"        

        
        
        attribute "sunrise", "string"
        attribute "sunriseAPM", "string"
        attribute "sunriseEpoch", "number"
        attribute "sunset", "string"
        attribute "sunsetAPM", "string"
        attribute "sunsetEpoch", "number"
        attribute "dayHours", "string"
        attribute "dayMinutes", "number"
        attribute "isDay", "number"
        attribute "isNight", "number"
        
        attribute "moonrise", "string"
        attribute "moonriseAPM", "string"
        attribute "moonriseEpoch", "number"
        attribute "moonset", "string"
        attribute "moonsetAPM", "string"
        attribute "moonsetEpoch", "number"
        attribute "lunarSegment", "number"
        attribute "lunarAge", "number"
        attribute "lunarPercent", "number"
        attribute "moonPhase", "string"
        if (debug) attribute "moonPercent", "number"
        if (debug) attribute "moonDisplay", "string"
        if (debug) attribute "moonInfo", "string"
        
        attribute "locationName", "string"
        attribute "currentDate", "string"
  		attribute "lastSTupdate", "string"
        attribute "timestamp", "string"
        
        if (debug) {
        	attribute "meteoTemplate", "string"			// For debugging only
        	attribute "purpleAir", "string"				// For debugging only
        	attribute "meteoWeather", "string"			// For debugging only
        	attribute "iconErr", "string"				// For debugging only
        	attribute "wundergroundObs", "string"		// For debugging only
        	attribute "darkSkyWeather", "string"		// For debugging only
        }
        
        if (summaryText) attribute "summaryList", "string"
        if (summaryText) attribute "summaryMap", "string"
        
        command "refresh"
		command "getWeatherReport"
    }

    preferences {
    	input(name: 'updateMins', type: 'enum', description: "Select the update frequency", 
        	title: "${getVersionLabel()}\n\nUpdate frequency (minutes)", displayDuringSetup: true, defaultValue: '5', options: ['1', '3', '5','10','15','30'], required: true)
        
        input(name: "zipCode", type: "text", title: "Zip Code or PWS (optional)", required: false, displayDuringSetup: true, description: 'Specify ZipCode or pws:')
        
        input (description: "Setup Meteobridge access", title: "Meteobridge Setup", displayDuringSetup: true, type: 'paragraph', element: 'MeteoBridge')
        input "meteoIP", "string", title:"Meteobridge IP Address", description: "Eenter your Meteobridge's IP Address", required: true, displayDuringSetup: true
 		input "meteoPort", "string", title:"Meteobridge Port", description: "Enter your Meteobridge's Port", defaultValue: 80 , required: true, displayDuringSetup: true
    	input "meteoUser", "string", title:"Meteobridge User", description: "Enter your Meteobridge's username", required: true, defaultValue: 'meteobridge', displayDuringSetup: true
    	input "meteoPassword", "password", title:"Meteobridge Password", description: "Enter your Meteobridge's password", required: true, displayDuringSetup: true
        
        input ("purpleID", "string", title: 'Purple Air Sensor ID (optional)', description: 'Enter your PurpleAir Sensor ID', required: false, displayDuringSetup: true)

        input ("darkSkyKey", "string", title: 'DarkSky Secret Key', description: 'Enter your DarkSky key (from darksky.net)', required: false, displayDuringSetup: true)
        
        input ("fcstSource", "enum", title: 'Select weather forecast source', description: "Select the source for your weather forecast (default=Meteobridge)", required: false, displayDuringSetup: true,
        		options: (darkSkyKey!=''?['darksky':'Dark Sky']:[]) + ['meteo': 'Meteobridge', 'wunder':'Weather Underground'])
                
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
                attributeState "chanceflurries", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_flurries_11_fc.png", 					label: "Chance of Flurries"
                attributeState "chancelightsnow", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_flurries_11_fc.png", 					label: "Chance of Light Snow"
                attributeState "chancerain", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_drizzle_05_fc.png", 					label: "Chance of Rain"
                attributeState "chancedrizzle", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_drizzle_05_fc.png", 					label: "Chance of Drizzle"
                attributeState "chancelightrain", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_drizzle_05_fc.png", 					label: "Chance of Light Rain"
                attributeState "chancesleet", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_freezing_rain_07_fc.png", 				label: "Chance of Sleet"
                attributeState "chancesnow", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_snow_10_fc.png", 						label: "Chance of Snow"
                attributeState "chancetstorms", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_thunderstorms_15_fc.png", 				label: "Chance of Thunderstorms"
                attributeState "clear", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_sunny_00_fc.png", 						label: "Clear"
                attributeState "humid", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_sunny_00_fc.png", 						label: "Humid"
                attributeState "sunny", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_sunny_00_fc.png", 						label: "Sunny"
                attributeState "clear-day",			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_sunny_00_fc.png", 						label: "Clear"
                attributeState "cloudy", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_cloudy_04_fc.png", 					label: "Overcast"
                attributeState "humid-cloudy",		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_cloudy_04_fc.png", 					label: "Humid and Overcast"
                attributeState "flurries", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_flurries_11_fc.png", 					label: "Flurries"
                attributeState "lightsnow", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_flurries_11_fc.png", 					label: "Light Snow"
                attributeState "fog", 				icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png", 						label: "Foggy"
                attributeState "hazy", 				icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png", 						label: "Hazy"
                attributeState "mostlycloudy", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_mostly_cloudy_03_fc.png", 				label: "Mostly Cloudy"
                attributeState "mostly-cloudy", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_mostly_cloudy_03_fc.png", 				label: "Mostly Cloudy"
                attributeState "mostly-cloudy-day",	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_mostly_cloudy_03_fc.png", 				label: "Mostly Cloudy"
                attributeState "humid-mostly-cloudy", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_mostly_cloudy_03_fc.png", 		    label: "Humid and Mostly Cloudy"
                attributeState "humid-mostly-cloudy-day", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_mostly_cloudy_03_fc.png", 		label: "Humid and Mostly Cloudy"
                attributeState "mostlysunny", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png", 				label: "Mostly Sunny"
                attributeState "partlycloudy", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png", 				label: "Partly Cloudy"
                attributeState "partly-cloudy", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png", 				label: "Partly Cloudy"
                attributeState "partly-cloudy-day",	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png", 				label: "Partly Cloudy"
                attributeState "humid-partly-cloudy", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png", 			label: "Humid and Partly Cloudy"
                attributeState "humid-partly-cloudy-day", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png", 		label: "Humid and Partly Cloudy"
                attributeState "partlysunny", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_mostly_cloudy_03_fc.png", 				label: "Partly Sunny"
                attributeState "rain", 				icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_rain_06_fc.png", 						label: "Rain"
                attributeState "heavyrain", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_rain_06_fc.png", 						label: "Heavy Rain"
                attributeState "drizzle",			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_drizzle_05_fc.png", 					label: "Drizzle"
                attributeState "lightrain",			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_drizzle_05_fc.png", 					label: "Light Rain"
                attributeState "sleet",				icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_freezing_rain_07_fc.png", 				label: "Sleet"
                attributeState "snow", 				icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_snow_10_fc.png", 						label: "Snow"
                attributeState "tstorms", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_thunderstorms_15_fc.png", 				label: "Thunderstorms"
                attributeState "thunderstorm", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_thunderstorms_15_fc.png", 				label: "Thunderstorm"
                attributeState "windy",				icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_windy_16.png", 							label: "Windy"
                attributeState "tornado",			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_tornado_17_fc.png",						label: "Tornado"
                attributeState "hail",				icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_flurries_11_fc.png",					label: "Hail"
                attributeState "nt_chanceflurries", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_flurries_111_fc.png", 			label: "Chance of Flurries"
                attributeState "chancelightsnow-night", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_flurries_111_fc.png", 		label: "Chance of Light Snow"
                attributeState "nt_chancerain", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_drizzle_105_fc.png", 				label: "Chance of Rain"
                attributeState "chancerain-night", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_drizzle_105_fc.png", 				label: "Chance of Rain"
                attributeState "chancelightrain-night", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_drizzle_105_fc.png", 			label: "Chance of Light Rain"
                attributeState "nt_chancesleet", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png",		label: "Chance of Sleet"
                attributeState "chancesleet-night", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png",		label: "Chance of Sleet"
                attributeState "nt_chancesnow", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_night_snow_110_fc.png", 				label: "Chance of Snow"
                attributeState "chancesnow-night", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_night_snow_110_fc.png", 				label: "Chance of Snow"
                attributeState "nt_chancetstorms", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_thunderstorms_115_fc.png",		label: "Chance of Thunderstorms"
                attributeState "chancetstorms-night", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_thunderstorms_115_fc.png",		label: "Chance of Thunderstorms"
                attributeState "nt_clear", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_clear_night_100_fc.png", 				label: "Clear"
                attributeState "clear-night",		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_clear_night_100_fc.png", 				label: "Clear"
                attributeState "humid-night",		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_clear_night_100_fc.png", 				label: "Humid"
                attributeState "nt_sunny", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_clear_night_100_fc.png", 				label: "Clear"
                attributeState "nt_cloudy", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_cloudy_04_fc.png", 						label: "Overcast"
                attributeState "cloudy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_cloudy_04_fc.png", 						label: "Overcast"
                attributeState "humid-cloudy-night", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_cloudy_04_fc.png", 					label: "Humid and Overcast"
                attributeState "nt_fog", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png", 						label: "Foggy"
                attributeState "fog-night", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png", 						label: "Foggy"
                attributeState "nt_hazy", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png", 						label: "Hazy"
                attributeState "nt_mostlycloudy", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_mostly_cloudy_103_fc.png",		label: "Mostly Cloudy"
                attributeState "mostly-cloudy-night", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_mostly_cloudy_103_fc.png",		label: "Mostly Cloudy"
                attributeState "humid-mostly-cloudy-night", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_mostly_cloudy_103_fc.png", label: "Humid and Mostly Cloudy"
                attributeState "nt_mostlysunny", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_partly_cloudy_101_fc.png",		label: "Mostly Clear"
                attributeState "nt_partlycloudy", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_partly_cloudy_101_fc.png",		label: "Partly Cloudy"
                attributeState "partly-cloudy-night", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_partly_cloudy_101_fc.png",		label: "Partly Cloudy"
                attributeState "humid-partly-cloudy-night", icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_partly_cloudy_101_fc.png", label: "Humid and Partly Cloudy"
                attributeState "nt_partlysunny", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_mostly_cloudy_103_fc.png",		label: "Partly Clear"
                attributeState "nt_flurries", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_flurries_111_fc.png", 			label: "Flurries"
                attributeState "lightsnow-night", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_flurries_111_fc.png", 			label: "Light Snow"
                attributeState "nt_rain", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_rain_106_fc.png", 				label: "Rain"
                attributeState "rain-night", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_rain_106_fc.png", 				label: "Rain"
                attributeState "heavyrain-night", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_rain_106_fc.png", 				label: "Heavy Rain"
                attributeState "nt_drizzle", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_drizzle_105_fc.png", 				label: "Drizzle"
                attributeState "lightrain-night", 	icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_drizzle_105_fc.png", 				label: "Light Rain"
                attributeState "nt_sleet", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png",		label: "Sleet"
                attributeState "sleet-night", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png",		label: "Sleet"
                attributeState "nt_snow", 			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_night_snow_110_fc.png,",				label: "Snow"
                attributeState "snow-night", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_night_snow_110_fc.png,",				label: "Snow"
                attributeState "nt_tstorms", 		icon:"shttps://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_thunderstorms_115_fc.png",		label: "Thunderstorms"
                attributeState "nt_thunderstorm", 	icon:"shttps://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_thunderstorms_115_fc.png",		label: "Thunderstorm"
                attributeState "thunderstorm-night", icon:"shttps://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_thunderstorms_115_fc.png",		label: "Thunderstorm"
                attributeState "nt_cloudy", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_cloudy_04_fc.png", 						label: "Overcast"
                attributeState "cloudy-night", 		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_cloudy_04_fc.png", 						label: "Overcast"
                attributeState "nt_windy",			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_windy_16.png", 							label: "Windy"
                attributeState "windy-night",		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_windy_16.png", 							label: "Windy"
                attributeState "nt_tornado",		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_tornado_17_fc.png",						label: "Tornado"
                attributeState "tornado-night",		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_tornado_17_fc.png",						label: "Tornado"
                attributeState "nt_hail",			icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_flurries_111_fc.png",				label: "Hail"
                attributeState "hail-night",		icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_flurries_111_fc.png",				label: "Hail"
            }
        }    
        standardTile('moonPhase', 'device.moonPhase', decoration: 'flat', inactiveLabel: false, width: 1, height: 1) {
        	state "New", 			 label: '', icon: "https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Lunar0.png"
            state "Waxing Crescent", label: '', icon: "https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Lunar1.png"
            state "First Quarter", 	 label: '', icon: "https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Lunar2.png"
            state "Waxing Gibbous",  label: '', icon: "https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Lunar3.png"
            state "Full", 			 label: '', icon: "https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Lunar4.png"
            state "Waning Gibbous",  label: '', icon: "https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Lunar5.png"
            state "Third Quarter", 	 label: '', icon: "https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Lunar6.png"
            state "Waning Crescent", label: '', icon: "https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Lunar7.png"          
        }
        standardTile('moonDisplay', 'device.moonDisplay', decoration: 'flat', inactiveLabel: false, width: 1, height: 1) {
        	state 'Moon-waning-000', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-000.png'
        	state 'Moon-waning-005', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-005.png'
            state 'Moon-waning-010', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-010.png'
        	state 'Moon-waning-015', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-015.png'
            state 'Moon-waning-020', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-020.png'
        	state 'Moon-waning-025', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-025.png'
            state 'Moon-waning-030', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-030.png'
        	state 'Moon-waning-035', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-035.png'
            state 'Moon-waning-040', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-040.png'
        	state 'Moon-waning-045', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-045.png'
            state 'Moon-waning-050', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-050.png'
        	state 'Moon-waning-055', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-055.png'
            state 'Moon-waning-060', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-060.png'
        	state 'Moon-waning-065', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-065.png'
            state 'Moon-waning-070', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-070.png'
        	state 'Moon-waning-075', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-075.png'
        	state 'Moon-waning-080', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-080.png'
        	state 'Moon-waning-085', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-085.png'
            state 'Moon-waning-090', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-090.png'
        	state 'Moon-waning-095', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-095.png'
            state 'Moon-waning-100', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waning-100.png'
            state 'Moon-waxing-000', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-000.png'
        	state 'Moon-waxing-005', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-005.png'
            state 'Moon-waxing-010', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-010.png'
        	state 'Moon-waxing-015', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-015.png'
            state 'Moon-waxing-020', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-020.png'
        	state 'Moon-waxing-025', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-025.png'
            state 'Moon-waxing-030', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-030.png'
        	state 'Moon-waxing-035', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-035.png'
            state 'Moon-waxing-040', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-040.png'
        	state 'Moon-waxing-045', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-045.png'
            state 'Moon-waxing-050', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-050.png'
        	state 'Moon-waxing-055', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-055.png'
            state 'Moon-waxing-060', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-060.png'
        	state 'Moon-waxing-065', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-065.png'
            state 'Moon-waxing-070', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-070.png'
        	state 'Moon-waxing-075', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-075.png'
        	state 'Moon-waxing-080', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-080.png'
        	state 'Moon-waxing-085', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-085.png'
            state 'Moon-waxing-090', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-090.png'
        	state 'Moon-waxing-095', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-095.png'
            state 'Moon-waxing-100', label: '', icon: 'https://raw.githubusercontent.com/SANdood/MeteoWeather/master/images/Moon-waxing-100.png'
        }
        valueTile("mooninfo", "device.moonInfo", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label: '${currentValue}'
        }
        valueTile("lastSTupdate", "device.lastSTupdate", inactiveLabel: false, width: 3, height: 1, decoration: "flat", wordWrap: true) {
            state("default", label: 'Updated\nat ${currentValue}')
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
            state "default", label:'YDA'
        }
        valueTile("tomorrowTile", "device.locationName", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'TMW'
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
            state "default", label:'Sun\nRise\n${currentValue}'
        }
        valueTile("sunset", "device.sunsetAPM", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Sun\nSet\n${currentValue}'
        }
        valueTile("moonrise", "device.moonriseAPM", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Moon\nRise\n${currentValue}'
        }
        valueTile("moonset", "device.moonsetAPM", inactiveLabel: false, width: 1, height: 1, decoration: "flat", wordWrap: true) {
            state "default", label:'Moon\nSet\n${currentValue}'
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
                    "lastSTupdate",
               ])
    }
}

def noOp() {}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
}

def installed() {
	state.meteoWeather = [:]
    state.darkSwyWeather = [:]
	initialize()
}

def uninstalled() {
	unschedule()
}

def updated() {
	log.info "Updated, settings: ${settings}"
    state.meteoWeatherVersion = getVersionLabel()
	unschedule(getMeteoWeather)
    unschedule(getDarkSkyWeather)
    unschedule(getPurpleAirAQI)
    unschedule(updateWundergroundTiles)
    initialize()
}

def initialize() {
	log.info 'Initializing...'
    
    // Create the template using the latest preferences values (components defined at the bottom)
    state.meteoTemplate = forecastTemplate + yesterdayTemplate + currentTemplate
    if (debug) send(name: 'meteoTemplate', value: state.meteoTemplate, displayed: false, isStateChange: true)
    
    def userpassascii = meteoUser + ':' + meteoPassword
	state.userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    
    // Schedule the updates
    def t = updateMins ?: '5'
    if (t == '1') {
    	runEvery1Minute(getMeteoWeather)
    } else {
    	"runEvery${t}Minutes"(getMeteoWeather)
       	runIn(5,getMeteoWeather)						// Have to wait to let the state changes settle
    }
    
    if (darkSkyKey != '') {
    	runEvery10Minutes(getDarkSkyWeather)			// Async Dark Sky current & forecast weather
        getDarkSkyWeather()
    } 
    // if ((fcstSource && (fcstSource == 'wunder')) || (darkSkyKey == '')) {
    state.wunderForTomorrow = (fcstSource && (fcstSource == 'meteo')) ? true : false
   	runEvery10Minutes(updateWundergroundTiles)		// This doesn't change all that frequently
    updateWundergroundTiles()
    // }
    if (purpleID) {
    	runEvery3Minutes(getPurpleAirAQI)				// Async Air Quality
    	// getPurpleAirAQI()
    }
}

def runEvery3Minutes(handler) {
	Random rand = new Random()
    int randomSeconds = rand.nextInt(59)
    log.info "AQI seconds: ${randomSeconds}"
	schedule("${randomSeconds} 0/3 * * * ?", handler)
}

// handle commands
def poll() { refresh() }
def refresh() { 
	getMeteoWeather()
    if (darkSkyKey != '') {
    	getDarkSkyWeather()
    }
    if ((darkSkyKey == '') || (fcstSource && (fcstSource == 'wunder'))) {
    	updateWundergroundTiles()
    }
    getPurpleAirAQI() 
}
def getWeatherReport() { return state.meteoWeather }
def configure() { updated() }

// Execute the hubAction request
def getMeteoWeather() {
    //log.trace "getMeteoWeather()"
    if (!state.meteoWeatherVersion || (state.meteoWeatherVersion != getVersionLabel())) {
    	// if the version level of the code changes, silently run updated() and initialize()
        log.trace "Version changed, updating..."
    	updated()
        return
    }
    // Create the hubAction request based on updated preferences
    def hubAction = new physicalgraph.device.HubAction(
            method: "GET",
            path: "/cgi-bin/template.cgi",
            headers: [ HOST: "${meteoIP}:${meteoPort}", 'Authorization': state.userpass ],
            query: ['template': "{\"timestamp\":${now()}," + state.meteoTemplate, 'contenttype': 'application/json;charset=utf-8' ],
            null,
            [callback: meteoWeatherCallback]
        )
    try {
        sendHubCommand(hubAction)
    } catch (Exception e) {
    	log.error "sendHubCommand Exception $e on $hubAction"
    }
}

// Handle the hubAction response
def meteoWeatherCallback(physicalgraph.device.HubResponse hubResponse) {
	log.info "meteoWeatherCallback() status: " + hubResponse.status
    //log.debug "meteoWeatherCallback() headers: " + hubResponse.headers
    if ((hubResponse.status == 200) && hubResponse.json) {
		state.meteoWeather = hubResponse.json
        //log.debug "meteoWeatherCallback() json: " + hubResponse.json
        if (debug) send(name: 'meteoWeather', value: hubResponse.json, displayed: false, isStateChange: true)
        updateWeatherTiles()
        return true
    } else {
    	log.error "meteoWeatherCallback() - Missing hubResponse.json (${hubResponse.status})"
        return false
    }
}

def getDarkSkyWeather() {
	//log.trace "getDarkSkyWeather()"
    if( darkSkyKey == "" )
    {
        log.error "DarkSky Secret Key not found.  Please configure in preferences."
        return false
    }
	String excludes = (fcstSource && (fcstSource == 'darksky')) ? 'sources,minutely,flags' : 'sources,minutely,daily,flags'
    String units = getTemperatureScale() == 'F' ? 'us' : (speed_units=='speed_kph' ? 'ca' : 'uk2')
    def apiRequest = [
        uri : "https://api.darksky.net",
        path : "/forecast/${darkSkyKey}/${location.latitude},${location.longitude}",
        query : [ exclude : excludes, units : units ],
        contentType : "application/json"
    ]
    asynchttp_v1.get( darkSkyCallback, apiRequest );
}

def darkSkyCallback(response, data) {
	log.info "darkSkyCallback() status: " + response.status
    def scale = getTemperatureScale()
    if( response.hasError() )
    {
        log.error "darkSkyCallback: ${response.getErrorMessage()}"
        return false
    }
   
    if( !response?.json )
    {
        log.error "darkSkyCallback: unable to retrieve data!"
        return false
    }
    
    //log.info "currently icon: ${darkSky?.currently?.icon}, summary: ${darkSky?.currently?.summary}"
    //log.info "hourly icon: ${darkSky?.hourly?.icon}, summary: ${darkSky?.hourly?.summary}"
    def darkSky = response.json
    // state.darkSkyWeather = response.json
    if (debug) send(name: 'darkSkyWeather', value: response.json, displayed: false, isStateChange: true)

	// current weather icon/state
    def icon = darkSky.currently.icon
    def isNight = (state.meteoWeather?.current?.isNight?.isNumber() && (state.meteoWeather.current.isNight.toInteger() == 1))
    if (isNight) {
    	switch(icon) {
        	case 'rain':
            	if ((darkSky.currently.summary == 'Light Rain') || (darkSky.currently.summary == 'Drizzle')) icon = 'lightrain-night'
                else if (darkSky.currently.summary == 'Heavy Rain') icon = 'heavyrain-night'
                else if (darkSky.currently.summary == 'Possible Light Rain') icon = 'chancelightrain-night'
                else if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancerain-night'
                break;
            case 'snow':
            	if ((darkSky.currently.summary == 'Light Snow') || (darkSky.currently.summary == 'Flurries')) icon = 'lightsnow-night'
                else if (darkSky.currently.summary == 'Possible Light Snow') icon = 'chancelightsnow-night'
                else if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancesnow-night'
                break;
            case 'sleet':
            	if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancesleet-night'
                break;
            case 'partly-cloudy':
            case 'partly-cloudy-night':
            	if (darkSky.currently.summary.contains('Mostly Cloudy')) icon = 'mostly-cloudy-night'
                if (darkSky.currently.summary.startsWith('Humid')) icon = 'humid-' + icon
                break;
            case 'thunderstorm':
            	if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancetstorms-night'
                break;
            case 'cloudy':
            case 'cloudy-night':
            	if (darkSky.currently.summary.startsWith('Humid')) icon = 'humid-' + icon
                break;
            case 'clear':
            case 'sunny':
            	icon = 'clear-night'
            case 'clear-night':
            	if (darkSky.currently.summary == 'Humid') icon = 'humid-night'
                break;
            case 'wind':
            case 'fog':
            case 'hail':
            case 'wind':
            case 'tornado':
            	icon = icon + '-night'		// adjust icons for night time that DarkSky doesn't
                break;    
        }
    } else { 
    	switch(icon) {
        	case 'rain':
            	if ((darkSky.currently.summary == 'Light Rain') || (darkSky.currently.summary == 'Drizzle')) icon = 'lightrain'
                else if (darkSky.currently.summary == 'Heavy Rain') icon = heavyrain
                else if (darkSky.currently.summary == 'Possible Light Rain') icon = 'chancelightrain'
                else if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancerain'
                break;
            case 'snow':
            	if ((darkSky.currently.summary == 'Light Snow') || (darkSky.currently.summary == 'Flurries')) icon = 'lightsnow'
                else if (darkSky.currently.summary == 'Possible Light Snow') icon = 'chancelightsnow'
                else if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancesnow'
                break;
            case 'sleet':
            	if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancesleet-night'
                break;
            case 'thunderstorm':
            	if (darkSky.currently.summary.startsWith('Possible')) icon = 'chancetstorms-night'
                break;
        	case 'partly-cloudy':
            case 'partly-cloudy-day':
            	if (darkSky.currently.summary.contains('Mostly Cloudy')) icon = 'mostly-cloudy'
                if (darkSky.currently.summary.startsWith('Humid')) icon = 'humid-' + icon
                break;
            case 'cloudy':
            case 'cloudy-day':
            	if (darkSky.currently.summary.startsWith('Humid')) icon = 'humid-' + icon
                break;
            case 'clear':
            case 'clear-day':
            	if (darkSky.currently.summary == 'Humid') icon = 'humid'
                break;
        }
    	
    }
    send(name: "weatherIcon", value: icon, descriptionText: 'Conditions: ' + darkSky.currently.summary)
    send(name: "weather", value: darkSky.currently.summary, displayed: false)
    
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
                	log.debug "Adding key ${darkSky.hourly.data[i].icon}"
                	summaryMap."${darkSky.hourly.data[i].icon}" = []
                }
                if (!summaryMap."${darkSky.hourly.data[i].icon}".contains(darkSky.hourly.data[i].summary)) {
                	log.debug "Adding value '${darkSky.hourly.data[i].summary}' to key ${darkSky.hourly.data[i].icon}"
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
            	log.debug summaryMap
            	state.summaryMap = summaryMap
            	send(name: 'summaryMap', value: summaryMap, isStateChange: true, displayed: false)
            }
        }
        
        send(name: 'forecast', value: forecast, descriptionText: "DarkSky Forecast: " + forecast)
    	send(name: "forecastCode", value: darkSky.hourly.icon, displayed: false)
        
        def pop = (darkSky.hourly?.data[0]?.precipProbability)
        if (pop?.isNumber()) {
        	pop = roundIt((darkSky.hourly.data[0].precipProbability.toBigDecimal() * 100), 0)	
        	send(name: "popDisplay", value: "PoP\nnext hr\n~${pop}%", descriptionText: "Probability of precipitation in the next hour is ${pop}%")
        	send(name: "pop", value: pop, unit: '%', displayed: false)
        } else {
        	send(name: "popDisplay", value: null, displayed: false)
            send(name: "pop", value: null, displayed: false)
        }
        
        def rtd = darkSky.daily?.data[0]?.precipIntensity
        if (rtd?.isNumber()) {
        	rtd = roundIt((darkSky.daily?.data[0]?.precipIntensity * 24.0), hd+1)
        	def rtdd = roundIt(rtd, hd)
        	send(name: "precipForecast", value: rtd, unit: h, descriptionText: "Forecasted precipitation today is ${rtd}${h}")
            send(name: "precipFcstDisplay", value: "${rtdd}${h}", displayed: false)
        } else {
        	send(name: "precipForecast", value: null, displayed: false)
            send(name: "precipFcstDisplay", value: null, displayed: false)
        }
        
        def hiTTda = roundIt(darkSky.daily?.data[0]?.temperatureHigh, 0)
        def loTTda = roundIt(darkSky.daily?.data[0]?.temperatureLow, 0)
        send(name: "highTempForecast", value: hiTTda, unit: scale, descriptionText: "Forecast high temperature today is ${hiTTda}°${scale}")
        send(name: "lowTempForecast", value: loTTda, unit: scale, descriptionText: "Forecast high temperature today is ${loTTda}°${scale}")

        if (darkSky.daily?.data[0]?.humidity?.isNumber()) {
        	def avHTda = roundIt((darkSky.daily.data[0].humidity * 100), 0)
        	send(name: "avgHumForecast", value: avHTda, unit: '%', descriptionText: "Forecast average humidity today is ${avHTda}%")
        } else {
        	send(name: "avgHumForecast", value: null, unit: '%', displayed: false)
        }
        
        if (darkSky.daily?.data[0]?.precipProbability?.isNumber()) {
        	def popTda = roundIt((darkSky.daily.data[0].precipProbability * 100), 0)
        	send(name: "popFcstDisplay", value: "PoP\nTDY\n~${popTda}%", descriptionText: "Probability of precipitation today is ${popTda}%")
        	send(name: "popForecast", value: popTda, unit: '%', displayed: false)
        } else {
        	send(name: "popFcstDisplay", value: null, displayed: false)
            send(name: "popForecast", value: null, displayed: false)
        }
        
        // Tomorrow's Forecast
        def hiTTom = roundIt(darkSky.daily?.data[1]?.temperatureHigh, 0)
        def loTTom = roundIt(darkSky.daily?.data[1]?.temperatureLow, 0)
        send(name: "highTempTomorrow", value: hiTTom, unit: scale, descriptionText: "Forecast high temperature tomorrow is ${hiTTom}°${scale}")
        send(name: "lowTempTomorrow", value: loTTom, unit: scale, descriptionText: "Forecast high temperature tomorrow is ${loTTom}°${scale}")

		if (darkSky.daily?.data[1]?.humidity?.isNumber()) {
        	def avHTom = roundIt((darkSky.daily.data[1].humidity * 100), 0)
        	send(name: "avgHumTomorrow", value: avHTom, unit: '%', descriptionText: "Forecast average humidity today is ${hiHTom}%")
        } else {
        	send(name: "avgHumTomorrow", value: null, unit: '%', displayed: false)
        }
      
      	if (darkSky.daily?.data[1]?.precipIntensity?.isNumber()) {
		    def rtom = roundIt((darkSky.daily.data[1].precipIntensity * 24.0), hd+1)
        	def rtomd = roundIt(rtom, hd)
            send(name: 'precipTomDisplay', value: "${rtomd}${h}", displayed: false)
            send(name: 'precipTomorrow', value: rtom, unit: h, descriptionText: "Forecast precipitation tomorrow is ${rtom}${h}")
        } else {
            send(name: 'precipTomDisplay', value:  null, displayed: false)
            send(name: 'precipTomorrow', value: null, displayed: false)
        }
        
        if (darkSky.daily?.data[1]?.precipProbability?.isNumber()) {
        	def popTom = roundIt((darkSky.daily.data[1].precipProbability * 100), 0)
            send(name: "popTomDisplay", value: "PoP\nTMW\n~${popTom}%", descriptionText: "Probability of precipitation tomorrow is ${popTom}%")
            send(name: "popTomorrow", value: popTom, unit: '%', displayed: false)
        } else {
            send(name: "popTomDisplay", value: null, displayed: false)
            send(name: "popTomorrow", value: null, displayed: false)
        }
    }
    return true
}

// This updates the tiles with Weather Underground data
def updateWundergroundTiles() {
	log.trace "updateWundergroundTiles()"
    def features = ''
    if (darkSkyKey == '') {
    	features = 'conditions'
        if (state.wunderForTomorrow || (fcstSource && (fcstSource == 'wunder'))) {
        	features += '/forecast'
        }
    } else if (state.wunderForTomorrow || (fcstSource && (fcstSource == 'wunder'))) {
    	features = 'forecast'
    }
    if (features == '') return
    log.info "updateWundergroundTiles()"
    
    if (fcstSource && (fcstSource == 'wunder')) state.wunderForTomorrow = false
    if (debug) log.debug 'Fetures: ' + features
    
    def obs = get(features)		//	?.current_observation
    if (debug) send(name: 'wundergroundObs', value: obs, displayed: false)
    
    if ((obs != [:]) && features.contains('conditions')) {
    	if (obs.current_observation) {
            def weatherIcon 
            if (state.meteoWeather?.current?.isNight?.isNumber()) {
                weatherIcon = (state.meteoWeather.current.isNight.toInteger() == 1) ? 'nt_' + obs.current_observation.icon : obs.current_observation.icon
            } else {
                weatherIcon = obs.icon_url.split("/")[-1].split("\\.")[0]
            }
            send(name: "weather", value: obs.current_observation.weather, descriptionText: 'Conditions: ' + obs.current_observation.weather)
            send(name: "weatherIcon", value: weatherIcon, displayed: false)
        }
	}
    if (obs && features.contains('forecast')) {
    	// obs = get("forecast") // ?.forecast?.txt_forecast?.forecastday
        if (obs.forecast) {
        	def scale = getTemperatureScale()
            String h = (height_units && (height_units == 'height_in')) ? '"' : 'mm'
        	int hd = (h == '"') ? 2 : 1		// digits to store & display
            
            if (!state.wunderForTomorrow) {
            	// Hre we are NOT using Meteobridge's Davis weather forecast text/codes
            	def forecast = scale=='F' ? obs.forecast.txt_forecast?.forecastday[0]?.fcttext : obs.forecast.txt_forecast?.forecastday[0]?.fcttext_metric
            	send(name: 'forecast', value: forecast, descriptionText: "Weather Underground Forecast: " + forecast)
            	send(name: "forecastCode", value: obs.forecast.txt_forecast?.forecastday[0]?.icon, displayed: false)
            }
            
            def when = obs.forecast.txt_forecast?.forecastday[0]?.title?.contains('Night') ? 'TNT' : 'TDY'
            def pop = obs.forecast.txt_forecast?.forecastday[0].pop
            if (pop.isNumber()) {
            	send(name: "popDisplay", value: "PoP\n${when}\n~${pop}%", descriptionText: "Probability of precipitation ${when} is ${pop}%")
            	send(name: "pop", value: pop, unit: '%', displayed: false)
            } else {
            	send(name: "popDisplay", value: null, displayed: false)
                send(name: "pop", value: null, displayed: false)
            }
            
            def hiTTdy = scale=='F' ? obs.forecast.simpleforecast?.forecastday[0]?.high?.fahrenheit : obs.forecast.simpleforecast?.forecastday[0]?.high?.celsius
            def loTTdy = scale=='F' ? obs.forecast.simpleforecast?.forecastday[0]?.low?.fahrenheit : obs.forecast.simpleforecast?.forecastday[0]?.low?.celsius
            def avHTdy = obs.forecast.simpleforecast?.forecastday[0]?.avehumidity
            def popTdy = obs.forecast.simpleforecast?.forecastday[0]?.pop
            send(name: "highTempForecast", value: hiTTdy, unit: scale, descriptionText: "Forecast high temperature today is ${hiTTdy}°${scale}")
            send(name: "lowTempForecast", value: loTTdy, unit: scale, descriptionText: "Forecast high temperature today is ${loTTdy}°${scale}")
            send(name: "avgHumForecast", value: avHTdy, unit: '%', descriptionText: "Forecast average humidity today is ${hiHTdy}%")
            
            
            def rtd = (h=='"') ? roundIt(obs.forecast.simpleforecast.forecastday[0]?.qpf_allday?.in, hd+1) : roundIt(obs.forecast.simpleforecast.forecastday[0]?.qpf_allday?.mm, hd+1)
            def rtdd = roundIt(rtd, hd)
			if (rtdd != null) {
            	send(name: 'precipFcstDisplay', value:  "${rtdd}${h}", displayed: false)
            	send(name: 'precipForecast', value: rtd, unit: h, descriptionText: "Forecast precipitation today is ${rtd}${h}")
            } else {
            	send(name: 'precipTomDisplay', value:  null, displayed: false)
                send(name: 'precipTomorrow', value: null, displayed: false)
            }
            if (popTdy != null) {
            	send(name: "popFcstDisplay", value: "PoP\nTDY\n~${popTdy}%", descriptionText: "Probability of precipitation today is ${popTdy}%")
            	send(name: "popForecast", value: popTdy, unit: '%', displayed: false)
            } else {
            	send(name: "popFcstDisplay", value: null, displayed: false)
                send(name: "popForecast", value: null, displayed: false)
            }
            
            def hiTTom = scale=='F' ? obs.forecast.simpleforecast?.forecastday[1]?.high?.fahrenheit : obs.forecast.simpleforecast?.forecastday[1]?.high?.celsius
            def loTTom = scale=='F' ? obs.forecast.simpleforecast?.forecastday[1]?.low?.fahrenheit : obs.forecast.simpleforecast?.forecastday[1]?.low?.celsius
            def avHTom = obs.forecast.simpleforecast?.forecastday[1]?.avehumidity
            def popTom = obs.forecast.simpleforecast?.forecastday[1]?.pop
            send(name: "highTempTomorrow", value: hiTTom, unit: scale, descriptionText: "Forecast high temperature tomorrow is ${hiTTom}°${scale}")
            send(name: "lowTempTomorrow", value: loTTom, unit: scale, descriptionText: "Forecast high temperature tomorrow is ${loTTom}°${scale}")
            send(name: "avgHumTomorrow", value: avHTom, unit: '%', descriptionText: "Forecast average humidity tomorrow is ${hiHTom}%")
            
            def rtom = (h=='"') ? roundIt(obs.forecast.simpleforecast.forecastday[1]?.qpf_allday?.in, hd+1) : roundIt(obs.forecast.simpleforecast.forecastday[1]?.qpf_allday?.mm, hd+1)
            def rtomd = roundIt(rtom, hd)
			if (rtom != null) {
            	send(name: 'precipTomDisplay', value:  "${rtomd}${h}", displayed: false)
            	send(name: 'precipTomorrow', value: rtom, unit: "${h}", descriptionText: "Forecast precipitation tomorrow is ${rtd}${h}")
            } else {
            	send(name: 'precipTomDisplay', value:  null, displayed: false)
                send(name: 'precipTomorrow', value: null, displayed: false)
            }
            if (popTom != null) {
            	send(name: "popTomDisplay", value: "PoP\nTMW\n~${popTom}%", descriptionText: "Probability of precipitation tomorrow is ${popTom}%")
            	send(name: "popTomorrow", value: popTom, unit: '%', displayed: false)
            } else {
            	send(name: "popTomDisplay", value: null, displayed: false)
                send(name: "popTomorrow", value: null, displayed: false)
            }
        }
    }
}

// This updates the tiles with Meteobridge data
def updateWeatherTiles() {
    if (state.meteoWeather != [:]) {
        // log.debug "meteoWeather: ${state.meteoWeather}"
		String unit = getTemperatureScale()
        String h = (height_units && (height_units == 'height_in')) ? '"' : 'mm'
        int hd = (h = '"') ? 2 : 1		// digits to store & display
        int ud = unit=='F' ? 0 : 1
        
    // Yesterday data
        if (state.meteoWeather.yesterday) {
        	def t = roundIt(state.meteoWeather.yesterday.highTemp, ud)
        	send(name: 'highTempYesterday', value: t, unit: unit, descriptionText: "High Temperature yesterday was ${t}°${unit}")
            t = roundIt(state.meteoWeather.yesterday.lowTemp, ud)
            send(name: 'lowTempYesterday', value: t, unit: unit, descriptionText: "Low Temperature yesterday was ${t}°${unit}")
            def hum = roundIt(state.meteoWeather.yesterday.highHum, 0)
            send(name: 'highHumYesterday', value: hum, unit: "%", descriptionText: "High Humidity yesterday was ${hum}%")
            hum = roundIt(state.meteoWeather.yesterday.lowHum, 0)
            send(name: 'lowHumYesterday', value: hum, unit: "%", descriptionText: "Low Humidity yesterday was ${hum}%")
            def ryd = roundIt(state.meteoWeather.yesterday.rainfall, hd + 1)		// Internally keep 1 more digit of precision than we display
            def rydd = roundIt(state.meteoWeather.yesterday.rainfall, hd)
            if (ryd != '--') {
				send(name: 'precipYesterday', value: ryd, unit: "${h}", descriptionText: "Precipitation yesterday was ${ryd}${h}")
                send(name: 'precipYesterdayDisplay', value: "${rydd}${h}", displayed: false)
            } else send(name: 'precipYesterdayDisplay', value: ryd, displayed: false)
        }

    // Today data
		if (state.meteoWeather.current != [:]) { 
        	if (state.meteoWeather.current.isDay?.isNumber() && (state.meteoWeather.current.isDay.toInteger() != device.currentValue('isDay')?.toInteger())) {
            	updateWundergroundTiles()
                if (state.meteoWeather.current.isDay == 1) {
                	send(name: 'isDay', value: 1, displayed: true, descriptionText: 'Daybreak' )
                	send(name: 'isNight', value: 0, displayed: false)
                } else {
					send(name: 'isDay', value: 0, displayed: true, descriptionText: 'Nightfall')
                    send(name: 'isNight', value: 1, displayed: false)
                }
            }
            
            // Temperatures
            def td = roundIt(state.meteoWeather.current.temperature, 2)
			send(name: "temperature", value: td, unit: unit, descriptionText: "Temperature is ${td}°${unit}")
            td = roundIt(state.meteoWeather.current.temperature, 1)
            send(name: "temperatureDisplay", value: td.toString(), unit: unit, displayed: false, descriptionText: "Temperature is ${td}°${unit}")
            def t = roundIt(state.meteoWeather.current.highTemp, ud)
            send(name: "highTemp", value: t, unit: unit, descriptionText: "High Temperature so far today is ${t}°${unit}")
            t = roundIt(state.meteoWeather.current.lowTemp, ud)
            send(name: "lowTemp", value: t , unit: unit, descriptionText: "Low Temperature so far today is ${t}°${unit}")
            t = roundIt(state.meteoWeather.current.heatIndex, ud+1)
            if (state.meteoWeather.current.temperature != state.meteoWeather.current.heatIndex) {
            	send(name: "heatIndex", value: t , unit: unit, displayed: false)
                send(name: "heatIndexDisplay", value: t + '°', unit: unit, descriptionText: "Heat Index is ${t}°${unit}")
            } else {
            	send(name: 'heatIndex', value: t, unit: unit, descriptionText: "Heat Index is ${t}°${unit} - same as current temperature")
                send(name: 'heatIndexDisplay', value: '=', displayed: false)
            }
			t = roundIt(state.meteoWeather.current.dewpoint, ud+1)
            send(name: "dewpoint", value: t , unit: unit, descriptionText: "Dew Point is ${t}°${unit}")
            t = roundIt(state.meteoWeather.current.windChill, ud+1)
            if (isStateChange( device, 'windChill', t as String)) {
            	if (t) {
                    if (state.meteoWeather.current.temperature != state.meteoWeather.current.windChill) {			
                        send(name: "windChill", value: t, unit: unit, displayed: false, isStateChange: true)
                        send(name: "windChillDisplay", value: t + '°', unit: unit, descriptionText: "Wind Chill is ${t}°${unit}", isStateChange: true)
                    } else {
                        send(name: 'windChill', value: t, unit: unit, descriptionText: "Wind Chill is ${t}°${unit} - same as current temperature", isStateChange: true)
                        send(name: 'windChillDisplay', value: '=', displayed: false, isStateChange: true)
                    }
                } else {
                    // if the Meteobridge weather station doesn't have an anemometer, we won't get a wind chill value
                    send(name: 'windChill', value: null, displayed: false, isStateChange: true)
                    send(name: 'windChillDisplay', value: null, displayed: false, isStateChange: true)
                }
            }
            
            // Humidity
            def hum = roundIt(state.meteoWeather.current.humidity, 0)
            if (isStateChange(device, 'humidity', hum as String)) {
				send(name: "humidity", value: hum, unit: "%", descriptionText: "Humidity is ${hum}%", isStateChange: true)
            	hum = roundIt(state.meteoWeather.current.highHum, 0)
            	send(name: "highHumidity", value: hum, unit: "%", descriptionText: "High Humidity so far today is ${hum}%")
            	hum = roundIt(state.meteoWeather.current.lowHum, 0)
            	send(name: "lowHumidity", value: hum, unit: "%", descriptionText: "Low Humidity so far today is ${hum}%")
            }
            // Ultraviolet Index
            if (state.meteoWeather.current.uvIndex != null) {
            	def uv = roundIt(state.meteoWeather.current.uvIndex, 1)		// UVindex can be null
                if (isStateChange(device, 'uvIndex', uv as String)) {
            		send(name: "uvIndex", value: uv, unit: 'uvi', descriptionText: "UV Index is ${uv}", displayed: false, isStateChange: true)
            		send(name: 'ultravioletIndex', value: uv, unit: 'uvi', isStateChange: true)
                }
            } else {
            	send(name: "uvIndex", value: null, unit: 'uvi', displayed: false)
            	send(name: 'ultravioletIndex', value: null, unit: 'uvi', displayed: false)
            }           
           
           	// Solar Radiation
            if (state.meteoWeather.current.solarRadiation != null) {
            	def val = roundIt(state.meteoWeather.current.solarRadiation, 0)
				send(name: "solarRadiation", value: val, unit: 'W/m²', descriptionText: "Solar radiation is ${val} W/m²")
            } else {
            	send(name: "solarRadiation", value: null, displayed: false)
            }
        
			// Barometric Pressure   
            def pr = roundIt(state.meteoWeather.current.pressure, 2)
            if (pr && isStateChange(device, 'pressure', pr as String)) {
                def pressure_trend_text
                switch (state.meteoWeather.current.pressureTrend) {
                    case "FF" :
                        pressure_trend_text = "➘ Fast"
                        break;
                    case "FS":
                        pressure_trend_text = "➘ Slow"
                        break;
                    case "ST":
                        pressure_trend_text = "Steady"
                        break;
                    case "N/A":
                        pressure_trend_text = '➙'
                        break;
                    case "RS":
                        pressure_trend_text = "➚ Slow"
                        break;
                    case "RF":
                        pressure_trend_text = "➚ Fast"
                        break;
                    default:
                        pressure_trend_text = ""
                }
                def pv = (pres_units && (pres_units == 'press_in')) ? 'inHg' : 'mmHg'

                send(name: 'pressure', value: pr, unit: pv, displayed: false, descriptionText: "Barometric Pressure is ${pr} ${pv}", isStateChange: true)
                send(name: 'pressureDisplay', value: "${pr}\n${pv}\n${pressure_trend_text}", descriptionText: "Barometric Pressure is ${pr} ${pv} - ${pressure_trend_text}", isStateChange: true)
                send(name: 'pressureTrend', value: pressure_trend_text, displayed: false, descriptionText: "Barometric Pressure trend is ${pressure_trend_text}")
       		}
            
			// Rainfall
        	def rlh = roundIt(state.meteoWeather.current.rainLastHour, hd+1)
            def rlhd = roundIt(state.meteoWeather.current.rainLastHour, hd)
            if (rlh != null) {
				send(name: 'precipLastHourDisplay', value: "${rlhd}${h}", displayed: false)
            	send(name: 'precipLastHour', value: rlh, unit: "${h}", descriptionText: "Precipitation in the Last Hour was ${rlh}${h}")
            } else {
            	send(name: 'precipLastHourDisplay', value: '0.00', displayed: false)
            }
            def rtd = roundIt(state.meteoWeather.current.rainfall, hd+1)
            def rtdd = roundIt(state.meteoWeather.current.rainfall, hd)
			if (rtd != null) {
            	send(name: 'precipTodayDisplay', value:  "${rtdd}${h}", displayed: false)
            	send(name: 'precipToday', value: rtd, unit: "${h}", descriptionText: "Precipitation so far today is ${rtd}${h}")
            } else {
            	send(name: 'precipTodayDisplay', value:  '0.00', displayed: false)
            }
            def rrt = roundIt(state.meteoWeather.current.rainRate, hd)
            if (rrt != null) {
            	send(name: 'precipRateDisplay', value:  "${rrt}${h}", displayed: false)
            	send(name: 'precipRate', value: rrt, unit: "${h}/hr", descriptionText: "Precipitation rate is ${rrt}${h}/hour")
            } else {
            	send(name: 'precipRateDisplay', value:  '0.00', displayed: false)
            }
            
			// Wet/dry indicator - wet if there has been measurable rainfall within the last hour...
            if (state.meteoWeather.current.rainLastHour?.isNumber() && (state.meteoWeather.current.rainLastHour.toBigDecimal() > 0.0)) {
				sendEvent( name: 'water', value: "wet" )
			} else {
				sendEvent( name: 'water', value: "dry" )
			}
            
            // Evapotranspiration
            def et = roundIt(state.meteoWeather.current.evapotranspiration, hd+1)
            if (et != null) {
            	send(name: "evapotranspiration", value: et, unit: "${h}", descriptionText: "Evapotranspiration rate is ${et}${h}/hour")
                send(name: "etDisplay", value: "${roundIt(et,hd)}${h}", displayed: false)
            } else {
            	send(name: "etDisplay", value: null, displayed: false)
            }

			// Wind 
			String s = (speed_units && (speed_units == 'speed_mph')) ? 'mph' : 'kph'
            if (state.meteoWeather.current.windSpeed != null) {
            	def ws = roundIt(state.meteoWeather.current.windSpeed,0)
           		def wg = roundIt(state.meteoWeather.current.windGust,0)
            	def winfo = "Wind ${ws} ${s}\nfrom ${state.meteoWeather.current.windDirText} (${state.meteoWeather.current.windDegrees.toInteger()}°)\ngusts to ${wg} ${s}"
                def winfoDesc = "Winds are ${ws} ${s} from the ${state.meteoWeather.current.windDirText} (${state.meteoWeather.current.windDegrees.toInteger()}°), gusting to ${wg} ${s}"
				send(name: "windinfo", value: winfo, displayed: true, descriptionText: winfoDesc)
				send(name: "windGust", value: wg, unit: "${s}", displayed: false, descriptionText: "Winds gusting to ${wg} ${s}")
				send(name: "windDirection", value: "${state.meteoWeather.current.windDirText}", displayed: false, descriptionText: "Winds from the ${state.meteoWeather.current.windDirText}")
				send(name: "windDirectionDegrees", value: "${state.meteoWeather.current.windDegrees.toInteger()}", unit: '°', displayed: false, descriptionText: "Winds from ${state.meteoWeather.current.windDegrees.toInteger()}°")
				send(name: "wind", value: ws, unit: "${s}", displayed: false, descriptionText: "Wind speed is ${ws} ${s}")
            } else {
            	def isChange = isStateChange( device, 'wind', null)
                if (isChange) {
                    send(name: "windinfo", value: null, displayed: false, isStateChange: true)
                    send(name: "windGust", value: null, displayed: false, isStateChange: true)
                    send(name: "windDirection", value: null, displayed: false, isStateChange: true)
                    send(name: "windDirectionDegrees", value: null, displayed: false, isStateChange: true)
                    send(name: "wind", value: null, displayed: false, isStateChange: true)
                }
            }

			if (location.name != device.currentValue("locationName")) {
				send(name: "locationName", value: location.name, isStateChange: true, descriptionText: "Location is ${loc}")
			}

			// Date stuff
        	if ( ((state.meteoWeather.current.date != "") && (state.meteoWeather.current.date != device.currentValue('currentDate'))) ||
            		((state.meteoWeather.current.sunrise != "") && (state.meteoWeather.current.sunrise != device.currentValue('sunrise'))) ||
                    ((state.meteoWeather.current.sunset != "") && (state.meteoWeather.current.sunset != device.currentValue('sunset'))) ||
                    ((state.meteoWeather.current.dayHours != "") && (state.meteoWeather.current.dayHours != device.currentValue('dayHours'))) ||
                    ( /*(state.meteoWeather.current.moonrise != "") && */ (state.meteoWeather.current.moonrise != device.currentValue('moonrise'))) || // sometimes there is no moonrise/set
                    ( /*(state.meteoWeather.current.moonset != "") &&  */ (state.meteoWeather.current.moonset != device.currentValue('moonset'))) ) {
            	// If any Date/Time has changed, time to update them all
                
            	// Sunrise / sunset
                if (state.meteoWeather.current.sunrise != "") updateMeteoTime(state.meteoWeather.current.sunrise, 'sunrise') else clearMeteoTime('sunrise')
                if (state.meteoWeather.current.sunset != "")  updateMeteoTime(state.meteoWeather.current.sunset, 'sunset') else clearMeteoTime('sunset')
                if (state.meteoWeather.current.dayHours != "") {
                	send(name: "dayHours", value: state.meteoWeather.current.dayHours, descriptionText: state.meteoWeather.current.dayHours + ' of daylight today')
                } else {
                	send(name: 'dayHours', value: "", displayed: false)
                }
                if (state.meteoWeather.current.dayMinutes?.isNumber()) {
                	send(name: "dayMinutes", value: state.meteoWeather.current.dayMinutes, displayed: true, descriptionText: state.meteoWeather.current.dayMinutes +' minutes of daylight today')
                }

            	// Moonrise / moonset
                if (state.meteoWeather.current.moonrise != "") updateMeteoTime(state.meteoWeather.current.moonrise, 'moonrise') else clearMeteoTime('moonrise')
                if (state.meteoWeather.current.moonset != "")  updateMeteoTime(state.meteoWeather.current.moonset, 'moonset') else clearMeteoTime('moonset')
                
                // update the date
                if (state.meteoWeather.current.date != "") {
                	send(name: 'currentDate', value: state.meteoWeather.current.date, displayed: false)
                } else {
                	send(name: 'currentDate', value: "", displayed: false)
                }
			}
            
         	// Lux estimator - get every time, even if we aren't using Meteobridge data to calculate the lux
            def lux = estimateLux()
			send(name: "illuminance", value: lux, unit: 'lux', descriptionText: "Illumination is ${lux} lux (est)")
            
    		// Forecast
        	if (!fcstSource || (fcstSource == 'meteo')) {
            	if (state.meteoWeather.forecast?.text != null) {
                    send(name: 'forecast', value: state.meteoWeather.forecast?.text, descriptionText: "Davis Forecast: " + state.meteoWeather.forecast?.text)
                    send(name: "forecastCode", value: state.meteoWeather.forecast?.code, descriptionText: "Davis Forecast Rule #${state.meteoWeather.forecast?.code}")
                } else {
                	// If the Meteobridge isn't providing a forecast (only provided for SOME Davis weather stations), use the one from WunderGround
                	state.wunderForTomorrow = true
                }
            }
            
        	// Lunar Phases
        	String xn = 'x'				// For waxing/waning below
            String phase = null
            def l = state.meteoWeather.current.lunarAge
        	if (state.meteoWeather.current.lunarSegment?.isNumber()) {
            	switch (state.meteoWeather.current.lunarSegment.toInteger()) {
                	case 0: 
                    	phase = 'New'
						xn = (l >= 27) ? 'n' : 'x'
                        break;
                    case 1:
                    	phase = 'Waxing Crescent'
                        break;
                    case 2:
                    	phase = 'First Quarter'
                        break;
                    case 3:
                    	phase = 'Waxing Gibbous'
                        break;
                    case 4:
                    	phase = 'Full'
                        xn = (l <= 14) ? 'x' : 'n'
                        break;
                    case 5:
                    	phase = 'Waning Gibbous'
                        xn = 'n'
                        break;
                    case 6:
                    	phase = 'Third Quarter'
                        xn = 'n'
                        break;
                    case 7:
                    	phase = 'Waning Crescent'
                        xn = 'n'
                        break;
                }
            	send(name: 'moonPhase', value: phase, descriptionText: 'The Moon\'s phase is ' + phase)
                send(name: 'lunarSegment', value: state.meteoWeather.current.lunarSegment, displayed: false)
                send(name: 'lunarAge', value: l, unit: 'days', displayed: false, descriptionText: "The Moon is ${l} days old" )          
            }
            if (state.meteoWeather.current.lunarPercent?.isNumber()) {
            	def lpct = roundIt(state.meteoWeather.current.lunarPercent, 0)
            	send(name: 'lunarPercent', value: lpct, displayed: true, unit: '%', descriptionText: "The Moon is ${lpct}% lit")
                String pcnt = sprintf('%03d', (roundIt((state.meteoWeather.current.lunarPercent / 5.0),0) * 5).toInteger())
                String pname = 'Moon-wa' + xn + 'ing-' + pcnt
                //log.debug "Lunar Percent by 5s: ${pcnt} - ${pname}"
                send(name: 'moonPercent', value: pcnt, displayed: false, unit: '%')
                send(name: 'moonDisplay', value: pname, displayed: false)
                if (state.meteoWeather.current.lunarAge.isNumber()) {
                	String sign = (xn == 'x') ? '+' : '-'
                    if ((phase == 'New') || (phase == 'Full')) sign = ''
                    String dir = (sign != '') ? "Wa${xn}ing" : phase
                	String info = "${dir}\n${lpct}%\nDay ${state.meteoWeather.current.lunarAge}"
                    send(name: 'moonInfo', value: info, displayed: false)
                }
            }
        }
        
        // update the timestamps last, after all the values have been updated
        def now = new Date(state.meteoWeather.timestamp).format("h:mm:ss a '\non' M/d/yyyy",location.timeZone).toLowerCase()
        sendEvent(name:"lastSTupdate", value: now, displayed: false)
        sendEvent(name:"timestamp", value: state.meteoWeather.timestamp, displayed: false)
	}
}
private updateMeteoTime(timeStr, stateName) {
	def t = timeToday(timeStr, location.timeZone).getTime()
	def tAPM = new Date(t).format('h:mm a', location.timeZone).toLowerCase()
   	send(name: stateName, value: timeStr, displayed: false)
    send(name: stateName + 'APM', value: tAPM, descriptionText: stateName.capitalize() + ' at ' + tAPM)
    send(name: stateName + 'Epoch', value: t, displayed: false)
}
private clearMeteoTime(stateName) {
	send(name: stateName, value: "", displayed: false)
    send(name: stateName + 'APM', value: "", descriptionText: 'No ' + stateName + 'today')
    send(name: stateName + 'Epoch', value: "", displayed: false)
}
private get(feature) {
    getWeatherFeature(feature, zipCode)
}
private localDate(timeZone) {
    def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
    df.setTimeZone(TimeZone.getTimeZone(timeZone))
    df.format(new Date())
}
private send(map) {
    sendEvent(map)
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
private estimateLux() {
	// If we have it, use solarRadiation as a proxy for Lux 
	if (state.meteoWeather?.current?.solarRadiation?.isNumber()){
    	def lux
    	switch (settings.lux_scale) {
        	case 'std':
            	// 0-10,000 - SmartThings Weather Tile scale
                lux = (state.meteoWeather.current.isNight > 0) ? 10 : roundIt(((state.meteoWeather.current.solarRadiation / 0.225) * 10.0), 0)	// Hack to approximate SmartThings Weather Station
        		return (lux < 10) ? 10 : ((lux > 10000) ? 10000 : lux)
    			break;
                
        	case 'real':
            	// 0-100,000 - realistic estimated conversion from SolarRadiation
                lux = (state.meteoWeather.current.isNight > 0) ? 10 : roundIt((state.meteoWeather.current.solarRadiation / 0.0079), 0)		// Hack approximation of Davis w/m^2 to lx
                return (lux< 10) ? 10 : ((lux > 100000) ? 100000 : lux)
                break;
                
            case 'default':
            default:
            	lux = (state.meteoWeather.current.isNight > 0) ? 10 : roundIt((state.meteoWeather.current.solarRadiation / 0.225), 0)	// Hack to approximate Aeon multi-sensor values
        		return (lux < 10) ? 10 : ((lux > 1000) ? 1000 : lux)
                break;
        }
    }
    // handle other approximations here
    def lux = 10
    def now = new Date().time
    if (state.meteoWeather.current.isDay > 0) {
        //day
        if (darkSkyKey != '') {
        	// Dark Sky: Use Cloud Cover
            def cloudCover = (state.darkSkyWeather?.currently?.cloudCover?.isNumber()) ?: 0.0
            lux = roundIt(1000.0 - (1000.0 * cloudCover), 0)
            if (lux == 0) {
            	if (state.darkSkyWeather?.currently?.uvIndex?.isNumber()) {
                	lux = (state.darkSkyWeather.currently.uvIndex > 0) ? 100 : 50	// hack - it's never totally dark during the day
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
        def oneHour = 1000 * 60 * 60

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
	//log.trace "getPurpleAirAQI()"
    if (!settings.purpleID) {
    	send(name: 'airQualityIndex', value: null, displayed: false)
        send(name: 'aqi', value: null, displayed: false)
        return
    }
    def params = [
        uri: 'https://www.purpleair.com',
        path: '/json',
        query: [show: settings.purpleID]
        // body: ''
    ]
    asynchttp_v1.get(purpleAirResponse, params)
}

def purpleAirResponse(resp, data) {
	log.info "purpleAirResponse() status: " + resp?.status 
	if (resp?.status == 200) {
		try {
			if (!resp.json) {
            	// FAIL - no data
                log.warn "purpleAirResponse() no JSON: ${resp.data}"
                return false
            }
		} catch (Exception e) {
			log.error "purpleAirResponse() - General Exception: ${e}"
        	throw e
            return false
        }
    } else {
    	return false
    }
    
    def purpleAir = resp.json
	// good data, do the calculations
    if (debug) send(name: 'purpleAir', value: resp.json, displayed: false)
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
    	send(name: 'airQualityIndex', value: roundIt(aqi, 0), displayed: false)
        if (aqi < 1.0) aqi = roundIt(aqi, 0)
        //log.info "AQI: ${aqi}"
    	send(name: 'aqi', value: aqi, displayed: false)
    }
    return
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
String getMeteoSensorID() {
    def version = state.meteoWeather?.version?.isNumber() ? state.meteoWeather.version : 1.0
    return ( version > 3.6 ) ? '*' : '0'
}
def getForecastTemplate() {
	return '"forecast":{"text":"[forecast-text:]","code":[forecast-rule]},"version":[mbsystem-swversion:1.0],'
}
def getYesterdayTemplate() {
	String s = getTemperatureScale() 
	String d = getMeteoSensorID() 
    return "\"yesterday\":{\"highTemp\":[th${d}temp-ydmax=${s}.2:null],\"lowTemp\":[th${d}temp-ydmin=${s}.2:null],\"highHum\":[th${d}hum-ydmax=.2:null],\"lowHum\":[th${d}hum-ydmin=.2:null]," + yesterdayRainfall + '},'
}
def getCurrentTemplate() {
	String d = getMeteoSensorID()
	return "\"current\":{\"date\":\"[MM]/[DD]/[YYYY]\",\"humidity\":[th${d}hum-act=.2:null],\"indoorHum\":[thb${d}hum-act=.2:null]," + temperatureTemplate + currentRainfall + pressureTemplate + windTemplate +
			"\"pressureTrend\":\"[thb${d}seapress-delta1=enbarotrend:N/A]\",\"dayHours\":\"[mbsystem-daylength:]\",\"highHum\":[th${d}hum-dmax=.2:null],\"lowHum\":[th${d}hum-dmin=.2:null]," +
			"\"sunrise\":\"[mbsystem-sunrise:]\",\"sunset\":\"[mbsystem-sunset:]\",\"dayMinutes\":[mbsystem-daylength=mins.0:null],\"uvIndex\":[uv${d}index-act:null]," +
            "\"solarRadiation\":[sol${d}rad-act:null],\"lunarAge\":[mbsystem-lunarage:],\"lunarPercent\":[mbsystem-lunarpercent:],\"lunarSegment\":[mbsystem-lunarsegment:null]," +
            '"moonrise":"[mbsystem-moonrise:]","moonset":"[mbsystem-moonset:]","isDay":[mbsystem-isday=.0],"isNight":[mbsystem-isnight=.0]}}'
}
def getTemperatureTemplate() { 
	String s = getTemperatureScale() 
    String d = getMeteoSensorID() 
	return "\"temperature\":[th${d}temp-act=${s}.2:null],\"dewpoint\":[th${d}dew-act=${s}.2:null],\"heatIndex\":[th${d}heatindex-act=${s}.2:null],\"windChill\":[wind${d}chill-act=${s}.2:null]," +
    		"\"indoorTemp\":[thb${d}temp-act=${s}.2:null],\"indoorDew\":[thb${d}dew-act=${s}.2:null],\"highTemp\":[th${d}temp-dmax=${s}.2:null],\"lowTemp\":[th${d}temp-dmin=${s}.2:null],"
}
def getPressureTemplate() {
	String p = (pres_units && (pres_units == 'press_in')) ? 'inHg' : 'mmHg'
    String d = getMeteoSensorID() 
	return "\"pressure\":[thb${d}seapress-act=${p}.2:null],"
}
def getYesterdayRainfall() {
	String r = (height_units && (height_units == 'height_in')) ? 'in' : ''
    String d = getMeteoSensorID()
	return "\"rainfall\":[rain${d}total-ydaysum=${r}.3:null]," 
}
def getCurrentRainfall() {
	String r = (height_units && (height_units == 'height_in')) ? 'in' : ''
    String d = getMeteoSensorID()
	return "\"rainfall\":[rain${d}total-daysum=${r}.3:null],\"rainLastHour\":[rain${d}total-sum1h=${r}.3:null],\"evapotranspiration\":[sol${d}evo-act=${r}.3:null],\"rainRate\":[rain${d}rate-act=${r}.3:null],"
}
def getWindTemplate() {
    String s = (speed_units && (speed_units == 'speed_mph')) ? 'mph' : 'kmh'
    String d = getMeteoSensorID()
	return "\"windGust\":[wind${d}wind-max10=${s}.2:null],\"windAvg\":[wind${d}wind-act=${s}.2:null],\"windDegrees\":[wind${d}dir-act:null],\"windSpeed\":[wind${d}wind-act=${s}.2:null],\"windDirText\":\"[wind${d}dir-act=endir:null]\","
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
