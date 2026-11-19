import {
    AlertCircle,
    Calendar,
    CheckCircle2,
    ChevronDown,
    ChevronUp,
    Clock,
    Flame,
    Loader2,
    Lock,
    RotateCcw,
    UserCheck,
    Users,
    XCircle,
} from "lucide-react";
import {useEffect, useState} from "react";
import axios from "axios";
import CreateOpeningDialog from "./components/CreateOpeningDialog.tsx";
import {Button} from "../../../../components/ui/button.tsx";
import {
    Pagination,
    PaginationContent,
    PaginationEllipsis,
    PaginationItem,
    PaginationLink,
    PaginationNext,
    PaginationPrevious
} from "../../../../components/ui/pagination.tsx";
import {Field, FieldLabel} from "../../../../components/ui/field.tsx";
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectTrigger,
    SelectValue
} from "../../../../components/ui/select.tsx";
import {
    deleteIntervalApi,
    deleteOpeningApi,
    getOpeningsApi,
    type IntervalResponse,
    type OpeningResponse,
    type OpeningStatus,
    type Page,
    updateIntervalStatusApi,
    updateOpeningStatusApi,
} from "./api/opening-api.ts";
import {getApiErrorMessage} from "./api/api-error.ts";
import {DeleteOpeningDialog} from "./components/DeleteOpeningDialog.tsx";
import {DeleteIntervalDialog} from "./components/DeleteIntervalDialog.tsx";
import EditOpeningDialog from "./components/EditOpeningDialog.tsx";
import EditIntervalDialog from "./components/EditIntervalDialog.tsx";
import CreateIntervalDialog from "./components/CreateIntervalDialog.tsx";
import {useUser} from "../../../../auth/useUser.ts";

export type UserRole = "USER" | "TRAINEE" | "SAUNA_MASTER" | "ADMIN";

const STATUS_FILTERS = ["ALL", "SCHEDULED", "COMPLETED", "CANCELLED"] as const;
const STATUS_FILTER_LABELS: Record<(typeof STATUS_FILTERS)[number], string> = {
    ALL: "Összes",
    SCHEDULED: "Kiírva",
    COMPLETED: "Lezajlott",
    CANCELLED: "Lemondva",
};

const dayStartIso = (day: string) => `${day}T00:00:00`;
const dayEndIso = (day: string) => `${day}T23:59:59`;

function getPageNumbers(current: number, total: number): (number | "ellipsis")[] {
    if (total <= 7) return Array.from({length: total}, (_, i) => i);
    const pages: (number | "ellipsis")[] = [0];
    const start = Math.max(1, current - 1);
    const end = Math.min(total - 2, current + 1);
    if (start > 1) pages.push("ellipsis");
    for (let i = start; i <= end; i++) pages.push(i);
    if (end < total - 2) pages.push("ellipsis");
    pages.push(total - 1);
    return pages;
}

export default function OpeningManagerPage() {
    const [selectedStatus, setSelectedStatus] = useState<OpeningStatus | "ALL">("SCHEDULED");
    const [fromDay, setFromDay] = useState("");
    const [toDay, setToDay] = useState("");
    const [page, setPage] = useState(0);
    const [pageSize, setPageSize] = useState(25);

    const [data, setData] = useState<Page<OpeningResponse> | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [reloadKey, setReloadKey] = useState(0);

    const [expandedId, setExpandedId] = useState<string | null>(null);
    const [pendingId, setPendingId] = useState<string | null>(null);

    const {user} = useUser();
    const currentUserRole: UserRole = user?.role ? user.role : "USER";

    const reload = () => setReloadKey((k) => k + 1);

    useEffect(() => {
        const controller = new AbortController();
        setIsLoading(true);
        setError(null);

        getOpeningsApi(
            {
                status: selectedStatus === "ALL" ? undefined : selectedStatus,
                from: fromDay ? dayStartIso(fromDay) : undefined,
                to: toDay ? dayEndIso(toDay) : undefined,
                page,
                size: pageSize,
            },
            controller.signal
        )
            .then((result) => {
                setData(result);
                // az első nyitás legyen alapból lenyitva, ha még nincs kiválasztva semmi
                setExpandedId((prev) => prev ?? result.content[0]?.publicId ?? null);
            })
            .catch((err) => {
                if (axios.isCancel(err)) return;
                setError(getApiErrorMessage(err, "Nem sikerült betölteni a nyitásokat."));
            })
            .finally(() => {
                if (!controller.signal.aborted) setIsLoading(false);
            });

        return () => controller.abort();
    }, [selectedStatus, fromDay, toDay, page, pageSize, reloadKey]);

    // A legtöbb mutáció a frissített nyitást adja vissza -> helyben cseréljük
    const replaceOpening = (updated: OpeningResponse) =>
        setData((prev) =>
                prev && {
                    ...prev,
                    content: prev.content.map((o) => (o.publicId === updated.publicId ? updated : o)),
                }
        );

    const runAction = async (id: string, action: () => Promise<void>, fallbackError: string) => {
        setPendingId(id);
        setError(null);
        try {
            await action();
        } catch (err) {
            setError(getApiErrorMessage(err, fallbackError));
        } finally {
            setPendingId(null);
        }
    };

    const handleCompleteOpening = (opening: OpeningResponse) =>
        runAction(
            opening.publicId,
            async () => {
                await updateOpeningStatusApi(opening.publicId, "COMPLETED");
                reload(); // a státusz változhat a szűrőhöz képest, ezért újratöltünk
            },
            "Nem sikerült lezárni a nyitást."
        );

    const handleDeleteOpening = (opening: OpeningResponse) => {
        return runAction(
            opening.publicId,
            async () => {
                await deleteOpeningApi(opening.publicId);
                if (data && data.content.length === 1 && page > 0) setPage(page - 1);
                else reload();
            },
            "Nem sikerült törölni a nyitást."
        );
    };

    const handleToggleIntervalStatus = (opening: OpeningResponse, interval: IntervalResponse) =>
        runAction(
            interval.publicId,
            async () => {
                const nextStatus = interval.status === "CANCELLED" ? "ACTIVE" : "CANCELLED";
                replaceOpening(await updateIntervalStatusApi(opening.publicId, interval.publicId, nextStatus));
            },
            "Nem sikerült módosítani az idősáv státuszát."
        );

    const handleDeleteInterval = (opening: OpeningResponse, interval: IntervalResponse) => {
        return runAction(
            interval.publicId,
            async () => {
                replaceOpening(await deleteIntervalApi(opening.publicId, interval.publicId));
            },
            "Nem sikerült törölni az idősávot."
        );
    };

    const changeStatusFilter = (status: OpeningStatus | "ALL") => {
        setSelectedStatus(status);
        setPage(0);
    };

    const openings = data?.content ?? [];
    const totalPages = data?.totalPages ?? 0;

    return (
        <div className="w-full space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight">Nyitások kezelése</h1>
                    <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
                        Szauna üzemek kiírása, idősávok módosítása és telítettség követése.
                    </p>
                </div>

                <CreateOpeningDialog
                    onCreated={(created) => {
                        setExpandedId(created.publicId);
                        if (page === 0) reload();
                        else setPage(0);
                    }}
                />
            </div>

            <div
                className="flex flex-col md:flex-row md:items-center justify-between gap-3 bg-slate-50 dark:bg-[#0e1015] border border-slate-200 dark:border-slate-800/80 p-3 rounded-xl">
                <div
                    className="flex items-center gap-1 bg-white dark:bg-[#070709] p-1 rounded-lg border border-slate-200 dark:border-slate-800 overflow-x-auto">
                    {STATUS_FILTERS.map((st) => (
                        <Button
                            key={st}
                            onClick={() => changeStatusFilter(st)}
                            variant={selectedStatus === st ? "default" : "ghost"}
                            size={"sm"}
                        >
                            {STATUS_FILTER_LABELS[st]}
                        </Button>
                    ))}
                </div>

                <div className="flex items-center gap-2 text-xs">
                    <div
                        className="flex items-center justify-between sm:justify-start w-full sm:w-auto gap-2 bg-white dark:bg-[#070709] border border-slate-200 dark:border-slate-800 px-3 py-1.5 rounded-lg">
                        <Calendar className="w-3.5 h-3.5 text-slate-400 shrink-0"/>
                        <input
                            type="date"
                            value={fromDay}
                            max={toDay || undefined}
                            onChange={(e) => {
                                setFromDay(e.target.value);
                                setPage(0);
                            }}
                            className="bg-transparent focus:outline-none text-slate-700 dark:text-slate-200 w-full"
                            title="Kezdő dátum (from)"
                        />
                        <span className="text-slate-400">–</span>
                        <input
                            type="date"
                            value={toDay}
                            min={fromDay || undefined}
                            onChange={(e) => {
                                setToDay(e.target.value);
                                setPage(0);
                            }}
                            className="bg-transparent focus:outline-none text-slate-700 dark:text-slate-200 w-full"
                            title="Záró dátum (to)"
                        />
                    </div>
                </div>
            </div>

            {error && (
                <div role="alert"
                     className="flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300">
                    <AlertCircle className="w-4 h-4 mt-0.5 shrink-0"/>
                    <span className="flex-1">{error}</span>
                    <Button variant="ghost" size="sm" onClick={reload}>Újrapróbálás</Button>
                </div>
            )}

            <div className="space-y-4">
                {isLoading && !data && (
                    <div className="flex items-center justify-center gap-2 py-16 text-sm text-slate-500">
                        <Loader2 className="w-4 h-4 animate-spin"/> Nyitások betöltése...
                    </div>
                )}

                {!isLoading && data && openings.length === 0 && (
                    <div
                        className="rounded-xl border border-dashed border-slate-300 dark:border-slate-700 py-16 text-center text-sm text-slate-500 dark:text-slate-400">
                        Nincs a szűrésnek megfelelő nyitás. Módosítsd a szűrőket, vagy írj ki egy újat.
                    </div>
                )}

                <div className={`space-y-4 transition-opacity ${isLoading && data ? "opacity-60" : ""}`}>
                    {openings.map((opening) => {
                        const isExpanded = expandedId === opening.publicId;
                        const isBusy = pendingId === opening.publicId;
                        const canEdit = opening.status === "SCHEDULED";
                        const activeIntervals = opening.intervals.filter((i) => i.status === "ACTIVE");
                        const totalSeats = activeIntervals.reduce((acc, i) => acc + i.participantLimit, 0);
                        const bookedSeats = activeIntervals.reduce(
                            (acc, i) => acc + (i.participantLimit - i.availableSeats),
                            0
                        );

                        return (
                            <div
                                key={opening.publicId}
                                className="bg-white dark:bg-[#0e1015] border border-slate-200 dark:border-slate-800/90 rounded-xl overflow-hidden shadow-sm transition hover:border-slate-300 dark:hover:border-slate-700"
                            >
                                <div
                                    className="p-4 sm:p-5 flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                                    <div className="flex items-start sm:items-center gap-3.5 sm:gap-4">
                                        <div
                                            className="bg-sky-100/80 dark:bg-[#0c3b5e]/40 border border-sky-200 dark:border-sky-800/50 text-sky-800 dark:text-sky-300 rounded-xl p-2.5 sm:p-3 text-center min-w-[64px] sm:min-w-[72px] shrink-0">
                                            <div className="text-[11px] uppercase font-semibold">
                                                {new Date(opening.openingStart).toLocaleDateString("hu-HU", {
                                                    month: "short",
                                                })}
                                            </div>
                                            <div className="text-lg sm:text-xl font-bold text-sky-950 dark:text-white">
                                                {new Date(opening.openingStart).getDate()}
                                            </div>
                                        </div>

                                        <div className="space-y-1.5 min-w-0">
                                            <div className="flex flex-wrap items-center gap-2">
                                                <span className="font-semibold text-sm sm:text-base">
                                                    {formatDate(opening.openingStart)}
                                                </span>
                                                <span
                                                    className="text-slate-500 dark:text-slate-400 text-xs sm:text-sm inline-flex items-center gap-1">
                                                    <Clock className="w-3.5 h-3.5"/>
                                                    {formatTime(opening.openingStart)} – {formatTime(opening.openingEnd)}
                                                </span>
                                                {statusBadge(opening.status)}
                                                {opening.isPrivate && (
                                                    <span
                                                        className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full bg-purple-100 text-purple-800 border border-purple-200 dark:bg-purple-500/15 dark:text-purple-300 dark:border-purple-500/30">
                                                        <Lock className="w-3 h-3"/> Privát
                                                    </span>
                                                )}
                                            </div>

                                            <div
                                                className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 dark:text-slate-400">
                                                <span className="inline-flex items-center gap-1.5">
                                                    <Flame
                                                        className="w-3.5 h-3.5 text-orange-500 dark:text-orange-400"/>
                                                    Típus: <strong
                                                    className="text-slate-800 dark:text-white">{opening.typeName}</strong>
                                                </span>
                                                <span className="inline-flex items-center gap-1.5">
                                                    <UserCheck className="w-3.5 h-3.5 text-sky-600 dark:text-sky-400"/>
                                                    Mester: <strong
                                                    className="text-slate-800 dark:text-slate-200">{opening.hostedByName}</strong>
                                                </span>
                                                <span>
                                                    Ár:{" "}
                                                    <strong className="text-slate-800 dark:text-slate-200">
                                                        {opening.price === 0 ? "Ingyenes" : `${opening.price} JMF / fő`}
                                                    </strong>
                                                </span>
                                            </div>
                                        </div>
                                    </div>

                                    <div
                                        className="flex flex-col sm:flex-row sm:items-center justify-between lg:justify-end gap-3 sm:gap-4 border-t lg:border-t-0 pt-3 lg:pt-0 border-slate-200 dark:border-slate-800">
                                        <div className="flex items-center justify-between sm:block sm:text-right">
                                            <div className="text-xs text-slate-500 dark:text-slate-400">Összes
                                                foglalás
                                            </div>
                                            <div className="text-sm font-semibold whitespace-nowrap">
                                                {bookedSeats} / {totalSeats} hely{" "}
                                                <span
                                                    className="text-xs font-normal text-slate-500 dark:text-slate-400">
                                                    ({opening.intervals.length} sáv)
                                                </span>
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-2 w-full sm:w-auto">
                                            <EditOpeningDialog
                                                opening={opening}
                                                disabled={!canEdit || isBusy}
                                                onUpdated={replaceOpening}
                                            />
                                            {opening.status === "SCHEDULED" && (
                                                <Button
                                                    title="Nyitás lezárása (Completed)"
                                                    variant={"outline"}
                                                    size={"icon-lg"}
                                                    disabled={isBusy}
                                                    onClick={() => handleCompleteOpening(opening)}
                                                >
                                                    {isBusy ? <Loader2 className="w-4 h-4 animate-spin"/> :
                                                        <CheckCircle2 className="w-4 h-4"/>}
                                                </Button>
                                            )}

                                            {currentUserRole === "ADMIN" && opening.status !== "COMPLETED" && (
                                                <DeleteOpeningDialog
                                                    onConfirmDelete={() => handleDeleteOpening(opening)}/>
                                            )}

                                            <Button
                                                variant={"secondary"}
                                                size={"lg"}
                                                className="text-xs px-3"
                                                aria-expanded={isExpanded}
                                                onClick={() => setExpandedId(isExpanded ? null : opening.publicId)}
                                            >
                                                Idősávok
                                                {isExpanded ? (
                                                    <ChevronUp className="w-4 h-4 shrink-0"/>
                                                ) : (
                                                    <ChevronDown className="w-4 h-4 shrink-0"/>
                                                )}
                                            </Button>
                                        </div>
                                    </div>
                                </div>

                                {isExpanded && (
                                    <div
                                        className="border-t border-slate-200 dark:border-slate-800/80 bg-slate-50/70 dark:bg-[#090b0e] p-4 sm:p-5 space-y-4">
                                        <div className="flex flex-wrap items-center justify-between gap-2">
                                            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                                                Foglalható Idősávok ({opening.intervals.length} db)
                                            </h4>
                                            <CreateIntervalDialog
                                                opening={opening}
                                                disabled={!canEdit}
                                                onCreated={replaceOpening}
                                            />
                                        </div>

                                        {opening.intervals.length === 0 && (
                                            <p className="text-sm text-slate-500 dark:text-slate-400">
                                                Ehhez a nyitáshoz még nincs idősáv.
                                            </p>
                                        )}

                                        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 sm:gap-4">
                                            {opening.intervals.map((interval) => {
                                                const booked = interval.participantLimit - interval.availableSeats;
                                                const isFull = interval.availableSeats === 0;
                                                const isCancelled = interval.status === "CANCELLED";
                                                const isIntervalBusy = pendingId === interval.publicId;
                                                const fillPercent = interval.participantLimit > 0
                                                    ? Math.round((booked / interval.participantLimit) * 100)
                                                    : 0;

                                                return (
                                                    <div
                                                        key={interval.publicId}
                                                        className={`rounded-xl border p-4 flex flex-col justify-between gap-3 transition ${
                                                            isCancelled
                                                                ? "bg-slate-100/60 dark:bg-slate-900/20 border-slate-200 dark:border-slate-800/50 opacity-60"
                                                                : "bg-white dark:bg-[#0e1015] border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700"
                                                        }`}
                                                    >
                                                        <div className="flex items-center justify-between gap-2">
                                                            <div
                                                                className="flex items-center gap-2 font-semibold text-sm">
                                                                <Clock
                                                                    className="w-4 h-4 text-[#7c3aed] dark:text-purple-400 shrink-0"/>
                                                                <span className="whitespace-nowrap">
                                                                    {formatTime(interval.intervalStart)} –{" "}
                                                                    {formatTime(interval.intervalEnd)}
                                                                </span>
                                                            </div>

                                                            <span
                                                                className={`text-[11px] px-2 py-0.5 rounded-md font-medium whitespace-nowrap ${
                                                                    isCancelled
                                                                        ? "bg-amber-100 text-amber-800 dark:bg-amber-500/10 dark:text-amber-400"
                                                                        : isFull
                                                                            ? "bg-rose-100 text-rose-800 dark:bg-rose-500/15 dark:text-rose-400"
                                                                            : "bg-emerald-100 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-400"
                                                                }`}
                                                            >
                                                                {isCancelled
                                                                    ? "Lemondva"
                                                                    : isFull
                                                                        ? "Betelt"
                                                                        : `${interval.availableSeats} szabad`}
                                                            </span>
                                                        </div>

                                                        <div className="space-y-1.5">
                                                            <div
                                                                className="flex justify-between text-xs text-slate-500 dark:text-slate-400">
                                                                <span className="flex items-center gap-1">
                                                                    <Users className="w-3.5 h-3.5"/> Foglaltság
                                                                </span>
                                                                <span
                                                                    className="text-slate-800 dark:text-slate-200 font-medium">
                                                                    {booked} / {interval.participantLimit} fő
                                                                </span>
                                                            </div>
                                                            <div
                                                                className="w-full h-2 bg-slate-200 dark:bg-slate-800 rounded-full overflow-hidden">
                                                                <div
                                                                    className={`h-full transition-all ${
                                                                        isCancelled
                                                                            ? "bg-slate-400 dark:bg-slate-600"
                                                                            : isFull
                                                                                ? "bg-rose-500"
                                                                                : "bg-[#7c3aed]"
                                                                    }`}
                                                                    style={{width: `${fillPercent}%`}}
                                                                />
                                                            </div>
                                                        </div>

                                                        <div
                                                            className="flex items-center justify-between pt-2 border-t border-slate-100 dark:border-slate-800/70 text-xs">
                                                            <Button variant={"ghost"} size={"sm"}>
                                                                Foglalások
                                                            </Button>

                                                            <div className="flex items-center gap-1">
                                                                <EditIntervalDialog
                                                                    opening={opening}
                                                                    interval={interval}
                                                                    disabled={!canEdit || isIntervalBusy}
                                                                    onUpdated={replaceOpening}
                                                                />
                                                                <Button
                                                                    title={isCancelled ? "Idősáv újraaktiválása" : "Idősáv lemondása"}
                                                                    variant={"outline"}
                                                                    size={"icon-sm"}
                                                                    disabled={!canEdit || isIntervalBusy}
                                                                    onClick={() => handleToggleIntervalStatus(opening, interval)}
                                                                >
                                                                    {isIntervalBusy ? (
                                                                        <Loader2 className="w-3.5 h-3.5 animate-spin"/>
                                                                    ) : isCancelled ? (
                                                                        <RotateCcw className="w-3.5 h-3.5"/>
                                                                    ) : (
                                                                        <XCircle className="w-3.5 h-3.5"/>
                                                                    )}
                                                                </Button>

                                                                <DeleteIntervalDialog
                                                                    onConfirmDelete={() => handleDeleteInterval(opening, interval)}/>
                                                            </div>
                                                        </div>
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>

                {data && totalPages > 0 && (
                    <div className="flex gap-4 flex-col">
                        {totalPages > 1 && (
                            <Pagination>
                                <PaginationContent>
                                    <PaginationItem>
                                        <PaginationPrevious
                                            text={"Előző"}
                                            href="#"
                                            aria-disabled={data.first}
                                            className={data.first ? "pointer-events-none opacity-50" : ""}
                                            onClick={(e) => {
                                                e.preventDefault();
                                                if (!data.first) setPage(page - 1);
                                            }}
                                        />
                                    </PaginationItem>

                                    {getPageNumbers(page, totalPages).map((p, idx) =>
                                        p === "ellipsis" ? (
                                            <PaginationItem key={`ellipsis-${idx}`}>
                                                <PaginationEllipsis/>
                                            </PaginationItem>
                                        ) : (
                                            <PaginationItem key={p}>
                                                <PaginationLink
                                                    href="#"
                                                    isActive={p === page}
                                                    onClick={(e) => {
                                                        e.preventDefault();
                                                        setPage(p);
                                                    }}
                                                >
                                                    {p + 1}
                                                </PaginationLink>
                                            </PaginationItem>
                                        )
                                    )}

                                    <PaginationItem>
                                        <PaginationNext
                                            text={"Következő"}
                                            href="#"
                                            aria-disabled={data.last}
                                            className={data.last ? "pointer-events-none opacity-50" : ""}
                                            onClick={(e) => {
                                                e.preventDefault();
                                                if (!data.last) setPage(page + 1);
                                            }}
                                        />
                                    </PaginationItem>
                                </PaginationContent>
                            </Pagination>
                        )}

                        <Field orientation="horizontal" className="flex justify-end gap-2">
                            <FieldLabel htmlFor="select-rows-per-page" className="flex justify-end">
                                Sorok oldalanként:
                            </FieldLabel>
                            <Select
                                value={String(pageSize)}
                                onValueChange={(value) => {
                                    setPageSize(Number(value));
                                    setPage(0);
                                }}
                            >
                                <SelectTrigger id="select-rows-per-page">
                                    <SelectValue/>
                                </SelectTrigger>
                                <SelectContent align="start">
                                    <SelectGroup>
                                        <SelectItem value="10">10</SelectItem>
                                        <SelectItem value="25">25</SelectItem>
                                        <SelectItem value="50">50</SelectItem>
                                        <SelectItem value="100">100</SelectItem>
                                    </SelectGroup>
                                </SelectContent>
                            </Select>
                        </Field>
                    </div>
                )}
            </div>
        </div>
    );
}

// ---------- Segédfüggvények (komponensen kívül, nem jönnek létre minden renderkor) ----------

function formatTime(iso: string) {
    return new Date(iso).toLocaleTimeString("hu-HU", {hour: "2-digit", minute: "2-digit"});
}

function formatDate(iso: string) {
    return new Date(iso).toLocaleDateString("hu-HU", {
        year: "numeric",
        month: "short",
        day: "numeric",
        weekday: "short",
    });
}

function statusBadge(status: OpeningStatus) {
    const styles: Record<OpeningStatus, string> = {
        SCHEDULED:
            "bg-emerald-100 text-emerald-800 border-emerald-200 dark:bg-emerald-500/15 dark:text-emerald-400 dark:border-emerald-500/30",
        COMPLETED:
            "bg-sky-100 text-sky-800 border-sky-200 dark:bg-sky-500/15 dark:text-sky-400 dark:border-sky-500/30",
        CANCELLED:
            "bg-amber-100 text-amber-800 border-amber-200 dark:bg-amber-500/15 dark:text-amber-400 dark:border-amber-500/30",
        DELETED:
            "bg-rose-100 text-rose-800 border-rose-200 dark:bg-rose-500/15 dark:text-rose-400 dark:border-rose-500/30",
    };
    const labels: Record<OpeningStatus, string> = {
        SCHEDULED: "Kiírva",
        COMPLETED: "Lezajlott",
        CANCELLED: "Elmarad",
        DELETED: "Törölve",
    };
    return (
        <span className={`px-2.5 py-0.5 text-xs font-medium rounded-full border ${styles[status]}`}>
            {labels[status]}
        </span>
    );
}
