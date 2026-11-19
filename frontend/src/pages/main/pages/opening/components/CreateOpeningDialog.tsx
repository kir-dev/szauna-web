import {type FormEvent, useState} from "react";
import {format} from "date-fns";
import {hu} from "date-fns/locale";
import {CalendarIcon, ChevronDownIcon, CirclePlus, ClockIcon, Loader2} from "lucide-react";

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
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "../../../../../components/ui/select.tsx";
import {Checkbox} from "../../../../../components/ui/checkbox.tsx";
import {cn} from "../../../../../lib/utils.ts";
import {createOpeningApi, type OpeningResponse} from "../api/opening-api.ts";
import {getApiErrorMessage} from "../api/api-error.ts";
import {toLocalDateTime} from "../../../../../lib/date-utils.ts";

// TODO: a Swaggerben nincs végpont a nyitás típusokhoz, ezért egyelőre fix lista.
// Az id-knak egyezniük kell az adatbázisbeli opening_type id-kkal!
const OPENING_TYPES = [
    {id: 1, name: "Normál nyitás"},
    {id: 2, name: "Hétvégi / Kihelyezett"},
    {id: 3, name: "Különleges esemény"},
];

const DEFAULTS = {fromTime: "09:00", toTime: "17:00"};

function combineDateAndTime(date: Date, time: string) {
    const [hours, minutes] = time.split(":").map(Number);
    const result = new Date(date);
    result.setHours(hours, minutes, 0, 0);
    return result;
}

interface CreateOpeningDialogProps {
    onCreated?: (opening: OpeningResponse) => void;
}

export default function CreateOpeningDialog({onCreated}: CreateOpeningDialogProps) {
    const [open, setOpen] = useState(false);
    const [fromOpen, setFromOpen] = useState(false);
    const [toOpen, setToOpen] = useState(false);

    const [fromDate, setFromDate] = useState<Date | undefined>(new Date());
    const [toDate, setToDate] = useState<Date | undefined>(new Date());
    const [fromTime, setFromTime] = useState(DEFAULTS.fromTime);
    const [toTime, setToTime] = useState(DEFAULTS.toTime);
    const [openingTypeId, setOpeningTypeId] = useState("");
    const [price, setPrice] = useState("");
    const [useDefaultIntervals, setUseDefaultIntervals] = useState(false);
    const [isPrivateOpening, setIsPrivateOpening] = useState(false);

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const resetForm = () => {
        setFromDate(new Date());
        setToDate(new Date());
        setFromTime(DEFAULTS.fromTime);
        setToTime(DEFAULTS.toTime);
        setOpeningTypeId("");
        setPrice("");
        setUseDefaultIntervals(false);
        setIsPrivateOpening(false);
        setError(null);
    };

    const handleOpenChange = (next: boolean) => {
        if (isSubmitting) return;
        setOpen(next);
        if (!next) resetForm();
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
            const created = await createOpeningApi({
                openingStart: toLocalDateTime(start),
                openingEnd: toLocalDateTime(end),
                price: priceValue,
                openingTypeId: Number(openingTypeId),
                generateDefaultIntervals: useDefaultIntervals,
                isPrivate: isPrivateOpening,
            });
            onCreated?.(created);
            setOpen(false);
            resetForm();
        } catch (err) {
            setError(getApiErrorMessage(err, "Nem sikerült létrehozni a nyitást."));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogTrigger asChild>
                <Button variant="default" className="gap-2">
                    <CirclePlus className="size-4"/>
                    Nyitás kiírása
                </Button>
            </DialogTrigger>

            <DialogContent className="sm:max-w-[425px]">
                <form className="grid gap-5" onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>Nyitás kiírása</DialogTitle>
                        <DialogDescription>
                            Add meg a nyitás paramétereit, időpontját és típusát.
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
                                                // ha a zárás korábbra esne, húzzuk utána
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

                        <div className="grid gap-3 rounded-lg border bg-muted/30 p-3.5">
                            <Field orientation="horizontal" className="flex items-center gap-2.5">
                                <Checkbox
                                    id="default-intervals"
                                    checked={useDefaultIntervals}
                                    onCheckedChange={(checked) => setUseDefaultIntervals(checked === true)}
                                />
                                <FieldLabel
                                    htmlFor="default-intervals"
                                    className="cursor-pointer font-normal leading-none"
                                >
                                    Alapértelmezett intervallumokkal
                                </FieldLabel>
                            </Field>

                            <Field orientation="horizontal" className="flex items-center gap-2.5">
                                <Checkbox
                                    id="private-opening"
                                    checked={isPrivateOpening}
                                    onCheckedChange={(checked) => setIsPrivateOpening(checked === true)}
                                />
                                <FieldLabel
                                    htmlFor="private-opening"
                                    className="cursor-pointer font-normal leading-none"
                                >
                                    Privát nyitás
                                </FieldLabel>
                            </Field>
                        </div>
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
