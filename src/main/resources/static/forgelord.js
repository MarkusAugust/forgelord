// Registers LO-TEK's web components, once for the whole app.
//
// This is a file, not a <script> written inline in the page, for two reasons:
//
// 1. Writing JavaScript into the page from Kotlin takes kotlinx.html's `unsafe { }`, which skips
//    escaping. The project allows `unsafe` in one place only, the page's CSS, so that nobody has
//    to judge in review whether a given use is safe.
// 2. The Content-Security-Policy allows scripts from this origin. An inline script would need
//    the nonce as well; a file needs nothing.
//
// The version in the path is LO-TEK's. PageTest fails if it differs from LotekWebJar.REGISTER,
// so update it here when you update LO-TEK in build.gradle.kts.
import { defineLt } from "/webjars/lo-tek/0.45.0/dist/register.js"

defineLt()
