export interface MeResponse {
    email: string
    authSub: string,
    publicId: string,
    banReason: string,
    banned: boolean,
    role: "USER" | "TRAINEE" | "SAUNA_MASTER" | "ADMIN",
    displayName: string
}