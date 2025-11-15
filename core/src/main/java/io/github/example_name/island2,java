package io.github.example_name;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ArrayDeque;
import java.util.Deque;

public class Island {
    public static final int WIDTH = 170, HEIGHT = 130;

    // terrain: 0=water, 1=grass, 2=sand
    public static final int[][] DATA = new int[HEIGHT][WIDTH];

    // grass decor (1–3) + optional flip flags
    public static final int[][] DECOR = new int[HEIGHT][WIDTH];
    public static final boolean[][] FLIP = new boolean[HEIGHT][WIDTH];

    // flower decor (1–3) + optional flip flags
    public static final int[][] FLOWER = new int[HEIGHT][WIDTH];
    public static final boolean[][] FLOWER_FLIP = new boolean[HEIGHT][WIDTH];

    // NPCs and boat prop
    public static final List<NPC> NPCS = new ArrayList<>();
    public static Boat SAILBOAT = null;

    static {
        Random r = new Random(42);
        int cx = WIDTH / 2, cy = HEIGHT / 2;

        // --- main island generation ---
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double dx = (x - cx) / 1.2, dy = (y - cy) / 1.4;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double radius = 24, edge = smoothstep(radius + 3, radius - 6, dist);
                double noise = (r.nextDouble() - 0.5) * 4.0;
                DATA[y][x] = (dist + noise < radius * edge) ? 1 : 0;
            }
        }

        // --- extra small islets ---
        generateSmoothIsland(cx + 55, cy + 28, 9, 7, r);
        generateSmoothIsland(cx - 65, cy - 35, 10, 8, r);
        generateSmoothIsland(cx + 60, cy - 40, 12, 9, r);

        // --- sand border around land ---
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (DATA[y][x] == 0 && hasLandWithinRadius(x, y))
                    DATA[y][x] = 2;
            }
        }

        // --- decor scatter ---
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (DATA[y][x] == 1) {
                    if (r.nextDouble() < 0.12) {
                        DECOR[y][x] = r.nextInt(3) + 1;
                        FLIP[y][x] = r.nextBoolean();
                    }
                    if (r.nextDouble() < 0.04) {
                        FLOWER[y][x] = r.nextInt(3) + 1;
                        FLOWER_FLIP[y][x] = r.nextBoolean();
                    }
                }
            }
        }

        // --- vendor on a boat at the right shoreline of the MAIN island ---
        addVendorOnRightCoast();

        // NOTE: Removed the random ambient islander to avoid a "farmer" in the middle.
        // (No additional NPC is added here.)
    }

    private static boolean hasLandWithinRadius(int x, int y) {
        for (int dy = -3; dy <= 3; dy++)
            for (int dx = -3; dx <= 3; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && ny >= 0 && ny < HEIGHT && nx < WIDTH) {
                    if ((DATA[ny][nx] == 1 || DATA[ny][nx] == 2) && Math.sqrt(dx * dx + dy * dy) <= 3)
                        return true;
                }
            }
        return false;
    }

    private static void generateSmoothIsland(int cx, int cy, int rx, int ry, Random r) {
        for (int y = Math.max(0, cy - ry - 3); y < Math.min(HEIGHT, cy + ry + 3); y++) {
            for (int x = Math.max(0, cx - rx - 3); x < Math.min(WIDTH, cx + rx + 3); x++) {
                double dx = (x - cx) / (double) rx, dy = (y - cy) / (double) ry;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double noise = (r.nextDouble() - 0.5) * 0.5;
                if (dist + noise < 1.0) DATA[y][x] = 1;
            }
        }
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    public enum NPCType { GENERIC, FARMER, VENDOR }

    public static class Boat {
        public int x, y; // tile coordinates
        public Boat(int x, int y) { this.x = x; this.y = y; }
    }

    // Place vendor on a WATER tile directly off the RIGHT shoreline of the MAIN island
    private static void addVendorOnRightCoast() {
        // 1) Find a central land tile (main island seed)
        int sx = WIDTH / 2, sy = HEIGHT / 2;
        boolean foundStart = false;
        int searchRadius = Math.max(WIDTH, HEIGHT);
        for (int rad = 0; rad < searchRadius && !foundStart; rad++) {
            for (int dy = -rad; dy <= rad && !foundStart; dy++) {
                for (int dx = -rad; dx <= rad; dx++) {
                    int x = sx + dx, y = sy + dy;
                    if (x >= 0 && y >= 0 && x < WIDTH && y < HEIGHT) {
                        if (DATA[y][x] == 1 || DATA[y][x] == 2) { // grass or sand
                            sx = x; sy = y; foundStart = true;
                        }
                    }
                }
            }
        }

        int bestX = -1, bestY = -1;
        if (foundStart) {
            // 2) BFS over connected main-island land (4-neighbors), collect right shoreline
            boolean[][] vis = new boolean[HEIGHT][WIDTH];
            Deque<int[]> q = new ArrayDeque<>();
            q.add(new int[]{sx, sy});
            vis[sy][sx] = true;

            while (!q.isEmpty()) {
                int[] p = q.poll();
                int x = p[0], y = p[1];

                // Is this a right shoreline cell? (land with water immediately to the right)
                if (x + 1 < WIDTH && (DATA[y][x] == 1 || DATA[y][x] == 2) && DATA[y][x + 1] == 0) {
                    if (x > bestX) { bestX = x; bestY = y; }
                }

                // 4-way neighbors across land (grass/sand)
                int[][] d4 = {{1,0},{-1,0},{0,1},{0,-1}};
                for (int[] d : d4) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx>=0 && ny>=0 && nx<WIDTH && ny<HEIGHT && !vis[ny][nx]) {
                        if (DATA[ny][nx] == 1 || DATA[ny][nx] == 2) {
                            vis[ny][nx] = true;
                            q.add(new int[]{nx, ny});
                        }
                    }
                }
            }
        }

        int shoreX, shoreY;
        if (bestX != -1) {
            shoreX = bestX; shoreY = bestY;
        } else {
            // 3) Fallback: find any land near right side with water to its right
            shoreX = WIDTH / 2; shoreY = HEIGHT / 2;
            outer:
            for (int x = WIDTH - 3; x >= 2; x--) {
                for (int y = 2; y < HEIGHT - 2; y++) {
                    int here = DATA[y][x];
                    if ((here == 1 || here == 2) && x + 1 < WIDTH && DATA[y][x + 1] == 0) {
                        shoreX = x; shoreY = y;
                        break outer;
                    }
                }
            }
        }

        int boatX = Math.min(WIDTH - 1, shoreX + 1); // the water tile at right edge
        int boatY = shoreY;

        // vendor "lives" on the boat tile (for proximity checks / trading)
        NPC vendor = new NPC("Vendor", boatX, boatY, "Fresh goods from the sea!", NPCType.VENDOR);
        NPCS.add(vendor);

        // boat prop at same tile for rendering
        SAILBOAT = new Boat(boatX, boatY);
    }

    public static class NPC {
        public String name;
        public int x, y;       // tile coords
        public String dialogue;
        public NPCType type;

        public NPC(String name, int x, int y, String dialogue) {
            this(name, x, y, dialogue, NPCType.GENERIC);
        }

        public NPC(String name, int x, int y, String dialogue, NPCType type) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.dialogue = dialogue;
            this.type = type;
        }

        public void talk() {
            System.out.println(name + ": " + dialogue);
        }
    }
}
