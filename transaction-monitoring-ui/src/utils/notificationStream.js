const STREAM_URL = 'http://localhost:8080/api/notifications/stream'

let eventSource = null
const listeners = new Set()

const ensureConnection = () => {
  if (eventSource) {
    return eventSource
  }

  eventSource = new EventSource(STREAM_URL)
  eventSource.addEventListener('transaction-evaluated', (event) => {
    let payload = null
    try {
      payload = JSON.parse(event.data)
    } catch {
      payload = null
    }
    listeners.forEach((listener) => listener(payload))
  })

  return eventSource
}

/**
 * Subscribes to real-time "transaction evaluated" notifications pushed by
 * the backend once the rule engine finishes processing a transaction, so
 * views can refresh themselves without a manual reload.
 *
 * Returns an unsubscribe function that should be called on unmount.
 */
export const subscribeToTransactionUpdates = (callback) => {
  ensureConnection()
  listeners.add(callback)
  return () => listeners.delete(callback)
}
