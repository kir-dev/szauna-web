import {useUser} from "./useUser.ts";
import {Navigate, Outlet, useLocation} from "react-router";

export function RequireAuth({roles}: Readonly<{ roles?: string[] }>){
    const { user, isLoggedIn, isLoading } = useUser();
    const location = useLocation();

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
            </div>
        );
    }
    if (!isLoggedIn) {
        return <Navigate to="/login" replace state={{from: location}}></Navigate>
    }
    if (roles && (!user || !roles.includes(user.role))){
        return <Navigate to="/forbidden" replace></Navigate>
    }

    return <Outlet />
}