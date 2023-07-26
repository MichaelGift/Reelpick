# Movie-Recommendations-App

## Introduction

Welcome to the Movie Recommendations App, your one-stop solution for finding the best movies to watch. If you're ever undecided on what to watch, this app will keep you up to date with a curated movie watch list. It provides you with movie recommendations, details, and the ability to save your favorite movies.

### Demo

Check out the app in action:

https://user-images.githubusercontent.com/120593172/233807849-1e5d59b2-2e24-4183-9a8f-403511faec5a.mp4


## Coming soon
I am constantly working on improving the app and adding new features. Here's what you can expect in the near future:

- **Streaming Integration:** Easily find platforms where the recommended movies are available for streaming.
- **Push Notifications:** Get timely updates on movie releases, personalized recommendations, and special offers.
- **TV Show Category:** Explore a diverse collection of TV shows for binge-watching enthusiasts.
- **Favorites Sync:** Enjoy your favorite movies across multiple devices with synchronized favorites.


## In Development

I have some exciting features in development

- **TV Show Category:** Currently on hold due to TMDB updating their API. We're actively working on integrating this feature once the API is ready.
- **Push Notifications:** Expected to be available by 5th June 2023. Stay tuned for updates!


## How to run

To run the app on your local machine, follow these simple steps.
1. Clone the Project to Android Studio:
```Bash
git clone https://github.com/MichaelGift/mMvie-Recommendations-App.git
```
2. Get TMDB API Key:
   - Sign up or log in to TMDB (The Movie Database) and request an API key.
   - Navigate to the module-level gradle file (usually app/build.gradle) and find the following line:
```Groovy
      buildConfigField "String","API_KEY","\(Insert TMDB API key here)"\""
```
  - Replace \(Insert TMDB API key here) with your actual TMDB API key.
3. Resync and Run
      - Resync the project to ensure the API key is properly set.
      - Click on the "Run" button in Android Studio to install and launch the app on your device/emulator.


## Tech Stack


The Movie Recommendations App is built using the following technologies and architecture:

- **MVVM (Model-View-ViewModel):** Ensuring a clear separation of concerns and making the codebase maintainable.
- **Room:** For local data storage and retrieval, enabling seamless user experience.
- **Retrofit:** For making network requests to fetch movie data from TMDB API.
- **Dagger:** To provide dependency injection and improve code modularity.
DataBinding: Making the UI components more reactive and efficient.

## Contributing
I encourage contributions to enhance the Movie Recommendations App. If you have any suggestions, bug fixes, or new features to add, please fork the repository, make your changes, and create a pull request
