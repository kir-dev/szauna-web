import './App.css'
import {createBrowserRouter, RouterProvider} from "react-router";
import IndexPage from "./pages/index/IndexPage.tsx";
import {ThemeProvider} from "./components/ThemeProvider.tsx";
import {Toaster} from "./components/ui/sonner.tsx";


const router = createBrowserRouter([
  {
    path: '/',
    Component: IndexPage
  }
])

function App() {

  return (
    <ThemeProvider defaultTheme={"system"} storageKey={"szauna-theme"}>
      <RouterProvider router={router}/>
      <Toaster/>
    </ThemeProvider>
  )
}

export default App
