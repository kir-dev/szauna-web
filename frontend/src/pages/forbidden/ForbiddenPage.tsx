import {Link} from "react-router";
import {Button} from "../../components/ui/button.tsx";

export default function ForbiddenPage() {
    return (
        <>
            <main className="flex-1 flex items-center justify-center py-16 min-h-screen px-4">
                <div className="max-w-md w-full text-center space-y-6">

                    <div
                        className="font-mono text-5xl md:text-6xl tracking-wider select-none text-muted-foreground animate-pulse">
                        ¯\_(ツ)_/¯
                    </div>

                    <div className="space-y-2">
                        <div
                            className="inline-block rounded-full bg-destructive/10 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-destructive">
                            403 • Hozzáférés megtagadva
                        </div>
                        <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight">
                            Itt túl meleg a helyzet!
                        </h1>
                        <p className="text-muted-foreground text-sm md:text-base">
                            Ehhez az oldalhoz nincs megfelelő jogosultságod.
                        </p>
                    </div>

                    {/* Akció gombok */}
                    <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
                        <Link to="/" className="w-full sm:w-auto">
                            <Button variant="default" className="w-full cursor-pointer">
                                Vissza a pihenőbe (Főoldal)
                            </Button>
                        </Link>
                        <Button
                            variant="secondary"
                            className="w-full sm:w-auto cursor-pointer"
                            onClick={() => window.history.back()}
                        >
                            Előző oldal
                        </Button>
                    </div>
                </div>
            </main>
        </>
    )
}