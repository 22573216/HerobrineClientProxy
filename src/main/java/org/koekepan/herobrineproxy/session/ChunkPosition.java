package org.koekepan.herobrineproxy.session;

import java.util.Objects;

// Define a class to store x, z coordinates
public class ChunkPosition {
    private final int x;
    private final int z;


    public ChunkPosition(int x, int z) {
        this.x = x;
        this.z = z;
    }

    // Add getters here if needed...

    // Define equality based on x, z values
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChunkPosition that = (ChunkPosition) obj;
        return x == that.x && z == that.z;
    }

    // Define hashCode based on x, z values
    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getX2() { return this.x + 15; }
    public int getZ2() { return this.z; }
    public int getX3() { return this.x; }
    public int getZ3() { return this.z + 15; }
    public int getX4() { return this.x + 15; }
    public int getZ4() { return this.z + 15; }
}
