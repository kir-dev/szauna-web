import {getMe} from "./auth-api.ts";
import {userStore} from "./user-store.ts";

export async function authMe(){
    try {
        const user = await getMe();
        userStore.set(user);
    }catch {
        userStore.set(null);
    }
}