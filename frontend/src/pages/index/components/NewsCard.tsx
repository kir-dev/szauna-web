import {
    Card,
    CardAction,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle
} from "../../../components/ui/card.tsx";
import {Badge} from "../../../components/ui/badge.tsx";
import {Button} from "../../../components/ui/button.tsx";
import {Link} from "react-router";

export default function NewsCard(){
    return (
        <Card className="min-w-xs md:min-w-md flex-1">
            <CardHeader>
                <CardTitle>Újra nyitás</CardTitle>
                <CardDescription>Mekk Elek - 2026. 10. 11.</CardDescription>
                <CardAction>
                    <Badge>Karbantartás</Badge>
                </CardAction>
            </CardHeader>
            <CardContent>
                <p>Hosszú idő óta ismételten újra várja a szaunázni vágyó kollégistákat az <b>SCHszauna.</b></p>
            </CardContent>
            <CardFooter>
                <Button variant={'link'}>
                    <Link to={"/news"}>Tovább olvasom</Link>
                </Button>
            </CardFooter>
        </Card>
    )
}