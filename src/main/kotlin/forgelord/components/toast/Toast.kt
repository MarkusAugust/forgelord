package forgelord.components.toast

import io.github.markusaugust.streamlord.html.js
import kotlinx.html.FlowContent
import kotlinx.html.id
import no.lotek.Lt
import no.lotek.html.lt
import no.lotek.html.ltToast

/**
 * Where toasts appear. Once per page.
 *
 * `Lt.toast()` gives it `data-ignore-morph`, so a patch from the server leaves the messages alone.
 */
fun FlowContent.toastRegion(regionId: String) {
    ltToast {
        lt(Lt.toast().host)
        id = regionId
    }
}

/**
 * The script that shows [message] as a danger toast in the region [regionId], for
 * `executeScript` in a stream.
 *
 * A toast tells the one who clicked that something went wrong. It is not state to keep in a
 * signal: it happens, and then it is gone. `js()` quotes both values as JavaScript strings.
 */
fun showDangerToast(
    regionId: String,
    message: String,
): String = "document.getElementById(${js(regionId)}).show(${js(message)}, {color: 'danger', duration: 0})"
