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

export function DeleteOpeningDialog({onConfirmDelete}: { onConfirmDelete: () => void }) {
    return (
        <AlertDialog>
            <AlertDialogTrigger>
                <Button
                    title="Nyitás törlése"
                    variant={"destructive"}
                    size={"icon-lg"}
                >
                    <Trash2 className="w-4 h-4"/>
                </Button> </AlertDialogTrigger>
            <AlertDialogContent size="sm">
                <AlertDialogHeader>
                    <AlertDialogMedia
                        className="bg-destructive/10 text-destructive dark:bg-destructive/20 dark:text-destructive">
                        <Trash2Icon/>
                    </AlertDialogMedia>
                    <AlertDialogTitle>Nyitás törlése?</AlertDialogTitle>
                    <AlertDialogDescription>
                        Ez a művelet nem vonható vissza! Biztosan törlöd a kiválasztott nyitást?
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
