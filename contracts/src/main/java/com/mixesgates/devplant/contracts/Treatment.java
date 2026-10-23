package com.mixesgates.devplant.contracts;

public final class Treatment {

    public enum Method { CULTURAL, ORGANIC, CHEMICAL }

    public final long id;
    public final Method method;
    public final String title;
    public final String detail;

    public Treatment(long id, Method method, String title, String detail) {
        this.id = id;
        this.method = method;
        this.title = title;
        this.detail = detail;
    }
}
