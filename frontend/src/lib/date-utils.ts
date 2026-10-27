export function formatOpeningDate(dateStr: string): string {
    const d = new Date(dateStr)
    return d.toLocaleDateString("hu-HU", {
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "long",
    })
}

export function formatTimeRange(startStr: string, endStr: string): string {
    const start = new Date(startStr).toLocaleTimeString("hu-HU", {
        hour: "2-digit",
        minute: "2-digit",
    })
    const end = new Date(endStr).toLocaleTimeString("hu-HU", {
        hour: "2-digit",
        minute: "2-digit",
    })
    return `${start} – ${end}`
}