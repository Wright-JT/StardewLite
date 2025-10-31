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
import java.util.Arrays;

public class Core extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private final int GRID_HEIGHT = IslandMap.DATA.length;
    private final int GRID_WIDTH = IslandMap.DATA[0].length;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;

    private final int[][] ISLAND_MAP = IslandMap.DATA;

    private Texture playerTexture;
    private Texture grassTexture;
    private Texture wheatTexture;
    private Texture carrotTexture;

    private float playerX;
    private float playerY;
    private final int TILE_SIZE = 32;
    private final float PLAYER_SCALE = 2.2f;
    private float playerWidth, playerHeight;

    private TileState[][] farm;
    private Crop[][] crops;

    private final int HOTBAR_SLOTS = 9;
    private int selectedSlot = 0;
    private final int[] inventory = new int[HOTBAR_SLOTS];
    private final String[] inventoryItems = new String[HOTBAR_SLOTS];

    enum TileState { EMPTY, TILLED, PLANTED }

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

    enum CropType { WHEAT, CARROT }

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
        wheatTexture = new Texture(Gdx.files.internal("wheat.png"));
        carrotTexture = new Texture(Gdx.files.internal("carrot.png"));

        playerWidth = TILE_SIZE * PLAYER_SCALE;
        playerHeight = TILE_SIZE * PLAYER_SCALE;

        farm = new TileState[GRID_WIDTH][GRID_HEIGHT];
        crops = new Crop[GRID_WIDTH][GRID_HEIGHT];
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++)
                farm[x][y] = TileState.EMPTY;

        Arrays.fill(inventory, 0);

        // --- find a grass tile near center for spawn ---
        for (int y = GRID_HEIGHT / 2 - 5; y < GRID_HEIGHT / 2 + 5; y++) {
            for (int x = GRID_WIDTH / 2 - 5; x < GRID_WIDTH / 2 + 5; x++) {
                if (ISLAND_MAP[y][x] == 1) {
                    playerX = x * TILE_SIZE;
                    playerY = y * TILE_SIZE;
                    break;
                }
            }
        }

        // --- scroll wheel for inventory slots ---
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
        float nextX = playerX;
        float nextY = playerY;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  nextX -= PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) nextX += PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    nextY += PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  nextY -= PLAYER_SPEED * delta;

        // prevent walking on water
        int tx = (int)(nextX / TILE_SIZE);
        int ty = (int)(nextY / TILE_SIZE);
        if (tx >= 0 && tx < GRID_WIDTH && ty >= 0 && ty < GRID_HEIGHT && ISLAND_MAP[ty][tx] == 1) {
            playerX = nextX;
            playerY = nextY;
        }

        // --- click to interact (within range only) ---
        if (Gdx.input.justTouched()) {
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouse);
            int mx = (int)(mouse.x / TILE_SIZE);
            int my = (int)(mouse.y / TILE_SIZE);

            if (inBounds(mx, my)) {
                float distX = Math.abs((int)(playerX / TILE_SIZE) - mx);
                float distY = Math.abs((int)(playerY / TILE_SIZE) - my);
                if (distX <= 2 && distY <= 2 && ISLAND_MAP[my][mx] == 1)
                    handleTileAction(mx, my);
            }
        }

        // --- space key interaction ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            int px = (int)(playerX / TILE_SIZE);
            int py = (int)(playerY / TILE_SIZE);
            if (inBounds(px, py) && ISLAND_MAP[py][px] == 1)
                handleTileAction(px, py);
        }

        // --- planting ---
        int ptx = (int)(playerX / TILE_SIZE);
        int pty = (int)(playerY / TILE_SIZE);
        if (inBounds(ptx, pty) && farm[ptx][pty] == TileState.TILLED) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
                crops[ptx][pty] = new Crop(CropType.WHEAT);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
                crops[ptx][pty] = new Crop(CropType.CARROT);
        }

        // --- update crops ---
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++)
                if (crops[x][y] != null) crops[x][y].update(delta);

        // --- draw map (water first, then grass texture) ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++)
                if (ISLAND_MAP[y][x] == 0) {
                    shapeRenderer.setColor(0f, 0.4f, 0.8f, 1f);
                    shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
        shapeRenderer.end();

        batch.begin();
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++)
                if (ISLAND_MAP[y][x] == 1)
                    batch.draw(grassTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        batch.end();

        // --- draw tilled & crops ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (ISLAND_MAP[y][x] == 0) continue;
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

        // --- draw player ---
        batch.begin();
        batch.draw(playerTexture,
            playerX - (playerWidth - TILE_SIZE) / 2f,
            playerY - (playerHeight - TILE_SIZE) / 2f,
            playerWidth, playerHeight);
        batch.end();

        drawInventory();
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
            shapeRenderer.setColor(i == selectedSlot ? 0.7f : 0.5f, 0.5f, 0.5f, 1);
            shapeRenderer.rect(startX + i * (slotSize + spacing), y, slotSize, slotSize);
        }
        shapeRenderer.end();

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

    private void handleTileAction(int tileX, int tileY) {
        if (ISLAND_MAP[tileY][tileX] == 0) return;

        TileState current = farm[tileX][tileY];
        Crop crop = crops[tileX][tileY];

        if (current == TileState.EMPTY) {
            farm[tileX][tileY] = TileState.TILLED;
        } else if (current == TileState.TILLED && crop == null) {
            farm[tileX][tileY] = TileState.EMPTY;
        } else if (crop != null) {
            if (crop.fullyGrown) {
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
                farm[tileX][tileY] = TileState.TILLED;
            } else crops[tileX][tileY] = null;
        }
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
        playerTexture.dispose();
        grassTexture.dispose();
        wheatTexture.dispose();
        carrotTexture.dispose();
    }
}
