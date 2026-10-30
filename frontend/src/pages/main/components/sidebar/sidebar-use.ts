import {createContext, useContext, useEffect, useState} from "react";

const STORAGE_KEY = 'sidebar:collapsed';

export type SidebarState = {
    collapsed: boolean;
    setCollapsed: (collapsed: boolean) => void;
    toggle: ()=> void;
    mobileOpen: boolean;
    setMobileOpen: (mobileOpen: boolean) => void;
}

export function useSidebarState() :SidebarState{
    const [collapsed, setCollapsedState] = useState<boolean>(()=>{
        if (typeof window === "undefined") return false;
        try {
            return window.localStorage.getItem(STORAGE_KEY) === "1";
        }catch{
            return false;
        }
    })

    const [mobileOpen, setMobileOpen] = useState(true);
    const setCollapsed = (collapsed: boolean) => {
        setCollapsedState(collapsed);
        window.localStorage.setItem(STORAGE_KEY, collapsed?"1":"0");
    }

    const toggle = () => setCollapsed(!collapsed);

    useEffect(()=>{
        const onStorage = (e: StorageEvent) => {
            if(e.key === STORAGE_KEY){
                setCollapsedState(e.newValue === "1")
            }
        }

        window.addEventListener("storage", onStorage);
        return () => window.removeEventListener("storage", onStorage);
    }, [])

    return {collapsed, setCollapsed, toggle, mobileOpen, setMobileOpen};
}

export const SidebarContext = createContext<SidebarState| null>(null);

export function useSideBar(){
    const context = useContext(SidebarContext);
    if(!context){
        throw new Error("useSidebarState must be used within SidebarContext");
    }
    return context;
}