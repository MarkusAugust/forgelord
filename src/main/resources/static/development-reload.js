// Reloads the page when the app has started again. Development only: the page loads this file
// when configuration/DevelopmentReload.kt is there, which it never is in the jar.
//
// The stream says once which start of the app answered. The first answer is remembered. A
// different one means the app has restarted with new code.
let start

function listen() {
    const source = new EventSource("/development/reload")

    source.onmessage = (event) => {
        if (start === undefined) start = event.data
        else if (start !== event.data) location.reload()
    }

    // The browser connects again by itself after most failures. When it has given up, start over.
    source.onerror = () => {
        if (source.readyState === EventSource.CLOSED) setTimeout(listen, 500)
    }
}

listen()
