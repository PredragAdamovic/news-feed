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

```bash
./gradlew assembleDebug        # or open the project in Android Studio and run
./gradlew testDebugUnitTest    # 18 unit tests
```

**There is nothing to set up.** A free-tier NewsAPI key is checked into
`app/build.gradle.kts` and used by default. Put your own in `local.properties` (git-ignored,
see `local.properties.example`) to override it:

```properties
NEWS_API_KEY=your_key_here
```

A free key takes about a minute at <https://newsapi.org/register>. With neither, the app
detects the empty value before making a request and shows a screen saying what to add.

Minimum SDK 24, target 36, compiled against 37.

### Deep links

Two forms reach the same destination.

**`myapp://article/{id}`** — the scheme the task asked for. A custom scheme needs no setup,
but it cannot be sent to anyone: messengers only linkify things that look like URLs.

```bash
adb shell am start -a android.intent.action.VIEW -d "myapp://article/<id>"
```

**`https://predragadamovic.github.io/news-feed/article/{id}`** — the shareable form, behind
the share action on the detail screen. This is a verified App Link: the manifest filter
carries `autoVerify="true"`, and the host serves
[`/.well-known/assetlinks.json`](https://predragadamovic.github.io/.well-known/assetlinks.json)
naming this package and its signing certificate.

The verification is not decoration. Since Android 12 an unverified `http(s)` filter never
opens the app at all — the link goes straight to the browser, with no chooser. Confirmed on
an Android 14 device:

```
$ adb shell pm get-app-links dev.predrag.newsfeed
    Domain verification state:
      predragadamovic.github.io: verified
```

**For an id that resolves right now**, derive it from any article url in the feed — the id is
the first 12 bytes of the url's SHA-256, which is what `ArticleMapper.idFor` does:

```bash
printf %s "https://www.example.com/the-article-url" | shasum -a 256 | cut -c1-24
```

An id this install has never loaded gets the "Article not available" screen rather than a
spinner that never resolves; see
[Deep links and article identity](#deep-links-and-article-identity).

Note that `adb shell am start` is not the same as a real click — it runs as the shell user and
always adds `FLAG_ACTIVITY_NEW_TASK`. The already-running case was verified by tapping a link
rather than by `adb`.

---

## How it is put together

```
dev.predrag.newsfeed
├── core/          NewsError, NewsResult — the vocabulary for failure
├── domain/        Article, ArticlePage, NewsRepository (interface), use cases
├── data/          NewsAPI client, DTOs, mapper, on-disk cache, repository impl
├── di/            Hilt modules
└── ui/
    ├── list/      ArticleListScreen + ViewModel + UiState
    ├── detail/    ArticleDetailScreen + ViewModel
    ├── common/    loading / error / empty / stale views, error copy
    ├── navigation/routes and the two deep-link patterns
    ├── theme/     colour scheme
    └── util/      date formatting, Custom Tabs, sharing
```

One module. The layers are packages, and the dependency rule is enforced by the `domain`
package importing nothing from `data` or `ui`. Splitting this into Gradle modules would add
build configuration without changing a single decision at this size.

### Architecture decisions, and why

**MVVM over a Clean-style domain layer.** `domain` holds the models, the `NewsRepository`
contract and the two use cases the view models call; `data` holds the implementation. The
view models depend on `GetTopHeadlinesUseCase` and `GetArticleByIdUseCase` and never see the
repository at all, which is what keeps `ui` from reaching into `data` — a rule that is easy
to state and easy to break. It was broken here once: `FIRST_PAGE` used to be read from
`NewsRepositoryImpl.Companion`, so `ui` imported a constant out of `data` and compiled
perfectly well. It now sits on `ArticlePage`, where both layers can see it without either
reaching past the domain.

The two use cases are thin, and that is worth saying out loud: with two one-line operations
there is no logic for them to own yet. What they buy is the seam. The first operation that
has to combine sources — headlines minus what the reader has bookmarked, say — belongs
neither in the repository nor in the view model, and this is where it goes.

The `NewsRepository` interface still carries the most weight: it is what lets the paging
tests run against a fake in milliseconds, and the seam along which the API could be swapped.

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

### Crashlytics and Analytics

Wired against a Firebase project; `google-services.json` is committed. `NewsErrorMapper`
reports non-fatals to Crashlytics for `Unexpected` and `Malformed` only. Analytics is on with
automatic screen and session tracking.

### Inspecting traffic

Debug builds bundle [Chucker](https://github.com/ChuckerTeam/chucker) — every request is
captured, posted as a notification and browsable from its own launcher entry. `AppModule`
calls `networkInspector(context)`, which the debug source set implements with the interceptor
and the release source set implements as `null`, so nothing of it ships in release.

Needs the notification permission on Android 13+.

### Deep links and article identity

NewsAPI returns no article id, and the URL is the only field that identifies an article
across pages and launches. The mapper hashes it (SHA-256, truncated) into something short
and safe in a link path — the same id serves both the custom scheme and the App Link.

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
- **Article images.** `urlToImage` is dropped in the mapper rather than carried unused;
  showing it is one dependency (Coil) and one composable, and it would make the list look
  like a news app. Left out because the task explicitly does not want dependency bloat or
  visual polish, and it would not have demonstrated anything new.
- **Observable connectivity.** Offline is currently detected by a request failing. Watching
  `ConnectivityManager` would let the app re-fetch by itself when the network returns,
  instead of waiting for a pull.
- **A real empty/stale distinction after a refresh.** Right now a cached page stops paging;
  coming back online requires a pull to resume. Acceptable, but a `RemoteMediator`-style
  source of truth would make it seamless.

## What I left out on purpose

- **A multi-module split.** Real value at three teams and a long build, no value at this
  size — though it is what would turn the dependency rule from something to notice into a
  build error.
- **Image loading, animations, pixel-level design.** Explicitly not what the task is about.
- **`content` from the API.** The free plan truncates it to ~200 characters with a
  `[+N chars]` suffix, so showing it would look broken. The detail screen uses `description`
  and sends the reader to the source.
