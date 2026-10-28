import {apiClient} from "../lib/api-client.ts";
import type {MeResponse} from "../types/User.ts";

export async function getMe(): Promise<MeResponse | null> {
    const res = await apiClient.get<MeResponse | null>("/v1/user/me")
    return res.data
}