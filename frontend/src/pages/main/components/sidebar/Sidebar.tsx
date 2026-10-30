import {LogOut, PanelLeftOpen, PanelRightOpen} from 'lucide-react';
import {Link, NavLink, useLocation, useNavigate} from "react-router";
import {useEffect, useState} from "react";
import logoImg from "../../../../assets/big_logo.png";
import ThemeToggle from "../../../../components/ThemeToggle.tsx";
import {Button} from "../../../../components/ui/button.tsx";
import {useUser} from "../../../../auth/useUser.ts";
import {Badge} from "../../../../components/ui/badge.tsx";
import {navItems} from "../../../../App.tsx";
import {logout} from "../../main-service.ts";


const STORAGE_KEY = "sidebar";
export default function Sidebar({isMobileOpen, setIsMobileOpen}) {
    const [isPinned, setIsPinned] = useState(() => {
        try {
            const saved = localStorage.getItem(STORAGE_KEY);
            return saved !== null ? JSON.parse(saved) : true;
        } catch {
            return true;
        }
    });

    const navigate = useNavigate();

    const handleLogout = async () => {
        navigate("/", {replace: true});
        await logout();
    };

    const {pathname} = useLocation();

    const {user, isLoggedIn, isLoading} = useUser();

    const visivleMenu = navItems.filter(
        (item) => !item.roles || (user && item.roles.includes(user.role))
    )


    const [isHovered, setIsHovered] = useState(false);
    const [ignoreHover, setIgnoreHover] = useState(false);

    const location = useLocation();

    useEffect(() => {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(isPinned));
        } catch (error) {
        }
    }, [isPinned]);

    useEffect(() => {
        if (window.innerWidth < 768 && setIsMobileOpen) {
            setIsMobileOpen(false);
        }
    }, [location.pathname, setIsMobileOpen]);


    const handleToggleSidebar = () => {
        if (window.innerWidth < 768) {
            setIsMobileOpen(false);
        } else {
            const nextPinned = !isPinned;
            setIsPinned(nextPinned);

            if (!nextPinned) {
                setIsHovered(false);
                setIgnoreHover(true);
            }
        }
    };

    const isExpanded = isPinned || (isHovered && !ignoreHover) || isMobileOpen;

    return (
        <>
            {isMobileOpen && (
                <div
                    className="fixed inset-0 z-40 bg-black/60 backdrop-blur-xs transition-opacity md:hidden"
                    onClick={() => setIsMobileOpen(false)}
                />
            )}

            <div
                className={`hidden md:block shrink-0 transition-all duration-300 ease-in-out ${
                    isPinned ? 'w-72' : 'w-17'
                }`}
            />

            <aside
                onMouseEnter={() => {
                    if (window.innerWidth >= 768 && !ignoreHover) {
                        setIsHovered(true);
                    }
                }}
                onMouseLeave={() => {
                    if (window.innerWidth >= 768) {
                        setIsHovered(false);
                        setIgnoreHover(false);
                    }
                }}
                className={`
          fixed inset-y-0 left-0 z-50
          flex flex-col justify-between
          transition-all duration-300 ease-in-out select-none
          bg-background
          ${!isMobileOpen ? 'border-r dark:border-r-gray-800 border-r-gray-300' : ''}
          ${isMobileOpen ? 'translate-x-0 w-72' : '-translate-x-full md:translate-x-0'}
          ${isExpanded ? 'md:w-72' : 'md:w-17'}
          ${!isPinned && isExpanded ? 'md:shadow-2xl dark:md:shadow-black/80 md:shadow-gray-300/80 border-r border-gray-200 dark:border-gray-800 ' : ''}
        `}
            >
                <div className="p-3 flex flex-col gap-4">

                    <div className="flex items-center justify-between">
                        <div className="relative group h-10">
                            <button
                                onClick={handleToggleSidebar}
                                className="p-2.5 rounded-full duration-100 transition-all dark:hover:bg-gray-700/40 hover:bg-gray-200 flex items-center justify-center  cursor-pointer"
                            >
                                {(!isExpanded || !isPinned) ? <PanelLeftOpen size={16}/> : (
                                    <PanelRightOpen size={16}/>)}
                            </button>

                        </div>

                        <div
                            className={`flex items-center gap-2 pr-2 transition-opacity duration-200 ${isExpanded ? 'opacity-100' : 'opacity-0 hidden'}`}>
                            <img
                                src={logoImg}
                                alt="SCHSzauna"
                                className="h-10 object-contain hover:scale-105 transition-transform"
                            /></div>
                    </div>

                </div>

                <div
                    className={`
            flex-1 overflow-y-auto px-3 my-2 transition-opacity duration-200 overflow-x-hidden
          }
          `}
                >
                    <nav className={"flex flex-col gap-2"}>
                        {visivleMenu.map((menu) => {
                                const targetPath = `/${menu.path}`;
                                const isActive = pathname.startsWith(targetPath) || targetPath === pathname;
                                return (
                                    <NavLink key={menu.path} to={menu.path}><Button
                                        variant={isActive ? 'default' : 'secondary'}
                                        className={`w-full flex h-10 ${isExpanded ? 'justify-start' : 'justify-center'} gap-2`}>{menu.icon}{isExpanded && menu.label}</Button></NavLink>
                                )
                            }
                        )}
                    </nav>
                </div>

                <div className="p-3 border-t border-gray-200 dark:border-gray-800 space-y-2">
                    <ThemeToggle withExpanded={true} isExpanded={isExpanded}/>

                    <Link to={"/profile"} className={"block"}>
                        <div className={"flex items-center gap-4"}>
                            <div>
                            <span
                                className={"flex justify-center items-center h-10 w-10 rounded-full bg-primary text-white"}>{user?.displayName.at(0)}</span>
                            </div>
                            {isExpanded && <div
                                className={`transition-opacity duration-300 ${isExpanded ? 'opacity-100 ' : 'opacity-0 hidden'}`}>
                                <h1>{user?.displayName}</h1>
                                <Badge variant={"secondary"}>{user?.role}</Badge>
                            </div>}
                        </div>
                    </Link>
                    <Button onClick={handleLogout} className={"w-full"}>
                        {isExpanded ? <span
                                className={`transition-opacity duration-300 ${isExpanded ? 'opacity-100 ' : 'opacity-0 hidden'}`}>Kijelentkezés</span> :
                            <LogOut/>}
                    </Button>
                </div>
            </aside>
        </>
    );
}