# MeteoWeather
SmartThings Weather Station using Meteobridge Local Connection (via hubAction)

Provides pretty much all information exposed by MeteoBridge's Templates HTML API. Optimized for Davis Vantage Pro2 stations, but will report whatever subset Meteobridge provides for any connected weather station.

**Note:** since this uses `hubAction()`, it will work only when your hub and Meteobridge are on the same (local) IP network. This approach keeps your password safe(r), but doesn't handle remote IP addresses. *If you try to convert to external IP, you will have to accomodate differences between the Timezone of your Hub and the Meteobridge.*
