package com.github.terminatornl.tiquality.profiling;

import com.github.terminatornl.tiquality.profiling.interfaces.ITickTime;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nonnull;

public class TickTime implements IMessage, ITickTime<TickTime> {

    private long nanosConsumed = 0L;
    private int calls = 0;

    public TickTime() {

    }

    public TickTime(TickTime other) {
        this.nanosConsumed = other.nanosConsumed;
        this.calls = other.calls;
    }

    public TickTime(ByteBuf byteBuf) {
        fromBytes(byteBuf);
    }

    public void consumeNanosIncrementCalls(long nanos) {
        nanosConsumed += nanos;
        calls++;
    }

    public void add(TickTime times) {
        this.nanosConsumed += times.nanosConsumed;
        this.calls += times.calls;
    }

    public void addNanos(long nanosConsumed) {
        this.nanosConsumed = +nanosConsumed;
    }

    public void addCalls(int calls) {
        this.calls = +calls;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        nanosConsumed = buf.readLong();
        calls = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(nanosConsumed);
        buf.writeInt(calls);
    }

    public long getNanosConsumed() {
        return nanosConsumed;
    }

    public int getCalls() {
        return calls;
    }

    @Override
    public String toString() {
        return nanosConsumed + " ns, " + calls + " call" + (calls == 1 ? "" : "s");
    }

    @Override
    public int compareTo(@Nonnull TickTime o) {
        return Long.compare(this.nanosConsumed, o.nanosConsumed);
    }
}
