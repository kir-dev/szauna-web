import {Trash2, Trash2Icon} from "lucide-react"

import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogMedia,
    AlertDialogTitle,
    AlertDialogTrigger,
} from "../../../../../components/ui/alert-dialog"
import {Button} from "../../../../../components/ui/button"

export function DeleteIntervalDialog({onConfirmDelete}: { onConfirmDelete: () => void }) {
    return (
        <AlertDialog>
            <AlertDialogTrigger>
                <Button
                    title="Idősáv törlése"
                    variant={"destructive"}
                    size={"icon-sm"}
                >
                    <Trash2 className="w-3.5 h-3.5"/>
                </Button></AlertDialogTrigger>
            <AlertDialogContent size="sm">
                <AlertDialogHeader>
                    <AlertDialogMedia
                        className="bg-destructive/10 text-destructive dark:bg-destructive/20 dark:text-destructive">
                        <Trash2Icon/>
                    </AlertDialogMedia>
                    <AlertDialogTitle>Sáv törlése?</AlertDialogTitle>
                    <AlertDialogDescription>
                        Ez a művelet nem vonható vissza! Biztosan törlöd a kiválasztott sávot?
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel variant="outline">Mégse</AlertDialogCancel>
                    <AlertDialogAction variant="destructive" onClick={onConfirmDelete}>Törlés</AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    )
}
