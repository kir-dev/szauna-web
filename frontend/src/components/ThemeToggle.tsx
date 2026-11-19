import {Button} from "./ui/button.tsx";
import {Laptop, Moon, Sun} from "lucide-react";
import {useTheme} from "./ThemeProvider.tsx";

export default function ThemeToggle({withExpanded, isExpanded}: {withExpanded?: boolean, isExpanded?: boolean}) {
    const {theme, setTheme} = useTheme()

    const cycleTheme = ()=>{
        if (theme == "system"){
            setTheme("light")
        }else if (theme == "light"){
            setTheme("dark")
        }else {
            setTheme("system")
        }
    }

    if (!withExpanded){
        return (
            <Button type={'button'} variant={'outline'} className="w-8 h-8 flex justify-center items-center" onClick={cycleTheme}>
                {theme === "system" && <Laptop className="h-4 w-4"/> }
                {theme === "light" && <Sun className="h-4 w-4"/> }
                {theme === "dark" && <Moon className="h-4 w-4"/> }
            </Button>
        )
    }

    return (
        <Button type={'button'} variant={'outline'} className="w-full h-10 flex  items-center" onClick={cycleTheme}>
            {theme === "system" && <Laptop className="h-4 w-4"/> }
            {theme === "light" && <Sun className="h-4 w-4"/> }
            {theme === "dark" && <Moon className="h-4 w-4"/> }
            {isExpanded&& "Megjelenés"}
        </Button>
    )

}