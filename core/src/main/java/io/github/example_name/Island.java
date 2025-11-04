package io.github.example_name;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Island {
    public static final int WIDTH = 170, HEIGHT = 130;

    public static final int[][] DATA = new int[HEIGHT][WIDTH];      // terrain: 0=water, 1=grass, 2=sand
    public static final int[][] DECOR = new int[HEIGHT][WIDTH];     // grass decor (1–3)
    public static final boolean[][] FLIP = new boolean[HEIGHT][WIDTH];

    public static final int[][] FLOWER = new int[HEIGHT][WIDTH];    // flower decor (1–3)
    public static final boolean[][] FLOWER_FLIP = new boolean[HEIGHT][WIDTH];

    // --- NPC data ---
    public static final List<NPC> NPCS = new ArrayList<>();

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

        // --- generate small additional islands ---
        generateSmoothIsland(cx + 55, cy + 28, 9, 7, r);
        generateSmoothIsland(cx - 65, cy - 35, 10, 8, r);
        generateSmoothIsland(cx + 60, cy - 40, 12, 9, r);

        // --- create sand border around land ---
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (DATA[y][x] == 0 && hasLandWithinRadius(x, y))
                    DATA[y][x] = 2;
            }
        }

        // --- decorative grass and flower placement ---
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (DATA[y][x] == 1) {
                    // Grass patches (12% chance)
                    if (r.nextDouble() < 0.12) {
                        DECOR[y][x] = r.nextInt(3) + 1; // grass1–3
                        FLIP[y][x] = r.nextBoolean();
                    }

                    // Flowers (4% chance)
                    if (r.nextDouble() < 0.04) {
                        FLOWER[y][x] = r.nextInt(3) + 1; // flower1–3
                        FLOWER_FLIP[y][x] = r.nextBoolean();
                    }
                }
            }
        }

        // --- place NPC(s) ---
        addNPCs(r);
    }

    // --- helper methods ---
    private static boolean hasLandWithinRadius(int x, int y) {
        for (int dy = -3; dy <= 3; dy++)
            for (int dx = -3; dx <= 3; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && ny >= 0 && ny < HEIGHT && nx < WIDTH)
                    if (DATA[ny][nx] == 1 && Math.sqrt(dx * dx + dy * dy) <= 3)
                        return true;
            }
        return false;
    }

    private static void generateSmoothIsland(int cx, int cy, int rx, int ry, Random r) {
        for (int y = Math.max(0, cy - ry - 3); y < Math.min(HEIGHT, cy + ry + 3); y++)
            for (int x = Math.max(0, cx - rx - 3); x < Math.min(WIDTH, cx + rx + 3); x++) {
                double dx = (x - cx) / (double) rx, dy = (y - cy) / (double) ry;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double noise = (r.nextDouble() - 0.5) * 0.5;
                if (dist + noise < 1.0)
                    DATA[y][x] = 1;
            }
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }

    // --- NPC types ---
    public enum NPCType { GENERIC, FARMER }

    // --- NPC generation ---
    private static void addNPCs(Random r) {
        // Place a starter Farmer near the center on grass
        int fx = WIDTH / 2, fy = HEIGHT / 2;
        // find nearest grass tile
        int radius = 8;
        boolean placed = false;
        for (int rad = 0; rad <= radius && !placed; rad++) {
            for (int dy = -rad; dy <= rad && !placed; dy++) {
                for (int dx = -rad; dx <= rad && !placed; dx++) {
                    int nx = fx + dx, ny = fy + dy;
                    if (nx >= 0 && ny >= 0 && nx < WIDTH && ny < HEIGHT && DATA[ny][nx] == 1) {
                        NPC farmer = new NPC("Farmer", nx, ny,
                            "Howdy! I’ll buy your crops.", NPCType.FARMER);
                        NPCS.add(farmer);
                        placed = true;
                    }
                }
            }
        }

        // Optionally keep a generic islander elsewhere (random grass)
        int x, y;
        do {
            x = r.nextInt(WIDTH);
            y = r.nextInt(HEIGHT);
        } while (DATA[y][x] != 1);
        NPC generic = new NPC("Islander", x, y, "Welcome to our island!", NPCType.GENERIC);
        NPCS.add(generic);
    }

    // --- NPC class ---
    public static class NPC {
        public String name;
        public int x, y;       // tile coordinates
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
