# Movie Recommendations App

An Android app that learns what you like and builds a personal **For You** feed of movies, with an explanation for every pick ("Because you liked *Arrival*"). Built with Jetpack Compose, Hilt, Room and the TMDB API.

<!-- Screenshots / demo GIF: add onboarding, For You, Details and Watchlist captures here. -->

## Features

- **Onboarding**: pick 3 or more genres and at least 5 movies you've enjoyed. These seed your taste profile.
- **For You feed**: a hero carousel plus rows for *Top picks*, *Because you liked X*, *Your genres* and *Trending*. Pull to refresh, and long-press any poster to like, dislike, hide it or save it.
- **Live re-ranking**: a dislike or "not interested" reorders the feed immediately, offline; a new like brings in fresh candidates.
- **Movie details**: cast, trailer, runtime, genres, **where to watch** (stream, rent or buy for your region) and "More like this".
- **Search**: debounced, paginated, with recent searches.
- **Watchlist**: works offline. Swipe to remove or mark as watched, with undo.
- **Settings**: streaming region, light/dark/system theme, and a taste-profile reset.

## How recommendations work

The engine lives in [`domain/recommend`](app/src/main/java/com/myth/movieapp/domain/recommend). It's plain Kotlin with no Android dependencies and is fully unit-tested.

1. **Taste profile** ([`TasteProfile`](app/src/main/java/com/myth/movieapp/domain/recommend/TasteProfile.kt)). Each genre gets a weight from explicit signals only:

   | Signal | Weight per genre |
   |---|---|
   | Genre picked in onboarding | +1 |
   | Liked movie | +2 |
   | Disliked / not interested | −2 |
   | On watchlist | +1 |

   Weights are normalised so the strongest genre has a weight of 1. A movie's fit is the cosine similarity between its genres and this profile.

2. **Candidates** ([`CandidateSource`](app/src/main/java/com/myth/movieapp/domain/recommend/CandidateSource.kt)), fetched concurrently:
   - TMDB recommendations for your 5 most recent likes (the "seeds")
   - Well-voted movies in your top 3 genres
   - This week's trending movies, for exploration

   The feed fails only if every source fails.

3. **Ranking** ([`RecommendationRanker`](app/src/main/java/com/myth/movieapp/domain/recommend/RecommendationRanker.kt)):

   ```
   score = 0.45 · genre affinity
         + 0.30 · seed support      (how many of your likes recommended it, capped at 3)
         + 0.20 · Bayesian rating   (a 9.0 from 12 votes doesn't beat an 8.4 from 20k)
         + 0.05 · freshness         (linear decay over 10 years)
   ```

   Movies you've rated, saved or watched are filtered out. A diversity pass then stops more than two picks in a row from sharing the same dominant genre. Each pick keeps the reason it was chosen.

4. **Feed** ([`GetForYouFeedUseCase`](app/src/main/java/com/myth/movieapp/domain/recommend/GetForYouFeedUseCase.kt)) splits the ranked list into sections and caches it in Room, so the home screen opens instantly and works offline.

## Architecture

```
ui/            Compose screens + @HiltViewModel per screen (StateFlow UI state)
  onboarding/  foryou/  details/  search/  watchlist/  settings/  navigation/  theme/  common/
domain/        Pure Kotlin: models, repository interfaces, recommendation engine
data/          Retrofit (TMDB), Room (feedback, watchlist, genres, feed cache), DataStore (prefs)
di/            Hilt modules
```

- Single activity with type-safe Navigation Compose routes.
- Offline-first: the feed, watchlist and ratings live in Room. The network only adds or refreshes data.
- Repositories return `Result` rather than throwing. The UI maps errors to clear messages (offline, bad token, missing token).
- The TMDB token is sent as a bearer header by an OkHttp interceptor, so it never appears in URLs or logs.

## Tech stack

Kotlin 2.1 · Jetpack Compose (Material 3) · Navigation Compose · Hilt · KSP · Room · DataStore · Retrofit + OkHttp + kotlinx.serialization · Coil 3 · Coroutines/Flow · JUnit, Turbine, Compose UI tests · GitHub Actions

## How to run

1. Clone the project and open it in Android Studio:
   ```bash
   git clone https://github.com/MichaelGift/Movie-Recommendations-App.git
   ```
2. Get a TMDB **API Read Access Token** (v4) from [TMDB → Settings → API](https://www.themoviedb.org/settings/api).
3. Add it to `local.properties` in the project root. This file is git-ignored, so the token is never committed:
   ```properties
   TMDB_ACCESS_TOKEN=your_v4_read_access_token
   ```
   On CI, set a `TMDB_ACCESS_TOKEN` repository secret instead. Without a token the app still builds and explains what's missing.
4. Sync Gradle and run the `app` configuration. You need JDK 17 or newer; Android Studio's bundled JDK works.

## Tests

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew connectedDebugAndroidTest
```

- **Unit tests** (JVM) cover the taste profile, the ranker, candidate sourcing, the feed use case, and the onboarding and search ViewModels.
- **Instrumented tests** cover the Room repositories, plus Compose UI tests for onboarding and the details screen's watchlist action.

## Attribution

This product uses the TMDB API but is not endorsed or certified by TMDB. Movie data and images © [TMDB](https://www.themoviedb.org/). Streaming availability is provided by JustWatch.

## Contributing

Suggestions and fixes are welcome. Fork the repository, make your changes, and open a pull request.
