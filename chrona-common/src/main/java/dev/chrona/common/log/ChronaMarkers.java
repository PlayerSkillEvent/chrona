package dev.chrona.common.log;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class ChronaMarkers {
    public static final Marker ECON  = MarkerFactory.getMarker("ECON");
    public static final Marker QUEST = MarkerFactory.getMarker("QUEST");
    public static final Marker EVENT = MarkerFactory.getMarker("EVENT");
    public static final Marker AUDIT = MarkerFactory.getMarker("AUDIT");
    public static final Marker SEC   = MarkerFactory.getMarker("SECURITY");
    private ChronaMarkers() {}
}
