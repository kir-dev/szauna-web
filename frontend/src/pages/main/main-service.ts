import {logoutApi} from "./api/main-api.ts";
import {userStore} from "../../auth/user-store.ts";


export async function logout() {
    try {
        await logoutApi()
    } catch (err) {

    } finally {
        userStore.set(null);
    }
}