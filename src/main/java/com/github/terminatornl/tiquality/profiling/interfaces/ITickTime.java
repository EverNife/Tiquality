package com.github.terminatornl.tiquality.profiling.interfaces;

public interface ITickTime<T> extends Comparable<T>{

    public long getNanosConsumed();

    public int getCalls();

}
