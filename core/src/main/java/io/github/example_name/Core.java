package io.github.example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import java.util.Arrays;
import java.util.Random;

public class Core extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;

    private Texture playerTexture, grassTexture, sandTexture, wheatTexture, carrotTexture, houseTexture;
    private TiledMap oceanMap;
    private OrthogonalTiledMapRenderer oceanRenderer;

    private final int[][] ISLAND_MAP = IslandMap.DATA;
    private final int GRID_HEIGHT = IslandMap.DATA.length;
    private final int GRID_WIDTH = IslandMap.DATA[0].length;

    private float playerX, playerY;
    private final int TILE_SIZE = 32;
    private float playerWidth, playerHeight;

    private TileState[][] farm;
    private Crop[][] crops;

    private final int HOTBAR_SLOTS = 9;
    private int selectedSlot = 0;
    private final int[] inventory = new int[HOTBAR_SLOTS];
    private final String[] inventoryItems = new String[HOTBAR_SLOTS];

    enum TileState { EMPTY, TILLED }
    enum CropType { WHEAT, CARROT }

    static class Crop {
        float growTime;
        CropType type;
        boolean fullyGrown;

        Crop(CropType type) {
            this.type = type;
            this.growTime = 0f;
            this.fullyGrown = false;
        }

        void update(float delta) {
            growTime += delta;
            if (growTime >= 20f) fullyGrown = true;
        }

        float getGrowthPercent() { return Math.min(growTime / 20f, 1f); }
        float getSize() { return 0.1f + 0.8f * getGrowthPercent(); }
    }

    @Override
    public void create() {
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0);
        camera.update();

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        playerTexture = new Texture(Gdx.files.internal("farmer.png"));
        grassTexture = new Texture(Gdx.files.internal("grass.png"));
        sandTexture = new Texture(Gdx.files.internal("sand.png"));
        wheatTexture = new Texture(Gdx.files.internal("wheat.png"));
        carrotTexture = new Texture(Gdx.files.internal("carrot.png"));
        houseTexture = new Texture(Gdx.files.internal("house.png"));

        oceanMap = new TmxMapLoader().load("ocean.tmx");
        oceanRenderer = new OrthogonalTiledMapRenderer(oceanMap, TILE_SIZE / 8f);

        float PLAYER_SCALE = 2.2f;
        playerWidth = TILE_SIZE * PLAYER_SCALE;
        playerHeight = TILE_SIZE * PLAYER_SCALE;

        farm = new TileState[GRID_WIDTH][GRID_HEIGHT];
        crops = new Crop[GRID_WIDTH][GRID_HEIGHT];
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++)
                farm[x][y] = TileState.EMPTY;

        Arrays.fill(inventory, 0);

        // --- find spawn on land near center ---
        for (int y = GRID_HEIGHT / 2 - 5; y < GRID_HEIGHT / 2 + 5; y++) {
            for (int x = GRID_WIDTH / 2 - 5; x < GRID_WIDTH / 2 + 5; x++) {
                if (ISLAND_MAP[y][x] == 1) {
                    playerX = x * TILE_SIZE;
                    playerY = y * TILE_SIZE;
                    break;
                }
            }
        }

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (amountY > 0) selectedSlot = (selectedSlot + 1) % HOTBAR_SLOTS;
                else if (amountY < 0) selectedSlot = (selectedSlot - 1 + HOTBAR_SLOTS) % HOTBAR_SLOTS;
                return true;
            }
        });
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        camera.position.set(playerX + TILE_SIZE / 2f, playerY + TILE_SIZE / 2f, 0);
        camera.update();

        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        float PLAYER_SPEED = 150f;
        float nextX = playerX, nextY = playerY;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) nextX -= PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) nextX += PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) nextY += PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) nextY -= PLAYER_SPEED * delta;

        int tx = (int) (nextX / TILE_SIZE);
        int ty = (int) (nextY / TILE_SIZE);
        if (inBounds(tx, ty) && ISLAND_MAP[ty][tx] != 0) { // walkable on grass or sand
            playerX = nextX;
            playerY = nextY;
        }

        oceanRenderer.setView(camera);
        oceanRenderer.render();

        // --- tilling & harvesting ---
        if (Gdx.input.justTouched()) {
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouse);
            int mx = (int) (mouse.x / TILE_SIZE);
            int my = (int) (mouse.y / TILE_SIZE);
            if (inBounds(mx, my)) {
                float distX = Math.abs((int) (playerX / TILE_SIZE) - mx);
                float distY = Math.abs((int) (playerY / TILE_SIZE) - my);
                if (distX <= 2 && distY <= 2)
                    handleTileAction(mx, my);
            }
        }

        // inside Core.java – updated planting + inventory highlight section only

        // --- planting seeds ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            int targetX = (int) (playerX / TILE_SIZE);
            int targetY = (int) (playerY / TILE_SIZE);

            // Plant directly where the player is standing (optional: you can offset 1 tile forward later)
            if (inBounds(targetX, targetY) && farm[targetX][targetY] == TileState.TILLED && crops[targetX][targetY] == null) {
                CropType type = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) ? CropType.WHEAT : CropType.CARROT;
                crops[targetX][targetY] = new Crop(type);
            }
        }


        // update crops
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++)
                if (crops[x][y] != null) crops[x][y].update(delta);

        // draw land
        batch.begin();
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (ISLAND_MAP[y][x] == 1)
                    batch.draw(grassTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                else if (ISLAND_MAP[y][x] == 2)
                    batch.draw(sandTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
        batch.end();

        // draw soil and crops
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (ISLAND_MAP[y][x] != 1) continue;
                if (farm[x][y] == TileState.TILLED) {
                    shapeRenderer.setColor(0.55f, 0.27f, 0.07f, 1);
                    shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
                Crop c = crops[x][y];
                if (c != null) {
                    float size = c.getSize() * TILE_SIZE;
                    float offset = (TILE_SIZE - size) / 2f;
                    if (c.type == CropType.WHEAT) shapeRenderer.setColor(1f, 1f, 0f, 1f);
                    else shapeRenderer.setColor(1f, 0.55f, 0.1f, 1f);
                    shapeRenderer.rect(x * TILE_SIZE + offset, y * TILE_SIZE + offset, size, size);
                }
            }
        shapeRenderer.end();

        // player
        batch.begin();
        batch.draw(playerTexture,
            playerX - (playerWidth - TILE_SIZE) / 2f,
            playerY - (playerHeight - TILE_SIZE) / 2f,
            playerWidth, playerHeight);
        batch.end();

        drawInventory();
    }

    private void handleTileAction(int tileX, int tileY) {
        if (!inBounds(tileX, tileY)) return;
        int tileType = ISLAND_MAP[tileY][tileX];
        if (tileType == 0 || tileType == 2) return;

        TileState current = farm[tileX][tileY];
        Crop crop = crops[tileX][tileY];

        if (current == TileState.EMPTY) farm[tileX][tileY] = TileState.TILLED;
        else if (crop != null && crop.fullyGrown) {
            int slotIndex = -1;
            for (int i = 0; i < HOTBAR_SLOTS; i++)
                if (inventoryItems[i] == null || inventoryItems[i].equals(crop.type.toString())) {
                    slotIndex = i; break;
                }
            if (slotIndex != -1) {
                if (inventoryItems[slotIndex] == null)
                    inventoryItems[slotIndex] = crop.type.toString();
                inventory[slotIndex]++;
            }
            crops[tileX][tileY] = null;
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < GRID_WIDTH && y < GRID_HEIGHT;
    }

    private void drawInventory() {
        int slotSize = 40, spacing = 10;
        int totalWidth = HOTBAR_SLOTS * slotSize + (HOTBAR_SLOTS - 1) * spacing;
        int startX = (Gdx.graphics.getWidth() - totalWidth) / 2, y = 20;

        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int xPos = startX + i * (slotSize + spacing);
            int border = 2;

            // --- black border ---
            shapeRenderer.setColor(0f, 0f, 0f, 1f);
            shapeRenderer.rect(xPos - border, y - border, slotSize + border * 2, slotSize + border * 2);

            // --- slot background ---
            if (i == selectedSlot)
                shapeRenderer.setColor(0.7f, 0.7f, 0.7f, 1f); // slightly lighter gray
            else
                shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f);
            shapeRenderer.rect(xPos, y, slotSize, slotSize);
        }
        shapeRenderer.end();

        // Draw inventory item textures and counts
        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.begin();
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            if (inventory[i] > 0 && inventoryItems[i] != null) {
                Texture tex = inventoryItems[i].equals("WHEAT") ? wheatTexture :
                    inventoryItems[i].equals("CARROT") ? carrotTexture : null;
                if (tex != null) {
                    float size = slotSize * 0.9f, off = (slotSize - size) / 2f;
                    batch.draw(tex, startX + i * (slotSize + spacing) + off, y + off, size, size);
                }
                font.draw(batch, String.valueOf(inventory[i]),
                    startX + i * (slotSize + spacing) + slotSize - 14, y + slotSize - 6);
            }
        }
        batch.end();
}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
        playerTexture.dispose();
        grassTexture.dispose();
        sandTexture.dispose();
        wheatTexture.dispose();
        carrotTexture.dispose();
        houseTexture.dispose();
        oceanMap.dispose();
        oceanRenderer.dispose();
    }

    // === ISLAND GENERATION ===
    public static class IslandMap {
        public static final int WIDTH = 160, HEIGHT = 100;
        public static final int[][] DATA = new int[HEIGHT][WIDTH];

        static {
            Random r = new Random(42);
            int cx = WIDTH / 2, cy = HEIGHT / 2;

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    double dx = (x - cx) / 1.2, dy = (y - cy) / 1.4;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double radius = 24, edge = smoothstep(radius + 3, radius - 6, dist);
                    double noise = (r.nextDouble() - 0.5) * 4.0;
                    DATA[y][x] = (dist + noise < radius * edge) ? 1 : 0;
                }
            }

            generateSmoothIsland(cx + 55, cy + 28, 9, 7, r);
            generateSmoothIsland(cx - 65, cy - 35, 10, 8, r);
            generateSmoothIsland(cx + 60, cy - 40, 12, 9, r);

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    if (DATA[y][x] == 0 && hasLandWithinRadius(x, y))
                        DATA[y][x] = 2;
                }
            }
        }

        private static boolean hasLandWithinRadius(int x, int y) {
            for (int dy = -3; dy <= 3; dy++)
                for (int dx = -3; dx <= 3; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx >= 0 && ny >= 0 && ny < IslandMap.DATA.length && nx < IslandMap.DATA[0].length)
                        if (IslandMap.DATA[ny][nx] == 1 && Math.sqrt(dx * dx + dy * dy) <= 3)
                            return true;
                }
            return false;
        }

        private static void generateSmoothIsland(int cx, int cy, int rx, int ry, Random r) {
            for (int y = Math.max(0, cy - ry - 3); y < Math.min(IslandMap.DATA.length, cy + ry + 3); y++)
                for (int x = Math.max(0, cx - rx - 3); x < Math.min(IslandMap.DATA[0].length, cx + rx + 3); x++) {
                    double dx = (x - cx) / (double) rx, dy = (y - cy) / (double) ry;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double noise = (r.nextDouble() - 0.5) * 0.25;
                    double edge = smoothstep(1.0, 0.7, dist + noise);
                    if (edge > 0.5) IslandMap.DATA[y][x] = 1;
                }
        }

        private static double smoothstep(double e0, double e1, double x) {
            double t = clamp((x - e0) / (e1 - e0));
            return t * t * (3 - 2 * t);
        }

        private static double clamp(double val) {
            return Math.max(0.0, Math.min(1.0, val));
        }
    }
}
