import {type FormEvent, useState} from "react";
import {format} from "date-fns";
import {hu} from "date-fns/locale";
import {CalendarIcon, ChevronDownIcon, ClockIcon, Edit3, Loader2} from "lucide-react";

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
import {Popover, PopoverContent, PopoverTrigger} from "../../../../../components/ui/popover.tsx";
import {Input} from "../../../../../components/ui/input.tsx";
import {Calendar} from "../../../../../components/ui/calendar.tsx";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "../../../../../components/ui/select.tsx";
import {cn} from "../../../../../lib/utils.ts";
import {type OpeningResponse, updateOpeningApi} from "../api/opening-api.ts";
import {getApiErrorMessage} from "../api/api-error.ts";
import {toLocalDateTime} from "../../../../../lib/date-utils.ts";

const OPENING_TYPES = [
    {id: 1, name: "Normál nyitás"},
    {id: 2, name: "Hétvégi / Kihelyezett"},
    {id: 3, name: "Különleges esemény"},
];

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

interface EditOpeningDialogProps {
    opening: OpeningResponse;
    onUpdated?: (opening: OpeningResponse) => void;
    disabled?: boolean;
}

export default function EditOpeningDialog({opening, onUpdated, disabled}: EditOpeningDialogProps) {
    const [open, setOpen] = useState(false);
    const [fromOpen, setFromOpen] = useState(false);
    const [toOpen, setToOpen] = useState(false);

    const [fromDate, setFromDate] = useState<Date | undefined>(() => new Date(opening.openingStart));
    const [toDate, setToDate] = useState<Date | undefined>(() => new Date(opening.openingEnd));
    const [fromTime, setFromTime] = useState(() => extractTime(opening.openingStart));
    const [toTime, setToTime] = useState(() => extractTime(opening.openingEnd));
    const [openingTypeId, setOpeningTypeId] = useState(() => {
        const matched = OPENING_TYPES.find((t) => t.name === opening.typeName);
        return matched ? String(matched.id) : "";
    });
    const [price, setPrice] = useState(() => String(opening.price));

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Megnyitáskor mindig a legfrissebb opening adatokkal töltjük fel a mezőket
    const syncFormWithOpening = () => {
        setFromDate(new Date(opening.openingStart));
        setToDate(new Date(opening.openingEnd));
        setFromTime(extractTime(opening.openingStart));
        setToTime(extractTime(opening.openingEnd));
        const matched = OPENING_TYPES.find((t) => t.name === opening.typeName);
        setOpeningTypeId(matched ? String(matched.id) : "");
        setPrice(String(opening.price));
        setError(null);
    };

    const handleOpenChange = (next: boolean) => {
        if (isSubmitting) return;
        if (next) syncFormWithOpening();
        setOpen(next);
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError(null);

        if (!openingTypeId) return setError("Válaszd ki a nyitás típusát.");
        if (!fromDate || !toDate || !fromTime || !toTime) {
            return setError("Add meg a kezdés és a zárás időpontját.");
        }

        const start = combineDateAndTime(fromDate, fromTime);
        const end = combineDateAndTime(toDate, toTime);
        if (end <= start) return setError("A zárásnak a kezdés után kell lennie.");

        const priceValue = price === "" ? 0 : Number(price);
        if (!Number.isFinite(priceValue) || priceValue < 0) {
            return setError("Az ár nem lehet negatív.");
        }

        setIsSubmitting(true);
        try {
            const updated = await updateOpeningApi(opening.publicId, {
                openingStart: toLocalDateTime(start),
                openingEnd: toLocalDateTime(end),
                price: priceValue,
                openingTypeId: Number(openingTypeId),
            });
            onUpdated?.(updated);
            setOpen(false);
        } catch (err) {
            setError(getApiErrorMessage(err, "Nem sikerült módosítani a nyitást."));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogTrigger asChild>
                <Button
                    title="Nyitás adatainak szerkesztése"
                    variant="outline"
                    size="icon-lg"
                    disabled={disabled}
                >
                    <Edit3 className="w-4 h-4"/>
                </Button>
            </DialogTrigger>

            <DialogContent className="sm:max-w-[425px]">
                <form className="grid gap-5" onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>Nyitás szerkesztése</DialogTitle>
                        <DialogDescription>
                            Módosítsd a nyitás paramétereit, időpontját vagy árát.
                        </DialogDescription>
                    </DialogHeader>

                    <FieldGroup className="grid gap-4">
                        <Field className="grid gap-1.5">
                            <FieldLabel htmlFor="category-select">Típus</FieldLabel>
                            <Select value={openingTypeId} onValueChange={setOpeningTypeId}>
                                <SelectTrigger id="category-select" className="w-full">
                                    <SelectValue placeholder="Válassz lehetőséget..."/>
                                </SelectTrigger>
                                <SelectContent>
                                    {OPENING_TYPES.map((type) => (
                                        <SelectItem key={type.id} value={String(type.id)}>
                                            {type.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </Field>

                        <Field className="grid gap-1.5">
                            <FieldLabel htmlFor="from-date-id">Kezdés időpontja</FieldLabel>
                            <div className="grid grid-cols-[1fr_115px] gap-2">
                                <Popover open={fromOpen} onOpenChange={setFromOpen}>
                                    <PopoverTrigger asChild>
                                        <Button
                                            type="button"
                                            variant="outline"
                                            id="from-date-id"
                                            className={cn(
                                                "w-full justify-between font-normal",
                                                !fromDate && "text-muted-foreground"
                                            )}
                                        >
                                            <span className="flex items-center gap-2 truncate">
                                                <CalendarIcon className="size-4 opacity-60"/>
                                                {fromDate ? format(fromDate, "yyyy. MM. dd.") : "Válassz dátumot"}
                                            </span>
                                            <ChevronDownIcon className="size-4 opacity-50"/>
                                        </Button>
                                    </PopoverTrigger>
                                    <PopoverContent className="w-auto overflow-hidden p-0" align="start">
                                        <Calendar
                                            locale={hu}
                                            mode="single"
                                            selected={fromDate}
                                            captionLayout="dropdown"
                                            defaultMonth={fromDate}
                                            onSelect={(date) => {
                                                setFromDate(date);
                                                if (date && toDate && toDate < date) setToDate(date);
                                                setFromOpen(false);
                                            }}
                                        />
                                    </PopoverContent>
                                </Popover>

                                <div className="relative">
                                    <Input
                                        type="time"
                                        value={fromTime}
                                        onChange={(e) => setFromTime(e.target.value)}
                                        className="w-full bg-background pr-8 [&::-webkit-calendar-picker-indicator]:hidden"
                                    />
                                    <ClockIcon
                                        className="pointer-events-none absolute right-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground opacity-60"/>
                                </div>
                            </div>
                        </Field>

                        <Field className="grid gap-1.5">
                            <FieldLabel htmlFor="to-date-id">Zárás időpontja</FieldLabel>
                            <div className="grid grid-cols-[1fr_115px] gap-2">
                                <Popover open={toOpen} onOpenChange={setToOpen}>
                                    <PopoverTrigger asChild>
                                        <Button
                                            type="button"
                                            variant="outline"
                                            id="to-date-id"
                                            className={cn(
                                                "w-full justify-between font-normal",
                                                !toDate && "text-muted-foreground"
                                            )}
                                        >
                                            <span className="flex items-center gap-2 truncate">
                                                <CalendarIcon className="size-4 opacity-60"/>
                                                {toDate ? format(toDate, "yyyy. MM. dd.") : "Válassz dátumot"}
                                            </span>
                                            <ChevronDownIcon className="size-4 opacity-50"/>
                                        </Button>
                                    </PopoverTrigger>
                                    <PopoverContent className="w-auto overflow-hidden p-0" align="start">
                                        <Calendar
                                            locale={hu}
                                            mode="single"
                                            selected={toDate}
                                            captionLayout="dropdown"
                                            defaultMonth={toDate}
                                            onSelect={(date) => {
                                                setToDate(date);
                                                setToOpen(false);
                                            }}
                                        />
                                    </PopoverContent>
                                </Popover>

                                <div className="relative">
                                    <Input
                                        type="time"
                                        value={toTime}
                                        onChange={(e) => setToTime(e.target.value)}
                                        className="w-full bg-background pr-8 [&::-webkit-calendar-picker-indicator]:hidden"
                                    />
                                    <ClockIcon
                                        className="pointer-events-none absolute right-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground opacity-60"/>
                                </div>
                            </div>
                        </Field>

                        <Field className="grid gap-1.5">
                            <FieldLabel htmlFor="price">Ár</FieldLabel>
                            <div className="relative">
                                <Input
                                    id="price"
                                    type="number"
                                    min={0}
                                    placeholder="0"
                                    value={price}
                                    onChange={(e) => setPrice(e.target.value)}
                                    className="bg-background pr-10 [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
                                />
                                <span
                                    className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                                    JMF
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
                            Mentés
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}