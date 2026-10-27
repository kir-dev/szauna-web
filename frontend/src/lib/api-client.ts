import * as axios from "axios";

export const apiClient = axios.create({
    baseURL: "http://localhost:8080/api",
    withCredentials: true,
    headers: {
        "Content-Type":"application/json"
    },
})