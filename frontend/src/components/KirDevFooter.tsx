export default function KirDevFooter() {
    const currentYear = new Date().getFullYear()
    return (
        <footer className="py-6 text-center border-t border-border/60">
            <p className="text-md">Made with ❤️ By <b>Kir-Dev</b></p>
            <p className="text-xs">Minden jog fenntartva. &copy; {currentYear} </p>
        </footer>
    )
}