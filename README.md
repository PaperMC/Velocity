# bVelocity

bVelocity is a performance-oriented Velocity fork for operators who care about one thing first: pushing more Minecraft traffic through the same bandwidth budget.

Compared with stock Velocity, bVelocity ships with more aggressive compression defaults, higher native compression levels, built-in compression observability, and a cleaner operational command surface. The goal is simple: when the traffic is compressible, bVelocity squeezes it hard.

## Why bVelocity

- higher default compression pressure out of the box
- live compression counters and synthetic benchmark tooling

If your network is dominated by chunk data, NBT-heavy payloads, or other repetitive Minecraft traffic, bVelocity is designed to turn that into noticeably lower wire usage without requiring a plugin stack or external instrumentation.

## For Existing Velocity Users

If you are moving from stock Velocity, the migration is straightforward:

1. Stop the old proxy.
2. Back up your existing `velocity.toml`, `forwarding.secret`, plugins, and logs.
3. Replace the proxy jar with `proxy/build/libs/bVelocity-1.0.0-SNAPSHOT.jar`.
4. Keep using the same `velocity.toml` file name. bVelocity still reads `velocity.toml`.
5. Keep using the same forwarding mode and `forwarding.secret`.
6. Start bVelocity and verify the startup banner shows `bVelocity 1.0.0-SNAPSHOT`.

### Where bVelocity settings live

bVelocity keeps `velocity.toml` in lock-step with upstream Velocity, so every setting bVelocity
owns lives in a separate `bvelocity.toml` that is created on first boot if absent. Specifically:

- **Compression** (`compression-threshold`, `compression-level`) has moved here from
  `velocity.toml`'s `[advanced]` block. If you previously set these in `velocity.toml`, move
  them into `bvelocity.toml`'s `[compression]` section; the old keys in `velocity.toml` are
  ignored.
- **Optimization knobs** (flush consolidation, output-buffer headroom, compression-statistics
  collection) live in `bvelocity.toml`'s `[optimization]` section.

Things you should check after switching:

- `compression-threshold` (in `bvelocity.toml`)
  - If you were happy with upstream behavior, note that bVelocity defaults to `128`, not `256`.
- `compression-level` (in `bvelocity.toml`)
  - Upstream operators often leave this at `-1` and assume “normal” compression.
  - In bVelocity, `-1` resolves to a proxy-preferred level tuned for the ratio sweet spot
    (`native=6 / java=6`), not the upstream zlib default of 6-from-`-1`.
- plugins and forwarding
  - The proxy is still Velocity-based. Existing Velocity plugins and modern forwarding setups should continue to work.
- monitoring expectations
  - `/bv compression` reports protocol-layer savings, not full NIC-level traffic including TCP/IP overhead.

Recommended migration strategy for cautious operators:

- First boot with your existing config unchanged.
- Run `/bv status` and `/bv compression`.
- Observe CPU usage and player-facing latency.
- Then decide whether to keep bVelocity defaults or dial compression back.

## Recommended Settings

For most networks that want lower bandwidth usage without getting reckless, edit
`bvelocity.toml`:

```toml
[compression]
compression-threshold = 128
compression-level = -1
```

and leave these upstream knobs in `velocity.toml`:

```toml
[advanced]
tcp-fast-open = true
log-command-executions = true
log-player-connections = true
```

If you want a more conservative profile (in `bvelocity.toml`):

```toml
[compression]
compression-threshold = 192
compression-level = 9
```

If you want to chase bandwidth reduction aggressively (in `bvelocity.toml`):

```toml
[compression]
compression-threshold = 64
compression-level = 12
```

Be honest about the tradeoff: lower threshold and higher compression level can reduce wire usage further, but they also cost CPU. bVelocity gives you the tooling to measure that tradeoff live instead of guessing.

Note on the `-1` default: bVelocity tunes the auto level to the compression-ratio sweet spot rather than the absolute maximum. On typical Minecraft traffic the ratio plateaus well below level 12 while CPU cost keeps climbing linearly, so `-1` resolves to a level that captures nearly all of the savings at a fraction of the cost. Run `/bv compression benchmark` on your own traffic to confirm, and dial the level up explicitly if your benchmark shows meaningful gains at higher levels.

## Building

Use the Gradle wrapper:

```bash
./gradlew build
```

If you only want the proxy jar:

```bash
./gradlew :velocity-proxy:shadowJar
```

The packaged artifact is:

```text
proxy/build/libs/bVelocity-1.0.0-SNAPSHOT.jar
```

## Measuring Results

Use these commands after startup:

- `/bv status`
  - confirms backend, threshold, and effective level
- `/bv compression`
  - shows aggregate payload and wire savings
- `/bv compression benchmark`
  - runs a synthetic benchmark across compression levels

The benchmark is useful for comparing compression levels. The live stats are what tell you whether your real traffic mix is actually saving bandwidth.

## Example Numbers

One real-world run on `libdeflate (Linux x86_64)` produced the following:

```text
◆ Compression Benchmark
│ Sample=32.00 KiB | Rounds=64 | Backend=libdeflate (Linux x86_64)
│ Level 1 | Encoded=14.05 KiB | Saved=56.09% | Avg=109.93 us
│ Level 2 | Encoded=11.87 KiB | Saved=62.92% | Avg=153.79 us
│ Level 3 | Encoded=11.86 KiB | Saved=62.93% | Avg=185.31 us
│ Level 4 | Encoded=11.86 KiB | Saved=62.93% | Avg=200.54 us
│ Level 5 | Encoded=11.79 KiB | Saved=63.14% | Avg=222.20 us
│ Level 6 | Encoded=11.80 KiB | Saved=63.13% | Avg=339.09 us
│ Level 7 | Encoded=11.80 KiB | Saved=63.11% | Avg=717.75 us
│ Level 8 | Encoded=11.81 KiB | Saved=63.10% | Avg=1.20 ms
│ Level 9 | Encoded=11.81 KiB | Saved=63.10% | Avg=1.25 ms
│ Level 10 | Encoded=11.74 KiB | Saved=63.30% | Avg=1.87 ms
│ Level 11 | Encoded=11.73 KiB | Saved=63.35% | Avg=2.29 ms
│ Level 12 | Encoded=11.73 KiB | Saved=63.35% | Avg=2.28 ms
│ Best size: level 11 -> 11.73 KiB
│ Best speed: level 1 -> 109.93 us
```

And the matching live traffic window reported:

```text
◆ bVelocity Compression Stats
│ Window: 285s
│ Backend: libdeflate (Linux x86_64)
│ Threshold: 128 | Level: auto(native=6/java=6)
│ Packets total=58796 compressed=9634 passthrough=49162
│ Raw payload: 84.70 MiB | Wire bytes: 9.27 MiB
│ Compressed payload: 84.03 MiB | Compressed wire: 8.51 MiB
│ Compressed-only savings: 75.52 MiB (89.87%)
│ Overall wire efficiency: 89.06%
```

Interpret that carefully:

- the synthetic benchmark shows how each compression level behaves on a fixed sample
- the live stats show what your actual traffic mix did over time
- `Overall wire efficiency` is protocol-layer efficiency, not full NIC-level accounting including every transport overhead
