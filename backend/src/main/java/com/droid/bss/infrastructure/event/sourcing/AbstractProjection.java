package com.droid.bss.infrastructure.event.sourcing;

/**
 * Abstract base class for projections
 */
public abstract class AbstractProjection implements Projection {

    protected long version = 0;
    protected boolean upToDate = true;

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public boolean isUpToDate() {
        return upToDate;
    }

    /**
     * Mark projection as out of date
     */
    protected void markOutOfDate() {
        this.upToDate = false;
    }

    /**
     * Mark projection as up to date
     */
    protected void markUpToDate() {
        this.upToDate = true;
    }
}
