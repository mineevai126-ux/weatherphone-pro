# WeatherPhone Pro — Accuracy Roadmap

Goal: make WeatherPhone Pro more accurate than a basic weather informer.

## Core principle

The app must not blindly show one provider response. It should collect weather data from several sources, compare them, estimate confidence, and explain the forecast.

## Priority 1: precise location

- Add device location by GPS coordinates.
- Keep city search as fallback.
- Show forecast point: latitude, longitude, and place name.
- Prefer exact coordinates over city center.

## Priority 2: multiple weather sources

Use several providers/models and normalize their data into one internal format.

Planned sources:

1. Open-Meteo as the base free source.
2. Open-Meteo model variants where available: GFS, ICON, ECMWF-like models, regional models.
3. MET Norway / Yr style source if API access is suitable.
4. OpenWeather as an optional keyed source.
5. WeatherAPI, Tomorrow.io or Visual Crossing as optional keyed sources.
6. Local current observations if available from a suitable provider.

Important: sources that require API keys must be optional and configurable.

## Priority 3: unified weather model

Create internal models:

- WeatherProviderResult
- CurrentWeatherSnapshot
- HourlyProviderForecast
- DailyProviderForecast
- ProviderHealth
- ForecastConsensus
- AccuracyScore

Every provider should be converted to the same internal format:

- temperature
- feels like
- precipitation probability
- precipitation amount
- weather code
- cloud cover
- humidity
- pressure
- wind speed
- wind gusts
- wind direction
- visibility
- UV index
- timestamp
- provider name

## Priority 4: consensus engine

Create a WeatherConsensusEngine.

It should compare several forecasts and calculate:

- final temperature
- final rain or snow risk
- expected precipitation window
- wind risk
- confidence percent
- agreement between providers
- warning when providers disagree

Example output:

- Rain risk: medium
- Risk window: 16:00-19:00
- Confidence: 72%
- Reason: 3 of 4 sources show precipitation risk, humidity is high, wind gusts increase.

## Priority 5: accuracy engine

Create WeatherAccuracyEngine.

It should not only display forecast, but also evaluate reliability.

Signals:

- provider agreement
- forecast spread in temperature
- rain probability spread
- pressure trend
- humidity trend
- cloud cover trend
- wind gusts
- current observations vs forecast
- recent provider error for this location

## Priority 6: local correction

Save recent forecasts locally and compare them with later actual weather.

Track errors:

- temperature error
- wind error
- precipitation miss
- false rain alert
- missed rain alert

Use this to adjust provider weights for the selected location.

## Priority 7: user-facing accuracy UI

Add cards:

1. Forecast reliability
2. Provider agreement
3. Rain risk next hours
4. Weather confidence percent
5. Why this forecast
6. Best time window for walking, driving, garden, sport

## Priority 8: APK workflow

After implementation:

- build debug APK through GitHub Actions;
- artifact name must remain app-debug.apk;
- test installation on Android phone.

## First implementation milestone

Version 3 should add:

- GPS forecast by exact location;
- two or more forecast sources or model variants;
- consensus calculation;
- confidence percent;
- rain risk time window;
- explanation card;
- new APK build.
