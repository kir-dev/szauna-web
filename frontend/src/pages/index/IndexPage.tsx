import KirDevFooter from "../../components/KirDevFooter.tsx";
import NewsCard from "./components/NewsCard.tsx";
import Faq from "./components/Faq.tsx";
import {CurrentOpeningCard} from "./components/CurrentOpeningCard.tsx";
import {Link} from "react-router";
import {Button} from "../../components/ui/button.tsx";
import ThemeToggle from "../../components/ThemeToggle.tsx";
import logoImg from "../../assets/big_logo.png"
import {useUser} from "../../auth/useUser.ts";

export default function IndexPage() {
    const {user, isLoggedIn, isLoading} = useUser();

    return (<>
            <header className="sticky top-0 z-50 border-b border-border/60 bg-background/80 backdrop-blur-md">
                <div
                    className="container mx-auto max-w-11/12 md:max-w-3/4 gap-2 h-16 flex items-center justify-between">
                    <Link to="/" className="flex items-center gap-3">
                        <img
                            src={logoImg}
                            alt="SCHSzauna"
                            className="h-10 object-contain hover:scale-105 transition-transform"
                        />
                    </Link>


                    <div className="flex items-center gap-3">
                        <ThemeToggle />
                        {(() => {
                            if (isLoading) {
                                return (
                                    <Button disabled={true} variant="default" className="cursor-pointer">
                                        Betöltés
                                    </Button>
                                );
                            } else if (isLoggedIn) {
                                return (
                                    <Link to="/dashboard">
                                        <Button variant="default" className="cursor-pointer">
                                            {user?.displayName}
                                        </Button>
                                    </Link>
                                );
                            } else {
                                return (
                                    <Link to="/login">
                                        <Button variant="default" className="cursor-pointer">
                                            Bejelentkezés
                                        </Button>
                                    </Link>
                                );
                            }
                        })()}
                    </div>
                </div>
            </header>
            <main className="space-y-6 py-12">
                <section>
                    <CurrentOpeningCard></CurrentOpeningCard>
                </section>

                <section className="max-w-11/12 md:max-w-3/4 flex flex-col mx-auto">
                    <h1 className="font-bold md:text-4xl md:mt-8 md:mb-6 text-4xl mt-6 mb-4">Hírek</h1>
                    <div className="flex gap-6 flex-wrap">
                        <NewsCard></NewsCard>
                        <NewsCard></NewsCard>
                        <NewsCard></NewsCard>
                        <NewsCard></NewsCard>
                    </div>

                </section>

                <section className="max-w-11/12 md:max-w-3/4 flex flex-col mx-auto">
                    <h1 className="font-bold md:text-4xl md:mt-8 md:mb-6 text-4xl mt-6 mb-4">GYIK</h1>
                    <Faq></Faq>
                </section>
            </main>
            <KirDevFooter></KirDevFooter>
        </>
    )
}