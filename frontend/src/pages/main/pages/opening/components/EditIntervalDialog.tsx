import {type FormEvent, useState} from "react";
import {addDays, format} from "date-fns";
import {ClockIcon, Edit3, Loader2} from "lucide-react";

import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "../../../../../components/ui/dialog.tsx";
import {Button} from "../../../../../components/ui/button.tsx";
import {Field, FieldGroup, FieldLabel} from "../../../../../components/ui/field.tsx";
import {Input} from "../../../../../components/ui/input.tsx";
import {
    type IntervalResponse,
    type OpeningResponse,
    updateIntervalApi,
} from "../api/opening-api.ts";
import {getApiErrorMessage} from "../api/api-error.ts";
import {toLocalDateTime} from "../../../../../lib/date-utils.ts";

function combineDateAndTime(date: Date, time: string) {
    const [hours, minutes] = time.split(":").map(Number);
    const result = new Date(date);
    result.setHours(hours, minutes, 0, 0);
    return result;
}

function extractTime(iso: string) {
    const d = new Date(iso);
    return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

interface EditIntervalDialogProps {
    opening: OpeningResponse;
    interval: IntervalResponse;
    onUpdated?: (opening: OpeningResponse) => void;
    disabled?: boolean;
}

export default function EditIntervalDialog({opening, interval, onUpdated, disabled}: EditIntervalDialogProps) {
    const [open, setOpen] = useState(false);

    const [startTime, setStartTime] = useState(() => extractTime(interval.intervalStart));
    const [endTime, setEndTime] = useState(() => extractTime(interval.intervalEnd));
    const [participantLimit, setParticipantLimit] = useState(() => String(interval.participantLimit));

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Ennyi helyet már lefoglaltak – ennél kisebbre nem vehető a limit
    const bookedSeats = interval.participantLimit - interval.availableSeats;

    const openingStart = new Date(opening.openingStart);
    const openingEnd = new Date(opening.openingEnd);

    // Megnyitáskor mindig a legfrissebb interval adatokkal töltjük fel a mezőket
    const syncFormWithInterval = () => {
        setStartTime(extractTime(interval.intervalStart));
        setEndTime(extractTime(interval.intervalEnd));
        setParticipantLimit(String(interval.participantLimit));
        setError(null);
    };

    const handleOpenChange = (next: boolean) => {
        if (isSubmitting) return;
        if (next) syncFormWithInterval();
        setOpen(next);
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError(null);

        if (!startTime || !endTime) return setError("Add meg a kezdés és a vége időpontját.");

        // A dátum az idősáv eredeti napja; ha a vége "korábbi" óra, az a következő nap (éjfélen átnyúló sáv)
        const baseDate = new Date(interval.intervalStart);
        const start = combineDateAndTime(baseDate, startTime);
        let end = combineDateAndTime(baseDate, endTime);
        if (end <= start) end = addDays(end, 1);

        if (start < openingStart || end > openingEnd) {
            return setError(
                `Az idősávnak a nyitáson belül kell lennie (${format(openingStart, "HH:mm")} – ${format(openingEnd, "HH:mm")}).`
            );
        }

        const limitValue = Number(participantLimit);
        if (!Number.isInteger(limitValue) || limitValue < 1) {
            return setError("A létszámkorlát legalább 1 fő legyen.");
        }
        if (limitValue < bookedSeats) {
            return setError(`Már ${bookedSeats} hely foglalt, a korlát ennél nem lehet kisebb.`);
        }

        setIsSubmitting(true);
        try {
            const updated = await updateIntervalApi(opening.publicId, interval.publicId, {
                intervalStart: toLocalDateTime(start),
                intervalEnd: toLocalDateTime(end),
                participantLimit: limitValue,
            });
            onUpdated?.(updated);
            setOpen(false);
        } catch (err) {
            setError(getApiErrorMessage(err, "Nem sikerült módosítani az idősávot."));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogTrigger asChild>
                <Button
                    title="Idősáv szerkesztése"
                    variant="outline"
                    size="icon-sm"
                    disabled={disabled}
                >
                    <Edit3/>
                </Button>
            </DialogTrigger>

            <DialogContent className="sm:max-w-[425px]">
                <form className="grid gap-5" onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>Idősáv szerkesztése</DialogTitle>
                        <DialogDescription>
                            A nyitás {format(openingStart, "yyyy. MM. dd. HH:mm")} – {format(openingEnd, "HH:mm")} között tart,
                            az idősávnak ezen belül kell lennie.
                        </DialogDescription>
                    </DialogHeader>

                    <FieldGroup className="grid gap-4">
                        <div className="grid grid-cols-2 gap-3">
                            <Field className="grid gap-1.5">
                                <FieldLabel htmlFor="interval-start">Kezdés</FieldLabel>
                                <div className="relative">
                                    <Input
                                        id="interval-start"
                                        type="time"
                                        value={startTime}
                                        onChange={(e) => setStartTime(e.target.value)}
                                        className="w-full bg-background pr-8 [&::-webkit-calendar-picker-indicator]:hidden"
                                    />
                                    <ClockIcon
                                        className="pointer-events-none absolute right-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground opacity-60"/>
                                </div>
                            </Field>

                            <Field className="grid gap-1.5">
                                <FieldLabel htmlFor="interval-end">Vége</FieldLabel>
                                <div className="relative">
                                    <Input
                                        id="interval-end"
                                        type="time"
                                        value={endTime}
                                        onChange={(e) => setEndTime(e.target.value)}
                                        className="w-full bg-background pr-8 [&::-webkit-calendar-picker-indicator]:hidden"
                                    />
                                    <ClockIcon
                                        className="pointer-events-none absolute right-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground opacity-60"/>
                                </div>
                            </Field>
                        </div>

                        <Field className="grid gap-1.5">
                            <FieldLabel htmlFor="participant-limit">Létszámkorlát</FieldLabel>
                            <div className="relative">
                                <Input
                                    id="participant-limit"
                                    type="number"
                                    min={Math.max(1, bookedSeats)}
                                    step={1}
                                    value={participantLimit}
                                    onChange={(e) => setParticipantLimit(e.target.value)}
                                    className="bg-background pr-10 [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
                                />
                                <span
                                    className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                                    fő
                                </span>
                            </div>
                            {bookedSeats > 0 && (
                                <p className="text-xs text-muted-foreground">
                                    Jelenleg {bookedSeats} hely foglalt, a korlát ennél nem lehet kisebb.
                                </p>
                            )}
                        </Field>
                    </FieldGroup>

                    {error && (
                        <p role="alert"
                           className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
                            {error}
                        </p>
                    )}

                    <DialogFooter className="pt-4">
                        <DialogClose asChild>
                            <Button type="button" variant="outline" disabled={isSubmitting}>
                                Mégse
                            </Button>
                        </DialogClose>
                        <Button type="submit" disabled={isSubmitting}>
                            {isSubmitting && <Loader2 className="size-4 animate-spin"/>}
                            Mentés
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}