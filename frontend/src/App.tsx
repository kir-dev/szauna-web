import './App.css'
import {createBrowserRouter, Navigate, RouterProvider} from "react-router";
import IndexPage from "./pages/index/IndexPage.tsx";
import {ThemeProvider} from "./components/ThemeProvider.tsx";
import {Toaster} from "./components/ui/sonner.tsx";
import ForbiddenPage from "./pages/forbidden/ForbiddenPage.tsx";
import {RequireAuth} from "./auth/RequireAuth.tsx";
import MainLayout from "./pages/main/MainLayout.tsx";
import {useEffect} from "react";
import {authMe} from "./auth/auth-service.ts";

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
                children: [
                    {path: 'dashboard', element: <h1>Dashboard</h1>}
                ]
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
