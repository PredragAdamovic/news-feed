# News Feed

A small Android news reader: a paged list of US top headlines from
[NewsAPI](https://newsapi.org), a detail screen, and an offline copy of the last page.

Kotlin, Jetpack Compose, MVVM over a thin domain layer, Retrofit, Hilt.

---

## Running it

```bash
git clone <this repo>
cd news-feed
```

Add your NewsAPI key to `local.properties` in the project root (the file is git-ignored —
see `local.properties.example`):

```properties
NEWS_API_KEY=your_key_here
```

A free key takes about a minute at <https://newsapi.org/register>.

```bash
./gradlew assembleDebug        # or open the project in Android Studio and run
./gradlew testDebugUnitTest    # 18 unit tests
```

**Without a key the app still runs.** It starts, detects the empty key before making a
request, and shows a screen telling you what to add and where. That felt better than a build
failure for anyone cloning this to look around.

Minimum SDK 24, target 36, compiled against 37.

### Deep link

`myapp://article/{id}` opens an article directly, whether the app is cold or already running.

```bash
adb shell am start -a android.intent.action.VIEW -d "myapp://article/f3d61d40129a1289a2b7d61b"
adb shell am start -a android.intent.action.VIEW -d "myapp://article/c6e215c5f6dc5858b56972b7"
adb shell am start -a android.intent.action.VIEW -d "myapp://article/b85531a1974dd0a850b9870f"
```

Those three ids were live on 18 September 2026. Top headlines rotate, so by the time you read
this they will most likely resolve to the "Article not available" screen — which is the
intended behaviour for an id this install has never loaded, not a failure. See
[Deep links and article identity](#deep-links-and-article-identity).

**For an id that works right now**, derive it from any article url in the feed. The id is
the first 12 bytes of the url's SHA-256, which is exactly what `ArticleMapper.idFor` does:

```bash
printf %s "https://www.example.com/the-article-url" | shasum -a 256 | cut -c1-24
```

`adb` is not the same thing as a real link click: it runs as the shell user and always adds
`FLAG_ACTIVITY_NEW_TASK`. To exercise the path a user takes, put the link on a page and tap
it — that is how the running-app case was verified:

```html
<a href="myapp://article/f3d61d40129a1289a2b7d61b">Open article</a>
```

The scheme is a custom one because the task specified it. In production this would be an App
Link on an `https://` domain, verified through `assetlinks.json`, so that no other app can
claim it and no chooser appears — but an unverified `https://` link behaves worse than a
custom scheme, so there is no half-measure worth shipping here.

---

## How it is put together

```
dev.predrag.newsfeed
├── core/          NewsError, NewsResult — the vocabulary for failure
├── domain/        Article, ArticlePage, NewsRepository (interface only)
├── data/          NewsAPI client, DTOs, mapper, on-disk cache, repository impl
├── di/            Hilt modules
└── ui/
    ├── list/      ArticleListScreen + ViewModel + UiState
    ├── detail/    ArticleDetailScreen + ViewModel
    ├── common/    loading / error / empty / stale views, error copy
    ├── navigation/routes and the deep-link pattern
    └── util/      date formatting, Custom Tabs
```

One module. The layers are packages, and the dependency rule is enforced by the `domain`
package importing nothing from `data` or `ui`. Splitting this into Gradle modules would add
build configuration without changing a single decision at this size.

### Architecture decisions, and why

**MVVM with a domain boundary, not full Clean Architecture.** There is a `NewsRepository`
interface in `domain` that the view models depend on, and an implementation in `data`. That
boundary earns its keep: it is what lets the paging tests run against a fake in
milliseconds, and it is the seam along which the API could be swapped. What I did *not* add
is a use-case class per operation — with two operations that are already one line each, they
would be indirection with no reader benefit.

**A sealed `NewsError`, not exceptions in the UI.** The screen says different things for
"you are offline", "your key is missing" and "the service answered with 401", and offers a
retry for some and not others. Modelling that as a closed set means a new failure mode is a
compile error in the one `when` that turns errors into words, rather than a blank screen
found in testing. `NewsResult` exists rather than Kotlin's `Result` because `Result` can
only carry a `Throwable`, which is exactly what this layer has finished translating away.

**Manual paging, not Paging 3.** Paging 3 solves this, but the task describes the semantics
precisely — bottom loader, end detection, a failed page that does not wipe the list — and a
small explicit state machine shows those decisions instead of hiding them behind
`RemoteMediator`. It is also about sixty lines, all of them testable without instrumentation.
For a screen that later needed placeholders, a database-backed source of truth or
`cachedIn`, I would switch.

**Compose.** The task allows either; Compose is where new Android UI is written, and the
three list footer states (loading / error / end) are more honestly expressed as a `when`
inside `LazyColumn` than as adapter view types.

**Hilt.** One `@Singleton` graph, two modules. It is what a production codebase this would
join is almost certainly using, and it keeps the view model constructors honest.

**A JSON file for the cache, not Room.** Exactly one page is ever stored. Room would bring a
schema, a DAO, a migration story and a code generator to persist a single list. If the app
grew bookmarks, search or a database-backed paging source, that trade flips immediately.

### Three things the real endpoint forced

These are in the code with comments, but they are the parts I would want a reviewer to look
at, because none of them are visible from the API documentation.

1. **End of paging is not "fewer items than `pageSize`".** NewsAPI counts articles it has
   since removed in `totalResults` but omits them from `articles`. With `pageSize=10` the
   real pages came back as 7, 9, 7 while more pages still existed — the naive test would
   have stopped the feed after page one. The check is `page * pageSize >= totalResults`,
   with an empty page as a backstop.

2. **Articles marked `[Removed]`** arrive with that literal string in every field. They are
   dropped in the mapper; otherwise they render as rows that open nothing.

3. **The same article can arrive on two pages.** Headlines reorder between requests, and a
   duplicate key is a crash in `LazyColumn`, not a cosmetic problem. Appended pages are
   filtered against what is already loaded.

### Error handling

| Where it went wrong | What the user sees |
|---|---|
| First page, nothing loaded | Full-screen message + Retry |
| Next page, list has articles | Inline footer message + Retry; **the list stays** |
| Pull-to-refresh, list has articles | Snackbar; **the list stays** |
| Success but no articles | Empty state, still pullable |
| Offline with a cached page | The cached list plus a banner naming when it was saved |
| No API key | A setup message, before any request is made |

`CancellationException` is rethrown rather than mapped to an error — scrolling away cancels
an in-flight page, and that is not a failure.

### Deep links and article identity

NewsAPI returns no article id, and the URL is the only field that identifies an article
across pages and launches. The mapper hashes it (SHA-256, truncated) into something short
and safe in a `myapp://article/{id}` path.

Because there is no by-id endpoint, the detail screen resolves an id against what the
session has already loaded, then against the cache. A deep link naming an article this
install has never seen gets an explicit "not available" screen rather than a spinner that
never resolves. Resolving it properly would need either an endpoint that does not exist or a
local store of every article ever seen, and the second is not worth it for a headline feed.

---

## What I would do with more time

- **Screenshot/UI tests for the list states.** The paging logic is covered, but nothing
  asserts that the footer shows the error row rather than the spinner. A handful of
  `createAndroidComposeRule` tests would cover the states in the table above.
- **A `MockWebServer` test over the repository**, driving the real Retrofit stack against
  recorded NewsAPI payloads, including the `[Removed]` and short-page cases now only covered
  at the mapper level.
- **Article images.** `urlToImage` is parsed and carried through the model but never shown —
  adding Coil is one dependency and one composable, and it would make the list look like a
  news app. Left out because the task explicitly does not want dependency bloat or visual
  polish, and it would not have demonstrated anything new.
- **Observable connectivity.** Offline is currently detected by a request failing. Watching
  `ConnectivityManager` would let the app re-fetch by itself when the network returns,
  instead of waiting for a pull.
- **A real empty/stale distinction after a refresh.** Right now a cached page stops paging;
  coming back online requires a pull to resume. Acceptable, but a `RemoteMediator`-style
  source of truth would make it seamless.

## What I left out on purpose

- **Firebase Crashlytics/Analytics.** Listed as a bonus, but wiring it needs a Firebase
  project and a `google-services.json`, and committing one tied to my account would break
  the build for anyone else and put my project id in the repo. The plumbing is two plugins
  and an `initialize` call; I would add it against the team's own project.
- **Use-case classes.** Explained above: two one-line operations.
- **A multi-module split.** Same reasoning — real value at three teams and a long build, no
  value at this size.
- **Image loading, animations, pixel-level design.** Explicitly not what the task is about.
- **`content` from the API.** The free plan truncates it to ~200 characters with a
  `[+N chars]` suffix, so showing it would look broken. The detail screen uses `description`
  and sends the reader to the source.
