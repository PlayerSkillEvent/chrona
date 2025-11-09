package dev.chrona.common.npc.api;

import java.util.ArrayList;
import java.util.List;

public final class Path {
    public enum Loop {
        NONE, LOOP, PING_PONG
    }

    public final String name;
    public final List<Waypoint> points;
    public final double speedBlocksPerSec;
    public final Loop loop;

    public Path(String name, List<Waypoint> p, double s, Loop l) {
        this.name = name;
        points = List.copyOf(p);
        speedBlocksPerSec = s;
        loop = l;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Waypoint> pts = new ArrayList<>();
        private String name = "";
        private double speed = 3.0;
        private Loop loop = Loop.NONE;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder add(Waypoint wp) {
            pts.add(wp);
            return this;
        }

        public Builder addAll(List<Waypoint> wps) {
            pts.addAll(wps);
            return this;
        }

        public Builder speed(double blocksPerSec) {
            speed = Math.max(0.1, blocksPerSec);
            return this;
        }

        public Builder loop(Loop l) {
            loop = l;
            return this;
        }

        public Path build() {
            if (pts.size() < 2)
                throw new IllegalArgumentException("path needs >= 2 points");
            return new Path(name, pts, speed, loop);
        }
    }
}
