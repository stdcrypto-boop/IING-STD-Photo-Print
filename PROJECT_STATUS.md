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
