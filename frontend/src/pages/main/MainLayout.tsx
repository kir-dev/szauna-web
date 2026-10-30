import {Outlet} from "react-router";
import Sidebar from "./components/sidebar/Sidebar.tsx";
import {useState} from "react";
import {Menu} from "lucide-react";

export default function MainLayout() {
    const [isMobileOpen, setIsMobileOpen] = useState(false);
    return (
            <div className="flex h-screen ">
                {/* Itt a különálló Sidebar */}
                <Sidebar isMobileOpen={isMobileOpen} setIsMobileOpen={setIsMobileOpen} />

                {/* Saját tartalmad */}
                <div className="flex-1 flex flex-col overflow-y-auto">

                    {/* Csak mobilon látszó gomb a menü előhívásához */}
                    <button
                        onClick={() => setIsMobileOpen(true)}
                        className="md:hidden p-4 w-fit"
                    >
                        <Menu size={24} />
                    </button>

                    <main className="p-6">
                        <Outlet />
                    </main>
                </div>
            </div>
    )
}