import {apiClient} from "../../../lib/api-client.ts";

export async function logoutApi() {
     await apiClient.get<void>("/v1/auth/logout")
}