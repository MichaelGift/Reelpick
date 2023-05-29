# Movie-Recommendations-App
---
Undecided on what to watch? Use this app and stay up to date with the best movie watch list.

### Demo
---

https://user-images.githubusercontent.com/120593172/233807849-1e5d59b2-2e24-4183-9a8f-403511faec5a.mp4


## Coming soon
---
- Streaming
- Push Notification
- Tv Show Category
- Favorites
## In Development
---
Tv Show Category - on Hold-> TMDB is updating their API

Push Notification - 5/06/23

### How to run
---
1. Clone the project to Android Studio
2. Sign up or login to TMDB and request an API Key
3. Navigate to module level gradle file, under build

```Groovy
      buildConfigField "String","API_KEY","\(Insert TMDB API key here)"\""
```

4. Resync and RUN


### Tech Stack
---
- MVVM
- Room
- Retrofit
- Dagger
- DataBinding
