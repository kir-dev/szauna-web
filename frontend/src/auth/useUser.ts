import {useSyncExternalStore} from "react";
import {userStore} from "./user-store.ts";

export function useUser(){
    const { user, isLoading } = useSyncExternalStore(userStore.subscribe, userStore.get, userStore.get);
    return {
        user,
        isLoading,
        isLoggedIn: !!user,
    };
}