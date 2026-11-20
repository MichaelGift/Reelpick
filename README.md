# Reelpick & Playpick: recommendations you can see through

Two Android apps built on one recommendation engine and one design system:

| | |
|---|---|
| **Reelpick** (this page) | Films and TV series from TMDB, with cast & crew pages and where to watch. |
| **[Playpick](games/README.md)** | Games from RAWG, with a completion checklist built from each game's achievements. |

Both show a **taste match** and **why** for every pick, and show you the taste profile they've learned. The shared parts live in [`core/recommend`](core/recommend) (the taste profile and ranker, plain Kotlin) and [`core/design`](core/design) (theme, type and components, with each app supplying its own brand palette).

---

## Reelpick

An Android app that learns what you like and builds a personal **For You** feed of films and TV series. Every pick shows a **taste match** and **why it's there** ("Because you liked *Arrival*"), and the app shows you the taste profile it has learned. Built with Jetpack Compose, Hilt, Room and the TMDB API.

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/04-for-you.jpg" width="200" alt="For You feed with a full-bleed hero, taste match and the reason it was picked"><br><sub><b>For You</b>: match % and why</sub></td>
    <td align="center"><img src="docs/screenshots/05-top10-taste.jpg" width="200" alt="Ranked Top 10 row and the learned taste profile card"><br><sub><b>Top 10 + taste profile</b></sub></td>
    <td align="center"><img src="docs/screenshots/06-details.jpg" width="200" alt="Movie details with taste-match card, watchlist and rating controls"><br><sub><b>Details</b>: why it fits you</sub></td>
    <td align="center"><img src="docs/screenshots/07-search.jpg" width="200" alt="Browsing the Crime genre as a poster grid"><br><sub><b>Search</b>: browse by genre</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/01-welcome.jpg" width="200" alt="Welcome screen over a drifting poster mosaic"><br><sub><b>Welcome</b></sub></td>
    <td align="center"><img src="docs/screenshots/02-genres.jpg" width="200" alt="Onboarding step 1: picking genres"><br><sub><b>Pick genres</b></sub></td>
    <td align="center"><img src="docs/screenshots/03-seeds.jpg" width="200" alt="Onboarding step 2: picking five seed films"><br><sub><b>Pick seed films</b></sub></td>
    <td align="center"><img src="docs/screenshots/08-watchlist.jpg" width="200" alt="Watchlist poster grid"><br><sub><b>Watchlist</b> (offline)</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/09-scope-switch.jpg" width="200" alt="For You feed with the All, Movies and Series switch and a series in the hero"><br><sub><b>All / Movies / Series</b></sub></td>
    <td align="center"><img src="docs/screenshots/10-series-details.jpg" width="200" alt="Series details with years, seasons, episodes, status and network"><br><sub><b>Series details</b></sub></td>
    <td align="center"><img src="docs/screenshots/11-person.jpg" width="200" alt="Actor page with bio and known-for titles carrying taste matches"><br><sub><b>Cast &amp; crew pages</b></sub></td>
    <td align="center"><img src="docs/screenshots/12-search-people.jpg" width="200" alt="Search results with a People row above films and series"><br><sub><b>Search</b>: people, films, series</sub></td>
  </tr>
</table>

<sub>Captured on a Pixel emulator (Android 16) with live TMDB data. Saving four crime and drama films shifted the profile, which is why "Crime" appears in the taste card and the matches read 76%.</sub>

### Design

A dark-first, cinematic UI built from scratch as a small design system (`ui/theme`, `ui/common`):

- **Type:** Fraunces (display) paired with Manrope (UI), both variable fonts bundled under the SIL OFL.
- **Brand:** ember-to-magenta gradient, near-black surfaces, custom adaptive and themed launcher icon, and a branded splash screen.
- **Motion:** shared-element poster transitions into details, hero parallax, an auto-advancing hero, shimmer skeletons, and haptics on ratings.
- **Explainability:** taste-match badges, reason pills, and a "What shapes your picks" card that shows the learned genre weights.

### Features

- **Onboarding**: a cinematic welcome over a drifting poster mosaic. Then pick 3 or more genres and at least 5 films or series you've enjoyed; these seed your taste profile.
- **For You feed**: an **All / Movies / Series** switch (instant and offline, remembered between launches), a full-bleed hero carousel, a ranked *Top 10 for you*, your taste profile, and rows for *Because you liked X*, *Your genres* and *Trending*. Pull to refresh, and long-press any poster to like, dislike, hide it or save it.
- **Live re-ranking**: a dislike or "not interested" reorders the feed immediately, offline; a new like brings in fresh candidates.
- **Details**: cast, trailer, genres, **where to watch** (stream, rent or buy for your region) and "More like this". Films show runtime and who directed and wrote them; series show years, seasons, episodes, status, network, creators and each actor's episode count.
- **Cast & crew pages**: tap any actor, director, writer or creator for their bio, age, *Known for* and a filmography split by department, with your taste match on every title.
- **Search**: one box for films, series and people, debounced and paginated, with recent searches and *Browse by genre* tiles.
- **Watchlist**: an offline poster grid. Long-press to mark as watched or remove, with undo.
- **Settings**: streaming region, light/dark/system theme, and a taste-profile reset.

### How recommendations work

The engine lives in [`domain/recommend`](app/src/main/java/com/myth/movieapp/domain/recommend). It's plain Kotlin with no Android dependencies and is fully unit-tested.

1. **Taste profile** ([`TasteProfile`](app/src/main/java/com/myth/movieapp/domain/recommend/TasteProfile.kt)). Each genre gets a weight from explicit signals only:

   | Signal | Weight per genre |
   |---|---|
   | Genre picked in onboarding | +1 |
   | Liked film or series | +2 |
   | Disliked / not interested | −2 |
   | On watchlist | +1 |

   Weights are normalised so the strongest genre has a weight of 1. A title's fit is the cosine similarity between its genres and this profile.

   Films and series share one genre space: [`GenreMap`](app/src/main/java/com/myth/movieapp/data/remote/GenreMap.kt) splits TV's combined genres (such as *Sci-Fi & Fantasy*) into the movie genres, so liking a show teaches the same profile as liking a film.

2. **Candidates** ([`CandidateSource`](app/src/main/java/com/myth/movieapp/domain/recommend/CandidateSource.kt)), fetched concurrently:
   - TMDB recommendations for your 5 most recent likes (the "seeds"): a liked show brings shows, a liked film brings films
   - Well-voted films and series in your top 3 genres
   - This week's trending films and series, for exploration

   TMDB reuses numeric ids across films and TV, so every title is identified by `(media type, id)`: in Room, in the exclusions and in navigation.

   The feed fails only if every source fails.

3. **Ranking** ([`RecommendationRanker`](app/src/main/java/com/myth/movieapp/domain/recommend/RecommendationRanker.kt)):

   ```
   score = 0.45 · genre affinity
         + 0.30 · seed support      (how many of your likes recommended it, capped at 3)
         + 0.20 · Bayesian rating   (a 9.0 from 12 votes doesn't beat an 8.4 from 20k)
         + 0.05 · freshness         (linear decay over 10 years)
   ```

   Titles you've rated, saved or watched are filtered out. A diversity pass then stops more than two picks in a row from sharing the same dominant genre. Each pick keeps the reason it was chosen.

4. **Feed** ([`GetForYouFeedUseCase`](app/src/main/java/com/myth/movieapp/domain/recommend/GetForYouFeedUseCase.kt)) ranks one candidate pool, builds the sections for each scope (all, films, series) and caches them in Room, so the home screen opens instantly, switching scope needs no network, and it all works offline.

### Architecture

```
core/recommend Shared, plain Kotlin: generic TasteProfile<F> and Ranker over any item type (ItemTraits)
core/design    Shared Compose design system: BrandTheme(palette), Fraunces/Manrope, generic components
app/           Reelpick
games/         Playpick (see games/README.md)
```

Inside each app:

```
ui/            Compose screens + @HiltViewModel per screen (StateFlow UI state)
  onboarding/  foryou/  details/  person/  search/  watchlist/  settings/  navigation/  theme/  common/
domain/        Pure Kotlin: models, repository interfaces, recommendation engine
data/          Retrofit (TMDB), Room (feedback, watchlist, genres, feed cache), DataStore (prefs)
di/            Hilt modules
```

- Single activity with type-safe Navigation Compose routes.
- Offline-first: the feed, watchlist and ratings live in Room. The network only adds or refreshes data.
- The Room schema is exported to `app/schemas`, and upgrades migrate existing likes and watchlists rather than wiping them (covered by a `MigrationTestHelper` test).
- Repositories return `Result` rather than throwing. The UI maps errors to clear messages (offline, bad token, missing token).
- The TMDB token is sent as a bearer header by an OkHttp interceptor, so it never appears in URLs or logs.

### Tech stack

Kotlin 2.3 · Jetpack Compose (Material 3) · Navigation Compose · Hilt · KSP · Room · DataStore · Retrofit + OkHttp + kotlinx.serialization · Coil 3 · Coroutines/Flow · JUnit, Turbine, Compose UI tests · GitHub Actions

### How to run

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

### Tests

```bash
./gradlew testDebugUnitTest :core:recommend:test
```

```bash
./gradlew connectedDebugAndroidTest
```

- **Core tests** check the generic profile and ranker with non-film types, so neither app's model leaks into them.
- **Unit tests** (JVM) cover the taste profile, the ranker, candidate sourcing (including the film/series id collision), feed scopes, the genre map, the TMDB mappers, and the onboarding, search and person ViewModels.
- **Instrumented tests** cover the Room repositories and the v1 → v2 migration, plus Compose UI tests for onboarding, the details screen (films and series) and the person page.

### Attribution

Reelpick uses the TMDB API but is not endorsed or certified by TMDB. Film, TV and people data and images © [TMDB](https://www.themoviedb.org/). Streaming availability is provided by JustWatch.

## Contributing

Suggestions and fixes are welcome. Fork the repository, make your changes, and open a pull request.
