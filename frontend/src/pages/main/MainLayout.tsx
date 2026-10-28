import {Outlet} from "react-router";

export default function MainLayout(){
    return (
        <>
            Protected layout
            <Outlet/>
        </>
    )
}