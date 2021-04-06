package com.github.terminatornl.tiquality.profiling.interfaces;

import com.github.terminatornl.tiquality.api.Location;

public interface IAnalyzedComponent {

    public String getReferencedClass();

    public ITickTime getTimes();

    public Location<Integer, IBlockPos> getLastKnownLocation();

    public String getResourceLocationString();

    public String getFriendlyName();

}
