# TanStack Query → Store5 Translation Guide

A reference for porting a web application's data layer from TanStack Query (React) to Store5 on Android (Kotlin). Written for an agent doing the port, not a tutorial for a human learning either library.

Library: `org.mobilenativefoundation.store:store5` (`5.x`).

---

## 0. Read this first: the libraries are not the same shape

TanStack Query and Store5 solve overlapping problems but are organized differently. Translating one to the other line-by-line will produce something that compiles but fights both libraries. Before translating any specific hook, internalize these structural differences:

1. **TanStack Query is hook-first; Store5 is repository-first.** In TanStack, `useQuery` is called from a component and the cache is implicit and global. In Store5, you build a `Store<Key, Output>` instance per logical resource (typically inside a repository class held by a DI graph), and the UI subscribes to a `Flow` it exposes. Do **not** create one `Store` per Composable.

2. **Cache vs. source of truth is explicit.** TanStack Query has one in-memory cache keyed by `queryKey`. Store5 distinguishes a memory cache (always present, automatic) from a `SourceOfTruth` (optional, durable — typically Room or SQLDelight). If you give a Store a `SourceOfTruth`, **the SOT is the source of truth**, not the network response. The Fetcher writes through the SOT; reads emit from the SOT. This means you get free observability of writes from anywhere — but it also means if you skip the SOT, your "offline cache" is process-lifetime only.

3. **There is no `staleTime` or `cacheTime`.** Store5 has no built-in time-based freshness. Instead you write a `Validator`, which is called per-read and decides whether the cached item is still valid. Translate `staleTime` into a `Validator` that checks a timestamp you store alongside the data. (See §5.)

4. **Mutations are a separate object (`MutableStore`).** A read-only Store has no `write()`. To get TanStack-style `useMutation` with optimistic updates and write-back, you build a `MutableStore` via `MutableStoreBuilder.from(...).build(updater, bookkeeper)`. Without those, you cannot do server-syncing writes.

5. **`queryClient.invalidateQueries` does not exist.** The closest equivalents are `store.clear(key)` / `store.clearAll()` (purge cache + SOT, next read goes to network) and `StoreReadRequest.fresh(key)` (force a network read on this subscription). For "invalidate by tag" patterns, you must build it yourself or restructure around the SOT — writes to the SOT automatically propagate to all subscribers of that key.

6. **`enabled: false` does not exist.** In TanStack, `useQuery({ enabled: !!userId })` is idiomatic. In Store5, you control subscription at the call site — if the key isn't ready, don't call `store.stream(...)`. In a `ViewModel`, this usually means switching the upstream `Flow` (`flatMapLatest` over a key flow, emitting `flowOf(Loading)` when the key is null).

Keep these differences in mind for everything below.

---

## 1. Vocabulary map

| TanStack Query | Store5 | Notes |
|---|---|---|
| `QueryClient` | (no direct equivalent) | Each `Store` is its own coordinator. There is no global cache to invalidate across resources. Do not try to build a singleton wrapper that mimics `QueryClient` — it works against the grain. |
| `queryKey: ['posts', id]` | `Key` type parameter on `Store<Key, Output>` | The key is a typed value (often `Int`, `String`, or a small `data class`), not an array. Use a sealed class or `data class` for compound keys. |
| `queryFn` | `Fetcher.of { key -> ... }` | Suspend lambda from `Key` to network model. `Fetcher.ofResultFlow` for streaming/SSE. `Fetcher.ofFlow` for non-result flows. |
| `useQuery(...)` | `store.stream(StoreReadRequest.cached(key, refresh))` | Returns `Flow<StoreReadResponse<Output>>`. |
| `useMutation(...)` | `mutableStore.write(StoreWriteRequest.of(...))` | Requires `MutableStore` (built with `Updater` + `Bookkeeper`). |
| `useInfiniteQuery` | Page-keyed `Store` + paginator on top | No first-class infinite query. See §10. |
| `staleTime` | `Validator` | Implement `isValid(item): Boolean`. Store the timestamp inside your domain/entity model. |
| `gcTime` / `cacheTime` | `MemoryPolicy` (`expireAfterAccess`, `expireAfterWrite`, `setMaxSize`) on `StoreBuilder` | Memory cache only; SOT is governed by your DB's lifecycle. |
| `refetchOnMount` / `refetchOnWindowFocus` | `StoreReadRequest.fresh(key)` triggered by lifecycle | Wire to `Lifecycle.repeatOnLifecycle(STARTED)` or `WindowInfo.isWindowFocused` in Compose. |
| `placeholderData` / `initialData` | Seed via SOT, or use Store's first emission | No dedicated API. Pre-populate the SOT, or render `Loading` until first `Data`. |
| `select` | Map on the `Flow` after `store.stream(...)` | `.map { it.dataOrNull()?.let(transform) }`. Do this in the `ViewModel`, not the Store. |
| `keepPreviousData` | Manual: hold last `Data` value in `ViewModel` state | No built-in. `scan` over the flow keeping the last successful value. |
| `onSuccess` / `onError` (mutation) | Inspect `StoreWriteResponse` | `Success.Typed<Response>(response)`, `Error.Exception`, `Error.Message`. |
| `optimisticUpdate` | Write to `MutableStore` first; `Updater` syncs to network; `Bookkeeper` records failures | Optimism is the default — local write goes through the SOT immediately. |
| `queryClient.setQueryData(key, data)` | `mutableStore.write(StoreWriteRequest.of(key, data))` | Requires `MutableStore`. |
| `queryClient.invalidateQueries({ queryKey })` | `store.clear(key)` + re-stream, OR `StoreReadRequest.fresh(key)` | See §6. |
| `queryClient.removeQueries` | `store.clear(key)` / `store.clearAll()` | Removes from memory cache and SOT. |
| `useQueries` (parallel) | Combine multiple `store.stream(...)` flows with `combine { ... }` | Idiomatic Kotlin; nothing Store-specific. |
| Query devtools | None | Use Flipper, Chucker for network, and DB inspector for the SOT. |

---

## 2. The five Store5 components

A read-only Store needs a `Fetcher`. Everything else is optional. A full CRUD-capable `MutableStore` uses up to seven pieces. From inside out:

- **Fetcher** — how to get data from the network. Required.
- **SourceOfTruth** — durable local store (Room/SQLDelight). Optional but strongly recommended for any real app.
- **Converter** — translates between three types: `Network` (what the API returns), `Local` (what the SOT persists), `Output` (your domain model). Required when these differ, which they almost always should.
- **Validator** — given a cached `Output`, return whether it's still valid. Optional. This is your `staleTime`.
- **MemoryPolicy** — sizes and TTL for the in-memory cache layer. Optional.
- **Updater** *(MutableStore only)* — how to push a local write to the network.
- **Bookkeeper** *(MutableStore only)* — records failed writes so they can be retried.

The four type parameters on `MutableStoreBuilder.from<Key, Network, Output, Local>` correspond to: cache key, what the Fetcher returns, what the UI sees, what the SOT stores. Keep them distinct in nontrivial cases — collapsing `Network == Output` is the #1 way ports become fragile.

---

## 3. Translating a basic `useQuery`

### Web (TanStack Query)

```ts
function usePost(id: number) {
  return useQuery({
    queryKey: ['post', id],
    queryFn: () => api.getPost(id),
    staleTime: 60_000,
  });
}

function PostScreen({ id }: { id: number }) {
  const { data, isLoading, error } = usePost(id);
  if (isLoading) return <Spinner />;
  if (error) return <ErrorView error={error} />;
  return <PostView post={data!} />;
}
```

### Android (Store5 + Compose)

Build the Store once, in the repository:

```kotlin
typealias PostStore = Store<Int, Post>

class PostRepository(
    private val api: PostApi,
    private val db: AppDatabase, // Room
) {
    val store: PostStore = StoreBuilder
        .from<Int, PostNetworkModel, Post, PostEntity>(
            fetcher = Fetcher.of { id -> api.getPost(id) },
            sourceOfTruth = SourceOfTruth.of(
                reader = { id -> db.postDao().observeById(id).map { it?.toDomain() } },
                writer = { _, entity -> db.postDao().upsert(entity) },
                delete = { id -> db.postDao().deleteById(id) },
                deleteAll = { db.postDao().deleteAll() },
            ),
        )
        .converter(
            Converter.Builder<PostNetworkModel, PostEntity, Post>()
                .fromNetworkToLocal { it.toEntity() }
                .fromOutputToLocal { it.toEntity() }
                .build()
        )
        .validator(Validator.by { post -> post.isFresh(maxAgeMs = 60_000) })
        .build()
}
```

Subscribe in the `ViewModel`:

```kotlin
class PostViewModel(
    private val repo: PostRepository,
    private val id: Int,
) : ViewModel() {
    val state: StateFlow<UiState<Post>> = repo.store
        .stream(StoreReadRequest.cached(id, refresh = false))
        .map { response ->
            when (response) {
                is StoreReadResponse.Loading -> UiState.Loading
                is StoreReadResponse.Data -> UiState.Success(response.value)
                is StoreReadResponse.Error.Exception -> UiState.Error(response.error)
                is StoreReadResponse.Error.Message -> UiState.Error(IllegalStateException(response.message))
                is StoreReadResponse.NoNewData -> UiState.Loading // or hold previous
                StoreReadResponse.Initial -> UiState.Loading
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
```

Render in Compose:

```kotlin
@Composable
fun PostScreen(vm: PostViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        UiState.Loading -> Spinner()
        is UiState.Error -> ErrorView(s.error)
        is UiState.Success -> PostView(s.value)
    }
}
```

**Things the agent should not get wrong here:**

- `StoreReadResponse` is a sealed class — handle all branches. `NoNewData` is emitted when the fetcher ran but the SOT already has equivalent data; `Initial` is the very first emission before anything has happened.
- `StoreReadRequest.cached(...)` will use cache/SOT first and only hit the network if cache is empty or invalid. `StoreReadRequest.fresh(...)` always hits the network.
- The `Fetcher.of { ... }` lambda is a `suspend` function. Use Retrofit suspend functions or `httpClient.get(...).body()` from Ktor. Do **not** wrap in `runBlocking` or `flow { emit(api.call()) }`.
- `SourceOfTruth.reader` returns a `Flow<Output?>`. With Room, return a `@Query` that returns `Flow<Entity?>` and `.map { it?.toDomain() }`. Do not collect-then-emit; let Room push.

---

## 4. Translating `useMutation`

### Web

```ts
const queryClient = useQueryClient();
const mutation = useMutation({
  mutationFn: (post: Post) => api.updatePost(post),
  onMutate: async (post) => {
    await queryClient.cancelQueries({ queryKey: ['post', post.id] });
    const prev = queryClient.getQueryData(['post', post.id]);
    queryClient.setQueryData(['post', post.id], post);
    return { prev };
  },
  onError: (_err, post, ctx) => {
    queryClient.setQueryData(['post', post.id], ctx?.prev);
  },
  onSettled: (_data, _err, post) => {
    queryClient.invalidateQueries({ queryKey: ['post', post.id] });
  },
});
```

### Android

Use `MutableStoreBuilder` instead of `StoreBuilder`:

```kotlin
val store: MutableStore<Int, Post> = MutableStoreBuilder
    .from(
        fetcher = Fetcher.of { id -> api.getPost(id) },
        sourceOfTruth = sourceOfTruth, // same as before
        converter = converter,
    )
    .build(
        updater = Updater.by(
            post = { _, value -> // suspend (Key, Output) -> UpdaterResult
                try {
                    val updated = api.updatePost(value)
                    UpdaterResult.Success.Typed(updated)
                } catch (t: Throwable) {
                    UpdaterResult.Error.Exception(t)
                }
            },
        ),
        bookkeeper = Bookkeeper.by(
            getLastFailedSync = { id -> db.bookkeepingDao().getLastFailure(id) },
            setLastFailedSync = { id, ts -> db.bookkeepingDao().recordFailure(id, ts); true },
            clear = { id -> db.bookkeepingDao().clear(id); true },
            clearAll = { db.bookkeepingDao().clearAll(); true },
        ),
    )
```

Then write:

```kotlin
suspend fun updatePost(post: Post) {
    store.write(StoreWriteRequest.of(key = post.id, value = post))
}
```

What this gives you for free, that TanStack requires manual wiring for:

- **Optimistic update**: the SOT is written before the network call, so any subscriber to `store.stream(post.id)` sees the new value immediately.
- **Rollback on error**: not automatic. If the `Updater` returns `Error`, the local SOT still has the optimistic value. To roll back, you must either re-fetch (`StoreReadRequest.fresh`) or write the previous value back. **The agent must implement explicit rollback if the product requires it.**
- **Retry of failed syncs**: `Bookkeeper` records the failure timestamp; you trigger retries (e.g., on connectivity restore) by reading bookkeeping records and re-issuing `store.write(...)`.

---

## 5. Translating `staleTime` → `Validator`

`Validator` runs against an `Output` already in cache and returns `true` if it's still valid. Store the fetch timestamp on the domain model (or alongside it) — Store5 will not track time for you.

```kotlin
data class Post(
    val id: Int,
    val title: String,
    val body: String,
    val fetchedAtMs: Long, // populated by your Network→Local converter
)

private val validator = Validator.by { post: Post ->
    val ageMs = Clock.System.now().toEpochMilliseconds() - post.fetchedAtMs
    ageMs < 60_000L // staleTime: 60s
}
```

Wire it: `.validator(validator)` on the builder. When a cached value fails the validator, Store5 treats it as missing and triggers the Fetcher.

`gcTime` translates to `MemoryPolicy`:

```kotlin
.cachePolicy(
    MemoryPolicy.builder<Int, Post>()
        .setMaxSize(100)
        .setExpireAfterAccess(5.minutes)
        .build()
)
```

Note: `MemoryPolicy` only affects the in-memory layer. Eviction from the SOT (Room) is your DB's responsibility; Store5 won't touch it.

---

## 6. Translating `invalidateQueries`

There is no global invalidation primitive. Three patterns, in order of preference:

1. **Write-through** (preferred): if you have a `MutableStore` and the mutation response includes the new entity, just `store.write(...)`. Subscribers update automatically. No invalidation needed.
2. **Force refresh on the active subscription**: re-issue the read with `StoreReadRequest.fresh(key)`. In a `ViewModel`, expose a `MutableSharedFlow<RefreshTrigger>` and `flatMapLatest` over it.
3. **Hard clear**: `store.clear(key)` removes the entry from memory cache and SOT; the next subscription goes to network. Use sparingly — it causes a UI loading state.

For "invalidate all queries with a tag" patterns (TanStack `invalidateQueries({ queryKey: ['posts'] })`), restructure: have one `Store<Unit, List<Post>>` for the list, and individual `Store<Int, Post>` for items. Mutations write to both. There is no implicit fan-out.

---

## 7. Translating `enabled: false`

Move conditional fetching upstream of the Store call. The agent should never create a sentinel "null" key that the Fetcher then handles.

```kotlin
class FeedViewModel(
    private val repo: FeedRepository,
    private val authState: Flow<AuthState>,
) : ViewModel() {

    val state: StateFlow<UiState> = authState
        .flatMapLatest { auth ->
            when (auth) {
                is AuthState.SignedIn -> repo.store
                    .stream(StoreReadRequest.cached(auth.userId, refresh = false))
                    .map(::toUiState)
                AuthState.SignedOut -> flowOf(UiState.SignedOut)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
```

`flatMapLatest` cancels the previous Store subscription when the key changes — this is the equivalent of `useQuery` re-running when its key changes.

---

## 8. Translating `refetchOnWindowFocus` / `refetchOnMount`

There is no built-in. Wire it explicitly:

```kotlin
@Composable
fun PostScreen(vm: PostViewModel) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.refresh() // calls store.stream(StoreReadRequest.fresh(...)) internally
        }
    }
    // ...
}
```

For app-wide focus refresh, observe `ProcessLifecycleOwner.get().lifecycle`. Be conservative — mobile users return to the app constantly; refreshing on every `RESUMED` will spam your API. A `Validator` with a sensible age check is usually a better default than focus-based refresh.

---

## 9. Translating `useQueries` and dependent queries

Parallel queries:

```kotlin
val combined: Flow<UiState> = combine(
    userStore.stream(StoreReadRequest.cached(userId, refresh = false)),
    settingsStore.stream(StoreReadRequest.cached(userId, refresh = false)),
) { user, settings -> mergeToUiState(user, settings) }
```

Dependent queries (TanStack: `enabled: !!userId`, then `enabled: !!user`): use `flatMapLatest` chains.

```kotlin
val profile = userIdFlow
    .flatMapLatest { id -> userStore.stream(StoreReadRequest.cached(id, refresh = false)) }
    .map { it.dataOrNull() }
    .filterNotNull()
    .flatMapLatest { user -> profileStore.stream(StoreReadRequest.cached(user.profileId, refresh = false)) }
```

---

## 10. Translating `useInfiniteQuery`

Store5 has no native infinite-query support. The pattern that works is:

- Define a page key: `data class FeedPageKey(val cursor: String?)`.
- `Store<FeedPageKey, FeedPage>` fetches one page at a time.
- The repository accumulates pages in a `MutableStateFlow<List<FeedItem>>`, calling `store.stream(...).first { it is Data }` per page.
- The SOT stores items keyed individually, plus a "page index" table mapping `cursor → List<itemId>`. This lets pull-to-refresh re-fetch page 1 without invalidating everything.

For most apps, **AndroidX Paging 3** is a better fit than building this yourself. You can compose them: have your `RemoteMediator` use a `Store5` instance for caching, and Paging handle the pagination UI contract.

---

## 11. Threading and lifecycle

- All Store5 entry points are `suspend` or `Flow`. Call from `viewModelScope` (or equivalent). Never from `Main` directly via `runBlocking`.
- `store.stream(...)` is cold per subscriber. Two collectors of the same key both get cached data immediately, but the Fetcher runs once (deduplicated by the FetcherController).
- Use `SharingStarted.WhileSubscribed(5_000)` on the `ViewModel`'s `stateIn` so a brief config change doesn't drop the upstream subscription. This is the closest analog to TanStack's "keep query alive across remounts" behavior.

---

## 12. Type-mapping checklist

When porting a single `useQuery` hook, the agent should produce, in order:

1. **Domain model** (`Post`) — what the UI consumes. Probably already defined on the web side as a TS interface; translate to `data class`.
2. **Network model** (`PostNetworkModel`) — `@Serializable` `data class`. Mirror the wire format exactly. Don't reuse the domain model here.
3. **Local entity** (`PostEntity`) — `@Entity` for Room. Includes a `fetchedAtMs: Long` if you're using a time-based `Validator`.
4. **DAO** with `@Query("SELECT * FROM post WHERE id = :id") fun observeById(id: Int): Flow<PostEntity?>` and upsert/delete.
5. **Converter** — three pure functions: `Network → Local`, `Output → Local`, and the Local → Output mapping happens in your DAO `.map { it?.toDomain() }` (Store5's converter doesn't have a `localToOutput` slot in `5.x`; reads convert via the SOT reader's mapping).
6. **Fetcher** — calls the API client, returns `PostNetworkModel`.
7. **Validator** — optional. If skipped, cached data is always considered valid and only `clear`/`fresh` will re-fetch.
8. **Updater + Bookkeeper** — only for mutations.
9. **Store builder** wiring the above.
10. **Repository** holds the Store and exposes a narrow API (`fun observe(id: Int): Flow<...>`, `suspend fun update(post: Post)`).
11. **ViewModel** maps `StoreReadResponse` to `UiState`.

If any of these are skipped, write a comment in the file explaining why — there is almost always a reason a real app needs all of them.

---

## 13. Things to refuse to port directly

These TanStack patterns map poorly and the agent should flag them rather than translate mechanically:

- **`useQuery` inside a deeply nested component without prop-drilling the data**. On Android, hoist to the `ViewModel`. Don't try to reproduce per-component data fetching.
- **`queryClient` passed as a context value used for arbitrary cache pokes**. Each Store should have a typed API on its repository. If five places are calling `queryClient.setQueryData`, that's five repository methods.
- **A mutation that on success calls `invalidateQueries` for ten unrelated keys**. This is a smell on the web too. On Android, model the dependency explicitly: have the mutation write to the relevant Stores, or have those Stores share a common SOT table that updates reactively.
- **Suspense / `useSuspenseQuery`**. Store5 has no equivalent. `StoreReadResponse.Loading` is the only loading state; render it.
- **`networkMode: 'offlineFirst'`**. Store5 with a SOT *is* offline-first. Don't add another flag.

---

## 14. Minimal Gradle setup

```kotlin
// libs.versions.toml
[versions]
store = "5.1.0"
room = "2.7.0"

[libraries]
store = { module = "org.mobilenativefoundation.store:store5", version.ref = "store" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
```

Verify the exact current Store5 version against `https://github.com/MobileNativeFoundation/Store/releases` before pinning — the agent should not assume the version above is current.

---

## 15. Quick-reference cheat sheet

```
useQuery(key, fn)                  → store.stream(StoreReadRequest.cached(key, false))
useQuery(key, fn, { staleTime })   → + Validator on Store
useMutation({ mutationFn })        → mutableStore.write(StoreWriteRequest.of(key, value))
queryClient.setQueryData(k, v)     → mutableStore.write(StoreWriteRequest.of(k, v))
queryClient.invalidateQueries(k)   → store.clear(k) OR StoreReadRequest.fresh(k)
queryClient.removeQueries(k)       → store.clear(k)
useInfiniteQuery                   → page-keyed Store + Paging 3 (recommended)
enabled: false                     → flatMapLatest over a key Flow upstream of the Store
refetchOnWindowFocus               → Lifecycle.repeatOnLifecycle(RESUMED) → fresh()
keepPreviousData                   → scan() in ViewModel keeping last Success
select                             → .map { it.dataOrNull()?.let(transform) } in ViewModel
useQueries (parallel)              → combine(store1.stream(...), store2.stream(...))
QueryClientProvider                → no equivalent; inject Repositories via DI (Hilt/Koin)
```