import type {OpeningResponse} from "../../../types/Opening.ts";
import {apiClient} from "../../../lib/api-client.ts";

export async function getCurrentOpening(): Promise<OpeningResponse | null> {
    const res = await apiClient.get<OpeningResponse | null>("/v1/opening/up-coming")
    return res.data
}