import {type FormEvent, useState} from "react";
import {addDays, format} from "date-fns";
import {ClockIcon, Loader2, Plus} from "lucide-react";

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
import {createIntervalApi, type OpeningResponse} from "../api/opening-api.ts";
import {getApiErrorMessage} from "../api/api-error.ts";
import {toLocalDateTime} from "../../../../../lib/date-utils.ts";

const DEFAULT_PARTICIPANT_LIMIT = 8; // a backend DEFAULT_PARTICIPANT_LIMIT értékével egyezik

function combineDateAndTime(date: Date, time: string) {
    const [hours, minutes] = time.split(":").map(Number);
    const result = new Date(date);
    result.setHours(hours, minutes, 0, 0);
    return result;
}

function toTime(d: Date) {
    return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

// Javasolt alapértékek: az utolsó aktív idősáv végétől a nyitás végéig,
// ha nincs aktív idősáv, akkor a teljes nyitás
function getDefaultRange(opening: OpeningResponse) {
    const openingStart = new Date(opening.openingStart);
    const openingEnd = new Date(opening.openingEnd);

    const activeEnds = opening.intervals
        .filter((i) => i.status === "ACTIVE")
        .map((i) => new Date(i.intervalEnd));

    const latestEnd = activeEnds.length > 0
        ? new Date(Math.max(...activeEnds.map((d) => d.getTime())))
        : openingStart;

    const start = latestEnd < openingEnd ? latestEnd : openingStart;
    return {start: toTime(start), end: toTime(openingEnd)};
}

interface CreateIntervalDialogProps {
    opening: OpeningResponse;
    onCreated?: (opening: OpeningResponse) => void;
    disabled?: boolean;
}

export default function CreateIntervalDialog({opening, onCreated, disabled}: CreateIntervalDialogProps) {
    const [open, setOpen] = useState(false);

    const [startTime, setStartTime] = useState("");
    const [endTime, setEndTime] = useState("");
    const [participantLimit, setParticipantLimit] = useState(String(DEFAULT_PARTICIPANT_LIMIT));

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const openingStart = new Date(opening.openingStart);
    const openingEnd = new Date(opening.openingEnd);

    // Megnyitáskor mindig friss alapértékekkel töltjük fel a mezőket
    const resetForm = () => {
        const {start, end} = getDefaultRange(opening);
        setStartTime(start);
        setEndTime(end);
        setParticipantLimit(String(DEFAULT_PARTICIPANT_LIMIT));
        setError(null);
    };

    const handleOpenChange = (next: boolean) => {
        if (isSubmitting) return;
        if (next) resetForm();
        setOpen(next);
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError(null);

        if (!startTime || !endTime) return setError("Add meg a kezdés és a vége időpontját.");

        // A nyitás napjához igazítjuk; ha a kezdés óra korábbi, mint a nyitás kezdete,
        // akkor az éjfél utáni (következő napi) részre esik
        let start = combineDateAndTime(openingStart, startTime);
        if (start < openingStart) start = addDays(start, 1);
        let end = combineDateAndTime(start, endTime);
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

        setIsSubmitting(true);
        try {
            const updated = await createIntervalApi(opening.publicId, {
                intervalStart: toLocalDateTime(start),
                intervalEnd: toLocalDateTime(end),
                participantLimit: limitValue,
            });
            onCreated?.(updated);
            setOpen(false);
        } catch (err) {
            setError(getApiErrorMessage(err, "Nem sikerült létrehozni az idősávot."));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogTrigger asChild>
                <Button variant="outline" size="sm" disabled={disabled}>
                    <Plus className="w-3.5 h-3.5"/>
                    Új idősáv hozzáadása
                </Button>
            </DialogTrigger>

            <DialogContent className="sm:max-w-[425px]">
                <form className="grid gap-5" onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>Új idősáv</DialogTitle>
                        <DialogDescription>
                            A nyitás {format(openingStart, "yyyy. MM. dd. HH:mm")} – {format(openingEnd, "HH:mm")} között tart,
                            az idősávnak ezen belül kell lennie, és nem fedhet át másik aktív idősávval.
                        </DialogDescription>
                    </DialogHeader>

                    <FieldGroup className="grid gap-4">
                        <div className="grid grid-cols-2 gap-3">
                            <Field className="grid gap-1.5">
                                <FieldLabel htmlFor="new-interval-start">Kezdés</FieldLabel>
                                <div className="relative">
                                    <Input
                                        id="new-interval-start"
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
                                <FieldLabel htmlFor="new-interval-end">Vége</FieldLabel>
                                <div className="relative">
                                    <Input
                                        id="new-interval-end"
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
                            <FieldLabel htmlFor="new-participant-limit">Létszámkorlát</FieldLabel>
                            <div className="relative">
                                <Input
                                    id="new-participant-limit"
                                    type="number"
                                    min={1}
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
                            Létrehozás
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}