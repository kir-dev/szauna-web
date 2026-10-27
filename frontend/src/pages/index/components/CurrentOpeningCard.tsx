import {Calendar, ChevronRight, Sparkles, Users} from "lucide-react"
import {Badge} from "../../../components/ui/badge.tsx";
import {Button} from "../../../components/ui/button.tsx";
import {Link} from "react-router";
import {useEffect, useState} from "react";
import type {OpeningResponse} from "../../../types/Opening.ts";
import {Skeleton} from "../../../components/ui/skeleton.tsx";
import {formatOpeningDate, formatTimeRange} from "../../../lib/date-utils.ts";
import {toast} from "sonner"
import {getCurrentOpening} from "../api/index-api.ts";


export function CurrentOpeningCard() {
    const [opening, setOpening] = useState<OpeningResponse | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        getCurrentOpening().then(
            (data) => setOpening(data)
        ).catch(() => {
            setError("A szerver nem elérhető, kérlek próbáld újra!")
            toast.error("Sikertelen betöltés", {
                description: "A szerver nem elérhető, kérlek próbáld újra!"
            })
        }).finally(() => setLoading(false)
        )
    }, [])

    if (loading) {
        return (
            <CurrentOpeningSkeleton></CurrentOpeningSkeleton>
        )
    }

    if (error){
        return (
            <div
                className="w-full max-w-11/12 md:max-w-3/4 mx-auto rounded-2xl border border-border/80 bg-card/60 backdrop-blur-md shadow-xl overflow-hidden">
                <h1 className="text-4xl md:text-6xl font-bold my-12 text-center mx-4">¯\_(ツ)_/¯</h1>
            </div>
        )
    }

    if (!opening) {
        return (
            <div
                className="w-full max-w-11/12 md:max-w-3/4 mx-auto rounded-2xl border border-border/80 bg-card/60 backdrop-blur-md shadow-xl overflow-hidden">
                <h1 className="text-2xl md:text-3xl font-bold my-12 text-center mx-4">Nincs kiírva nyitás!</h1>
            </div>
        )
    }

    return (
        <div
            className="w-full max-w-11/12 md:max-w-3/4 mx-auto rounded-2xl border border-border/80 bg-card/60 backdrop-blur-md shadow-xl overflow-hidden">

            <div
                className="p-6 pb-4 border-b border-border/60 bg-linear-to-br from-primary/5 via-transparent to-accent/5">
                <div className="flex items-center justify-between gap-3 mb-2">
                    <div
                        className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                        <Calendar className="w-4 h-4 text-primary"/>
                        <span>{formatOpeningDate(opening.openingStart)}</span>
                    </div>

                    <Badge
                        className="bg-primary/10 text-primary border-primary/20 hover:bg-primary/15 font-medium text-xs px-2.5 py-0.5">
                        {opening.typeName}
                    </Badge>
                </div>

                <h2 className="text-xl sm:text-2xl font-extrabold tracking-tight text-foreground">
                    Aktuális nyitás
                </h2>
                <p className="text-xs sm:text-sm text-muted-foreground mt-1 flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-accent shrink-0"/>
                    Klasszikus forró levegős szaunaest illóolajos felöntésekkel.
                </p>
            </div>

            <div className="p-4 sm:p-6 space-y-2.5">
                <div className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3 px-1">
                    Szabad idősávok
                </div>


                {
                    opening.intervals.map((interval) => {
                        const isFull = interval.availableSeats == 0
                        return (
                            <div
                                key={interval.publicId}
                                className={`flex items-center justify-between p-3.5 rounded-xl border transition-all ${
                                    isFull
                                        ? "border-dashed border-border/60 bg-muted/20 opacity-60"
                                        : "border-border/70 bg-card hover:border-primary/50 hover:bg-accent/5"
                                }`}
                            >
                                <div className="space-y-1">
                                    <div
                                        className={`text-base font-bold tracking-tight ${isFull ? "text-muted-foreground" : "text-foreground"}`}>
                                        {formatTimeRange(interval.intervalStart, interval.intervalEnd)}
                                    </div>
                                    <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                                        <Users className="w-3.5 h-3.5"/>
                                        {isFull ? (
                                            <span className="text-destructive font-medium">Betelt</span>
                                        ) : (
                                            <span>
                      <strong className="text-foreground font-semibold">{interval.availableSeats}</strong> szabad hely
                    </span>
                                        )}
                                        <span>•</span>
                                        <span>{interval.participantLimit - interval.availableSeats} / {interval.participantLimit} fő</span>
                                    </div>
                                </div>

                                {isFull ? (
                                    <Button size="sm" disabled variant="ghost" className="text-xs">
                                        Betelt
                                    </Button>
                                ) : (
                                    <Button asChild size="sm"
                                            className="bg-primary hover:bg-primary/90 text-primary-foreground font-semibold shadow-sm">
                                        <Link
                                            to={`/booking?openingId=${opening.publicId}&intervalId=${interval.publicId}`}>
                                            Foglalás <ChevronRight className="w-3.5 h-3.5 ml-1"/>
                                        </Link>
                                    </Button>
                                )}
                            </div>
                        )
                    })
                }
            </div>
        </div>
    )
}

export function CurrentOpeningSkeleton() {
    return (
        <div
            className="w-full max-w-11/12 md:max-w-3/4 mx-auto rounded-2xl border border-border/80 bg-card/60 backdrop-blur-md shadow-xl overflow-hidden">

            <div
                className="p-6 pb-5 border-b border-border/60 bg-gradient-to-br from-primary/5 via-transparent to-accent/5">
                <div className="flex items-center justify-between gap-3 mb-3">
                    <div className="flex items-center gap-2">
                        <Skeleton className="h-4 w-4 rounded"/>
                        <Skeleton className="h-3.5 w-48 rounded"/>
                    </div>

                    <Skeleton className="h-6 w-24 rounded-full"/>
                </div>

                <Skeleton className="h-8 w-44 rounded-lg mt-1 mb-2.5"/>

                <div className="flex items-center gap-2">
                    <Skeleton className="h-3.5 w-3.5 rounded-full"/>
                    <Skeleton className="h-4 w-80 max-w-[80%] rounded"/>
                </div>
            </div>

            <div className="p-4 sm:p-6 space-y-3">
                <Skeleton className="h-3 w-32 rounded mb-4"/>

                {[1, 2, 3].map((i) => (
                    <div
                        key={i}
                        className="flex items-center justify-between p-3.5 sm:p-4 rounded-xl border border-border/70 bg-card"
                    >
                        <div className="space-y-2">
                            <Skeleton className="h-5 w-28 rounded"/>
                            <div className="flex items-center gap-2">
                                <Skeleton className="h-3.5 w-3.5 rounded-full"/>
                                <Skeleton className="h-3.5 w-36 rounded"/>
                            </div>
                        </div>

                        <Skeleton className="h-9 w-24 rounded-lg"/>
                    </div>
                ))}
            </div>

        </div>
    )
}