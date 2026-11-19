import type {MeResponse} from "../types/User.ts";

type Listener = () => void;

interface UserState {
    user: MeResponse | null;
    isLoading: boolean;
}

let state: UserState = {
    user: null,
    isLoading: true,
};
const listeners = new Set<Listener>();

export const userStore = {
    get: (): UserState => state,
    set: (user: MeResponse | null) => {
        state = { user, isLoading: false };
        listeners.forEach((l) => l());
    },
    subscribe: (listener: Listener) => {
        listeners.add(listener);
        return () => listeners.delete(listener);
    },
};