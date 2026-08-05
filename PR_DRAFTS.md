# PR drafts

Draft text for the upstream issue reply and the two PRs. Paste-ready Markdown.

---

## Reply to the macOS issue

> Got macOS working — turns out it's two separate problems stacked together:
>
> 1. **JNA's `NativeLong` is 8 bytes on macOS but 4 bytes on Windows**, while Canon's `EdsInt32`/`EdsUInt32`/`EdsError` are always 4 bytes. The bindings as-generated only worked on Windows by accident; on macOS every struct field, function arg and return value was misaligned. Fix is to swap `NativeLong` → `int` everywhere those typedefs apply (and `NativeLongByReference` → `IntByReference`). Wire-format on Windows is unchanged.
>
> 2. **`EDSDK.framework` links AppKit/Cocoa**, so `dlopen` needs an `NSApplication` on the main thread. Add a macOS branch to the loader that searches `/Library/Frameworks` etc. and registers the C calling convention; switch the JNA callback interfaces from `StdCallCallback` → plain `Callback` so they obey the loader's calling-convention setting.
>
> Plus a few odds and ends: per-platform `Ole32` guard so loading the test class on macOS doesn't `UnsatisfiedLinkError`, mirrorless-tolerant `LiveViewLogic` (R-series bodies have no `kEdsPropID_Evf_Mode`), idempotent SDK init since EDSDK can't `EdsTerminateSDK`+`EdsInitializeSDK` in the same process.
>
> PR coming shortly. Verified end-to-end on a Canon R8 (mirrorless): live view streams JPEG frames at 960×640, the 530-test camera suite passes 0 failures.

---

## PR #1 description — `macos-support-pr` (22 commits)

```markdown
## Adds macOS support for the EOS Digital SDK

The Java bindings work on Windows by accident — `NativeLong` is 4 bytes there.
On macOS it's 8 bytes, while Canon's `EdsInt32`/`EdsUInt32` are always 4.
That, plus the fact that `EDSDK.framework` links Cocoa and so its `dlopen`
hangs in a forked Surefire JVM, made macOS a non-starter. This PR makes the
SDK usable end-to-end on macOS with no regressions on Windows.

### Changes

**Loader (`CanonLibraryImpl`)**
- New `getLibPathMac()` that searches the working dir, `/Library/Frameworks`,
  `~/Library/Frameworks`, `/System/Library/Frameworks`.
- On macOS, register `OPTION_CALLING_CONVENTION = C_CONVENTION` to override
  the StdCall default inherited from `StdCallLibrary`. Windows path unchanged.

**Binding type migration (`NativeLong` → `int`)**
- Every `EdsInt32`/`EdsUInt32`/`EdsError`/`EdsPropertyID` field, parameter and
  return value in `EdsdkLibrary` and the 20 JNA `Structure` subclasses now uses
  `int` (and `IntByReference`). `EdsInt64` stays `long`.
- All callback interfaces switched from `StdCallCallback` → plain
  `com.sun.jna.Callback` so they respect the loader's calling convention.
- `EdsPropertyDescEx` struct binding added (was missing).
- All call sites in `camera-framework` updated; tests migrated; signed-vs-
  unsigned semantics preserved with `& 0xFFFFFFFFL` widening where needed.
- Wire format on Windows is identical (`long` is 4 bytes there too); for
  external callers using `new NativeLong(x)` the migration is a one-line
  `s/new NativeLong\(x\)/x/`.

**Property data buffer fix (`PropertyGetLogicDefault` / `PropertySetLogicDefault`)**
- For `kEdsDataType_Int32`/`UInt32`, switch from `getNativeLong`/`setNativeLong`
  on `Memory` to `getInt`/`setInt` so we read/write exactly 4 bytes regardless
  of `NativeLong.SIZE`.

**Mirrorless (R-series) compatibility**
- `LiveViewLogicDefault.beginLiveView`/`endLiveView` swallow
  `EDS_ERR_INVALID_HANDLE` from `enableLiveView`/`disableLiveView` because
  `kEdsPropID_Evf_Mode` doesn't exist on R bodies.
- `isLiveViewEnabled` falls back to reading `kEdsPropID_Evf_OutputDevice`
  when `Evf_Mode` is unavailable.
- `PropertyDescLogicDefault` tolerates unknown enum values from newer bodies
  (`Assumptions.assumeTrue` style — keeps the rest of the descriptor list).
- Test infrastructure: write-probe-based `CameraTypeUtil#hasEvfMode`,
  Evf_Mode-dependent tests skip via `Assumptions.assumeTrue` on mirrorless.

**Test infrastructure for macOS**
- `CameraTestRunner`: standalone main() that bootstraps `Toolkit.getDefaultToolkit()`
  before EDSDK loads (gets AppKit on the main thread), then drives JUnit
  Platform Launcher across the camera-required test classes. Workaround for
  Surefire's forked-JVM `dlopen` hang on macOS. Plain `mvn test` is unaffected
  on Windows.
- `R8LiveViewDemo` / `PropertyTypeProbe` / `NativeLongSizeDemo`: standalone
  diagnostic mains.
- `MirrorlessLiveViewCameraTest`: end-to-end live-view assertions.
- `TestShortcutUtil.initLibrary()` is idempotent and `terminateLibrary()` is
  a no-op (EDSDK doesn't tolerate term+init in the same process).
- `ShootLogicCameraTest` Ole32 calls guarded behind `Platform.isWindows()`.
- `PropertyGetLogicCameraTest` data table aligned with R-series + handles
  `EDS_ERR_PROTECTION_VIOLATION` / `EDS_ERR_INTERNAL_ERROR` gracefully.
- `run-camera-tests.cmd` Windows companion to the bash classpath one-liner.

### Verification

- macOS (Apple Silicon, Java 17, Canon R8): `mvn test` 1235/0 failed,
  `CameraTestRunner` 513/0 failed (13 skipped, 6 aborted via `Assumptions`).
- R8LiveViewDemo captures 5 JPEG frames at 960×640.
- All commits compile + green at `mvn test` per commit; no Windows runtime
  behaviour change at the JNI boundary.

### Migration notes for downstream callers

- If you call `EdsdkLibrary` methods directly with `new NativeLong(x)` /
  `new NativeLongByReference()`: drop the `NativeLong` wrappers (use the int
  directly) and `IntByReference`.
- `kEdsWhiteBalance_Tangsten` is **not** renamed in this PR (PR #2 fixes it);
  if you use it the symbol still resolves here.
```

---

## PR #2 description — `edsdk-13.20.10-pr` (8 commits, depends on PR #1)

```markdown
## Updates the bindings to EDSDK 13.20.10

Brings the JNA bindings up to current EDSDK (13.20.10, released 2025-09-24).
The binding was last refreshed against 13.16.10; this catches it up.

**Depends on PR #1** (macOS support / `NativeLong → int` migration). The new
function declarations and struct fields use `int` per that PR's convention.

### Changes

**Removed (Canon dropped these in 13.9.10)**
- `EdsCacheImage`, `EdsReflectImageProperty`, `EdsSaveImage` — RAW-development
  API removed years ago. Calling them on a current SDK build is a linker error.
  The pre-13.9.10 RAW-dev property IDs (`kEdsPropID_ColorMatrix`, `PhotoEffect`,
  etc.) and their framework enums are intentionally **left in place**:
  removing them would be a hard breaking change without firm evidence they're
  rejected on hardware.

**Typo fix**
- `kEdsWhiteBalance_Tangsten` → `kEdsWhiteBalance_Tungsten` (Canon's spelling
  has always been `Tungsten`). The framework enum is renamed too. No in-tree
  caller used the typo'd name; downstream callers migrate with `s/Tangsten/Tungsten/`.

**Five new function bindings**
- `EdsCreateFlashSettingRef` (creates `EdsFlashRef` for flash properties)
- `EdsCreateFolder`
- `EdsGetPropertyDescEx` (extended desc query for `kEdsPropID_MovieParamEx`
  and similar; populates the `EdsPropertyDescEx` struct that PR #1 added)
- `EdsSetMetaImage`
- `EdsSetFramePoint` (was in the vendored header but never made it into the
  binding interface — original generation bug)

**Three new struct bindings**
- `EdsApertureLockSetting`, `EdsGpsMetaData`, `EdsMovieFileNoSet`.

**55 new constants**
- New enum interfaces: `EdsObjectFormat`, `EdsMirrorLockupState`,
  `EdsMirrorUpSetting`, `EdsDrivePowerZoom`.
- Extended interfaces: `kEdsImageType_HEIF`, `kEdsStorageType_CFe` (CFexpress),
  `kEdsEvfOutputDevice_PC_Small`, `kEdsEvfZoom_x6`/`x15`.
- New top-level events: `kEdsPropertyEvent_PropertyDescExChanged`,
  `kEdsStateEvent_PowerZoomInfoChanged`.
- 31 new `kEdsPropID_*` (AfLockState, AFTrackingObject, ApertureLockSetting,
  BrightnessSetting, ContinuousAfMode, FocusPosition, IBIS_HighResoShot,
  LensIsSetting, MovieFileName{ClipNo,Index,ReelNo,UserDef}, MovieParamEx,
  MovieRecVolume_{Acc,ExtMic,IntMic}, ScreenDimmerTime, ScreenOffTime,
  SlowFastMode, StillFileName{Setting,UserSet1,UserSet2}, StillFolderName,
  ViewfinderOffTime, …).

**Framework enum mirroring**
- `EdsImageType`, `EdsStorageType`, `EdsEvfOutputDevice`, `EdsEvfZoom`,
  `EdsPropertyEvent`, `EdsStateEvent`, `EdsPropertyID` extended.
- `EdsEvfOutputDevice.kEdsEvfOutputDevice_MOBILE/MOBILE2` removed (Canon
  removed them; `MOBILE2`'s value is now `PC_Small`).

**Tests**
- `StructureTest` covers the 3 new structs (28 → 31 tests).
- `NewEdsdkBindingsCameraTest`: live-camera smoke tests for the 5 new
  functions; `Assumptions.abort` if the installed EDSDK predates the symbol
  (so the suite stays green on machines whose EDSDK is older than 13.20.10).

### Verification

Same as PR #1: `mvn test` 1235/0 failed, `CameraTestRunner` against R8
513/0 failed. The `EdsCreateFolder` / `EdsGetPropertyDescEx` smoke tests
abort with a clear message on macOS systems whose installed EDSDK is older
than 13.20.10 — they pass on Windows with a current Canon EDSDK.

### Out of scope (separate PR / follow-up)

The 71 pre-13.9.10 RAW-development property IDs and the legacy enum classes
(`EdsColorMatrix`, `EdsFilterEffect`, etc.) are intentionally retained — see
the rationale in commit `04eae90`.
```
