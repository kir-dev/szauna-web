import axios from "axios";

// A backend Spring ProblemDetail formátumban küldi a hibákat
export interface ProblemDetail {
    type?: string;
    title?: string;
    status?: number;
    detail?: string;
    instance?: string;
    properties?: Record<string, unknown>;
}

export function getApiErrorMessage(error: unknown, fallback = "Váratlan hiba történt.") {
    if (!axios.isAxiosError<ProblemDetail>(error)) return fallback;

    const data = error.response?.data;
    if (data?.detail) return data.detail;

    // A 401/403 a Swagger szerint body nélkül jön
    switch (error.response?.status) {
        case 401:
            return "Lejárt a munkamenet, jelentkezz be újra.";
        case 403:
            return "Ehhez a művelethez nincs jogosultságod.";
        case 404:
            return "A keresett elem nem található.";
    }

    if (data?.title) return data.title;
    if (!error.response) return "A szerver nem érhető el.";
    return fallback;
}
