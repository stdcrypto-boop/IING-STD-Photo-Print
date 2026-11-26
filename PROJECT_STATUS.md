# Status
0.1: Android scaffold and real IPP Get-Printer-Attributes connectivity probe via manual URI. Not a printing app yet. Build and physical printer tests pending.
Next: DNS-SD discovery, parse printer attributes, validate formats and custom F4 media-col, implement direct Print-Job with supported formats, PDF/image rendering and preview. Never claim physical print success based on build alone.

Added DNS-SD discovery for first _ipp._tcp printer, with manual URI fallback. Build and hardware discovery pending. Maintenance target: investigate HP-specific supported cleaning/alignment commands; no unverified commands or dummy controls.

Added IPP attribute parsing and scrollable capability report (formats, named media, media types, color, quality, operations and state). Nested media-col collections are not yet interpreted; custom F4 and Print-Job remain pending. Build and physical tests pending.

Direct Print-Job implemented for native PDF/JPEG with strict attribute fidelity, A4/F4/Legal/4R selection and confirmation. F4 uses custom keyword only; media-col and raster fallback still pending. Printer may reject unsupported formats/media or chunked HTTP. No physical validation yet. Custom launcher icon registered.

F4 now encoded using IPP media-col with 21000 x 33000 hundredths mm. Added optional quality, color, media type and copies. Fixed fidelity attribute name length. Strict fidelity remains; actual device support and build pending. Next: preview, raster fallback, supported-option filtering and maintenance investigation.

Added bounded JPEG preview (Android 9+) and first-page PDF preview with page count. Preview is content-only, not exact print layout; all PDF pages still sent. Build and hardware validation pending.

Added symmetric media-col margins (0/3/5/10 mm) and print-scaling fit/fill/none, default omitted. Backward-compatible method overload avoids transient compile mismatch. Device may reject unsupported settings; preview remains content-only. Build and hardware validation pending.

Added PDF single page or contiguous page range via IPP page-ranges. Requires printer support; bounds against actual PDF page count and preview navigation pending. Build and hardware tests pending.

PDF preview now navigates pages and starts at selected range; actual page count checked before submitting job. Build/device tests pending. Next prioritize real print compatibility and maintenance before editor expansion.

Added fresh printer format preflight before Print-Job; rejects explicitly unsupported MIME types. Raster conversion fallback and HP maintenance commands still pending. No physical verification.

JPEG-to-PDF fallback implemented for printers advertising PDF but not JPEG, with fit/fill and selected paper. Default margin 5 mm; decode sample size 2; original-size fallback not supported. Temporary PDF deleted after send. PDF-to-raster and physical validation pending.

2026-11-26: JPEG fallback now uses adaptive bounded decoding (12 MP / 4200 px), explicit PdfDocument cleanup, and Android 9+ ImageDecoder. IPP job upload uses fixed Content-Length with private disk spool (c209d63). Added advertised PWG raster color types and resolution attributes, printer default/supported resolutions, and decoding of IPP resolution and boolean values (4cc8efc). Previous commit workflow lookup and combined status returned empty; no build or physical test verified. Next: implement bounded PWG raster encoding with supported color/resolution and PDF page rendering, then verify via fixtures and CI; maintenance commands remain unverified.

Implemented experimental PDF-to-PWG Raster fallback for explicitly advertised 300 dpi srgb_8, default margins, fit/default scaling and non-monochrome. Selected pages rendered individually; temporary files removed. Requires independent raster decoder fixture validation, build and physical test before release. Other modes deliberately rejected rather than silently changed.

User reported green build for previous raster stage. Added explicit monochrome sgray_8 encoding and advertised square 300/200/150 dpi selection. New build and independent raster validation pending; physical printing unverified. Next validate raster headers and row encoding with independent fixtures.

User reported green for grayscale/resolution stage. Added repeated-pixel run encoding for PWG rows, preserving literal runs and single pixels. Intended to reduce spool size on blank/text pages. Independent decoder fixtures and new CI build remain pending; no physical test claimed.

Added Java row encoder round-trip fixtures driven by Python and CI gate before APK build. Covers RGB/grayscale, repeated and literal pixels, boundary lengths and deterministic random data. CI result pending; header validation and physical test remain open.

Extracted PWG header generation and extended CI fixtures to verify header signature, dimensions, resolution, color layout and page count across A4/F4/Legal/4R. Tests are structural, not independent standards certification. CI and physical test pending.

Added JPEG 90-degree rotation in preview and actual print payload on Android 9+, bounded decode and JPEG quality 95. Unedited JPEG and PDF paths unchanged. Preview view rotation may clip in constrained layout; next improve fit and add crop. CI and device testing pending.

Added shared bitmap rotation and center crop ratios for preview and actual JPEG payload, with FIT_CENTER preview rather than rotating the view. Crop is centered, not freehand; ratios are not physical print sizes. CI/device testing pending. Next physical photo layout.

Added single passport photo PDF layout (20x30, 30x40, 40x60 mm) top-left, minimum 5 mm inset, forced no scaling. Requires printer native PDF and no-scaling support; raster fallback intentionally rejects none. Preview shows crop only, not full sheet. Physical dimensions and CI pending verification; next sheet preview and repeat layout.

Added repeated same-photo layout 1–20 copies per sheet, configurable gap 0/2/3/5/10 mm, shared physical cell geometry for PDF and sheet preview, overflow rejection and scrollable editor. Invalid layout clears preview and print reports maximum. Native PDF/no scaling still required; CI and physical size verification pending. Multi-image collage not yet implemented.

Added live sheet layout summary and visible overflow error; print confirmation now validates layout before dismissal and retains editor on invalid count. Recycles temporary edited bitmap on invalid layout. CI status not available through PR-only workflow lookup; physical test pending.

Added horizontal and vertical crop anchor sliders (0–100%, centered default) shared by JPEG and passport PDF preview/print. Fixed aspect presets remain; freeform rectangle not implemented. CI and device tests pending.

Corrected compile-time parameter mismatch introduced by crop anchor change: passportCells remains five arguments; passportInput accepts both crop anchors and forwards them to photoInput. Reviewed call sites. Combined CI status returned no checks; build and physical tests remain unverified. Next add regression coverage for layout/crop geometry before further features.

Added CI regression fixtures extracting actual passport layout methods, checking 240 combinations of paper, size, gap and margin, physical dimensions, spacing and overflow rejection. Fixtures not executed in this session; CI and hardware unverified. Crop geometry tests still pending.

Extracted production cropBounds and added 1575 CI fixture combinations covering tiny, portrait, landscape, square images, all ratios and anchors. Bitmap rotation itself not tested by JVM fixtures. Combined status for previous commit checked; no physical test or CI success claimed. Next collage or printer compatibility.

Added experimental actual-size PDF-to-PWG raster conversion using dpi/72 transform, top-left origin and paper dimension guard allowing only PDF point rounding tolerance. Default printer margins required; forwards strict print-scaling=none, so unsupported printers may reject. Prior combined status empty; CI and physical size unverified. Next verify raster and printer capabilities.

Added JPEG-to-PWG raster fallback via bounded photo PDF intermediate, preserving fit/fill with default margin, actual-size raster transform, strict no-scaling submission and temporary file cleanup. Native JPEG/PDF preferred. Explicit margin and JPEG original-size not supported on this fallback. Prior combined status empty; CI and physical printer verification pending. Next capability validation and collage.

Added preflight checks for explicitly advertised color mode, media type, print quality and final print scaling. Missing attributes do not cause rejection; strict IPP fidelity remains. Scaling validated after format conversion so software fit/fill remains possible. Previous combined status empty; new build and physical test unverified. Next capability fixtures and collage.

Added CI capability fixtures using production capability and requireSupported methods: supported options, explicit rejection, exact token matching, missing attributes and diagnostic content. Previous combined status empty; fixtures and APK build not executed in this session. Next multi-image collage.

Implemented separate multi-JPEG collage picker (1–20 images), shared passport dimensions and gap/overflow checks, center crop, bounded 1200px decoding, actual-size PDF generation and IPP submission. One sheet copy, no visual collage preview or per-photo editing yet; next add preview before printing. Previous combined status empty; build and physical tests unverified.

Added scrollable collage dialog with live 900px full-sheet preview, shared physical cell geometry and center crop, bounded thumbnail decoding off UI thread, stale-result guard and disabled print until valid preview. Per-photo editing still pending. Prior combined status empty; build and device tests unverified.

Added per-photo selector, independent 90-degree rotation and horizontal/vertical crop anchors in collage. Preview and print use immutable snapshots of edit arrays; slider refresh on release. Fixed aspect crop only; reordering pending. Prior combined status empty; CI and device tests unverified.

Added adjacent collage reorder controls, swapping URI and associated rotation/crop anchors together. Preview and print workers snapshot URI order and edits to avoid concurrent reorder mismatch. Prior combined status empty; CI and physical tests unverified. Next improve UI and preview performance.

Optimized collage preview with 150ms debounce, single background executor, cancellation of stale tasks, interruption checks between images and executor shutdown on dialog dismissal. Existing generation guard retained. No measured performance or build verification; prior combined status empty. Next UI polish and persistent printer settings.

Main screen UI polish: teal palette, density-aware spacing, grouped connection/settings/print sections, rounded ripple action buttons and discovery moved beside connection test. Visual/device and CI verification pending. Next persist settings.

Persisted six print setting selections locally with bounds checking. User screenshot confirms builds 83–86 green; new build pending. Copies and page range intentionally reset to avoid accidental repeated jobs. Next maintenance capability and update flow.

Added settings reset, read-only live IPP diagnostics with explicit maintenance limitations, release page navigation and installed version label. Release navigation is not an in-app updater; no release or signing continuity verified. Build pending.

Set candidate version 0.2/versionCode 2 and added RELEASE_CHECKLIST.md for build, signing, upgrade, physical printer, layout and offline acceptance. Candidate not released; debug signing continuity and actual maintenance protocol unresolved. No latest CI verification.
