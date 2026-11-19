import {apiClient} from "../../../../../lib/api-client.ts";

// ---------- Típusok (a Swagger sémák alapján) ----------

export type OpeningStatus = "SCHEDULED" | "COMPLETED" | "CANCELLED" | "DELETED";
export type IntervalStatus = "ACTIVE" | "CANCELLED" | "DELETED";

export interface IntervalResponse {
    publicId: string;
    intervalStart: string;
    intervalEnd: string;
    participantLimit: number;
    availableSeats: number;
    status: IntervalStatus;
}

export interface OpeningResponse {
    publicId: string;
    openingStart: string;
    openingEnd: string;
    price: number;
    hostedByName: string;
    typeName: string;
    status: OpeningStatus;
    intervals: IntervalResponse[];
    isPrivate: boolean;
}

export interface CreateOpeningRequest {
    openingStart: string; // ISO date-time
    openingEnd: string;
    price: number;
    openingTypeId: number;
    generateDefaultIntervals: boolean;
    isPrivate: boolean;
}

export interface UpdateOpeningRequest {
    openingStart: string;
    openingEnd: string;
    price: number;
    openingTypeId: number;
}

export interface CreateIntervalRequest {
    intervalStart: string;
    intervalEnd: string;
    participantLimit: number;
}

export type UpdateIntervalRequest = CreateIntervalRequest;

export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number; // aktuális oldal (0-tól)
    first: boolean;
    last: boolean;
    numberOfElements: number;
    empty: boolean;
}

export interface GetOpeningsParams {
    status?: OpeningStatus;
    from?: string; // ISO date-time
    to?: string;   // ISO date-time
    page?: number; // 0-tól indul
    size?: number;
    sort?: string; // pl. "openingStart,DESC"
}

const BASE = "/v1/opening";

// ---------- Nyitások ----------

export async function getOpeningsApi(params: GetOpeningsParams = {}, signal?: AbortSignal) {
    const res = await apiClient.get<Page<OpeningResponse>>(BASE, {
        params: {sort: "openingStart,DESC", ...params},
        signal,
    });
    return res.data;
}

export async function getOpeningByIdApi(openingId: string) {
    const res = await apiClient.get<OpeningResponse>(`${BASE}/${openingId}`);
    return res.data;
}

export async function getUpcomingOpeningApi() {
    const res = await apiClient.get<OpeningResponse>(`${BASE}/up-coming`);
    return res.data;
}

export async function createOpeningApi(body: CreateOpeningRequest) {
    const res = await apiClient.post<OpeningResponse>(BASE, body);
    return res.data;
}

export async function updateOpeningApi(openingId: string, body: UpdateOpeningRequest) {
    const res = await apiClient.put<OpeningResponse>(`${BASE}/${openingId}`, body);
    return res.data;
}

export async function updateOpeningStatusApi(openingId: string, status: OpeningStatus) {
    const res = await apiClient.patch<OpeningResponse>(`${BASE}/${openingId}`, {status});
    return res.data;
}

export async function deleteOpeningApi(openingId: string) {
    await apiClient.delete<void>(`${BASE}/${openingId}`);
}

// ---------- Idősávok ----------

export async function createIntervalApi(openingId: string, body: CreateIntervalRequest) {
    const res = await apiClient.post<OpeningResponse>(`${BASE}/${openingId}/interval`, body);
    return res.data;
}

export async function updateIntervalApi(openingId: string, intervalId: string, body: UpdateIntervalRequest) {
    const res = await apiClient.put<OpeningResponse>(`${BASE}/${openingId}/interval/${intervalId}`, body);
    return res.data;
}

export async function updateIntervalStatusApi(openingId: string, intervalId: string, status: IntervalStatus) {
    const res = await apiClient.patch<OpeningResponse>(`${BASE}/${openingId}/interval/${intervalId}`, {status});
    return res.data;
}

export async function deleteIntervalApi(openingId: string, intervalId: string) {
    const res = await apiClient.delete<OpeningResponse>(`${BASE}/${openingId}/interval/${intervalId}`);
    return res.data;
}
