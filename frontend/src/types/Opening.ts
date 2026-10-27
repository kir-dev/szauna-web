export type OpeningStatus =
    | "SCHEDULED"
    | "COMPLETED"
    | "CANCELLED"
    | "DELETED"

export type IntervalStatus =
    | "ACTIVE"
    | "CANCELLED"
    | "DELETED"

export interface IntervalResponse {
    publicId: string
    intervalStart: string
    intervalEnd: string
    participantLimit: number
    availableSeats: number
    status: IntervalStatus
}

export interface OpeningResponse {
    publicId: string
    openingStart: string
    openingEnd: string
    isPrivate: boolean
    price: number
    hostedByName: string
    typeName: string
    status: OpeningStatus
    intervals: IntervalResponse[]
}