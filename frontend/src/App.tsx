import './App.css'
import {createBrowserRouter, Navigate, redirect, RouterProvider} from "react-router";
import IndexPage from "./pages/index/IndexPage.tsx";
import {ThemeProvider} from "./components/ThemeProvider.tsx";
import {Toaster} from "./components/ui/sonner.tsx";
import ForbiddenPage from "./pages/forbidden/ForbiddenPage.tsx";
import {RequireAuth} from "./auth/RequireAuth.tsx";
import MainLayout from "./pages/main/MainLayout.tsx";
import {type ReactNode, useEffect} from "react";
import {authMe} from "./auth/auth-service.ts";
import DashboardPage from "./pages/main/pages/dashboard/DashboardPage.tsx";
import {userStore} from "./auth/user-store.ts";
import {CalendarRange, LayoutDashboard} from "lucide-react";
import BookingPage from "./pages/main/pages/booking/BookingPage.tsx";
import ProfilePage from "./pages/main/pages/profile/ProfilePage.tsx";
import {logout} from "./pages/main/main-service.ts";

export interface NavItem {
    path: string;
    label: string;
    element: ReactNode;
    roles?: string[];
    icon: ReactNode;
}

export const navItems: NavItem[] = [
    {
        path: "dashboard",
        label: "Dashboard",
        element: <DashboardPage/>,
        roles: ["USER", "ADMIN", "TRAINEE", "SAUNA_MATER"],
        icon: <LayoutDashboard />
    },
    {
        path: "bookings",
        label: "Foglalások",
        element: <BookingPage/>,
        roles: ["USER", "ADMIN", "TRAINEE", "SAUNA_MATER"],
        icon: <CalendarRange />
    }
];

const router = createBrowserRouter([
    {
        path: '/',
        Component: IndexPage
    },
    {
        path: '/forbidden',
        Component: ForbiddenPage
    },
    {
        path: '/login',
        loader: () => {
            window.location.href = "http://localhost:8080/oauth2/authorization/authsch" //Check if is logged in!
        }
    },
    {
        element: <RequireAuth/>,
        children: [
            {
                element: <MainLayout/>,
                children: navItems.map((item) => ({
                    element: <RequireAuth roles={item.roles}/>,
                    children: [{path: item.path, element: item.element}
                    ]
                })).concat({element: <RequireAuth/>, children: [{path: "profile", element: <ProfilePage/>}]})
            }
        ]
    },
    {
        path: '*',
        element: <Navigate to={"/"} replace/>
    }
])

function App() {

    useEffect(() => {
        authMe();
    }, [])

    return (
        <ThemeProvider defaultTheme={"system"} storageKey={"szauna-theme"}>
            <RouterProvider router={router}/>
            <Toaster/>
        </ThemeProvider>
    )
}

export default App
