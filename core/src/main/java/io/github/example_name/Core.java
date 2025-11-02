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
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Color;


import java.util.Arrays;

public class Core extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private final int GRID_WIDTH = 40;
    private final int GRID_HEIGHT = 32;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;

    private Texture playerTexture;
    private Texture grassTexture;
    private Texture wheatTexture;
    private Texture carrotTexture;

    private float playerX = 5 * 32;
    private float playerY = 4 * 32;
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

        float getSize() {
            float min = 0.1f;
            float max = 0.9f;
            return min + (max - min) * getGrowthPercent();
        }
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

        CurrencyManager.setCurrency(100);

        playerTexture = new Texture(Gdx.files.internal("farmer.png"));
        grassTexture = new Texture(Gdx.files.internal("grass.png"));
        wheatTexture = new Texture(Gdx.files.internal("wheat.png"));
        carrotTexture = new Texture(Gdx.files.internal("carrot.png"));

        playerWidth = TILE_SIZE * PLAYER_SCALE;
        playerHeight = TILE_SIZE * PLAYER_SCALE;

        farm = new TileState[GRID_WIDTH][GRID_HEIGHT];
        crops = new Crop[GRID_WIDTH][GRID_HEIGHT];
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++) {
                farm[x][y] = TileState.EMPTY;
                crops[x][y] = null;
            }

        Arrays.fill(inventory, 0);

        // --- Mouse scroll listener for inventory slots ---
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (amountY > 0)
                    selectedSlot = (selectedSlot + 1) % HOTBAR_SLOTS;
                else if (amountY < 0)
                    selectedSlot = (selectedSlot - 1 + HOTBAR_SLOTS) % HOTBAR_SLOTS;
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
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  playerX -= PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) playerX += PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    playerY += PLAYER_SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  playerY -= PLAYER_SPEED * delta;

        playerX = Math.max(0, Math.min(playerX, GRID_WIDTH * TILE_SIZE - TILE_SIZE));
        playerY = Math.max(0, Math.min(playerY, GRID_HEIGHT * TILE_SIZE - TILE_SIZE));

        // --- SPACE key till/harvest ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            int tileX = (int)(playerX / TILE_SIZE);
            int tileY = (int)(playerY / TILE_SIZE);
            handleTileAction(tileX, tileY);
        }

        // --- Mouse click till/harvest ---
        if (Gdx.input.justTouched()) {
            Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouse);

            int clickedTileX = (int)(mouse.x / TILE_SIZE);
            int clickedTileY = (int)(mouse.y / TILE_SIZE);

            float distX = Math.abs((int)(playerX / TILE_SIZE) - clickedTileX);
            float distY = Math.abs((int)(playerY / TILE_SIZE) - clickedTileY);
            boolean closeEnough = (distX <= 2 && distY <= 2);

            if (clickedTileX >= 0 && clickedTileX < GRID_WIDTH && clickedTileY >= 0 && clickedTileY < GRID_HEIGHT && closeEnough)
                handleTileAction(clickedTileX, clickedTileY);
        }

        // --- Plant crops ---
        int tileX = (int)(playerX / TILE_SIZE);
        int tileY = (int)(playerY / TILE_SIZE);
        tileX = Math.max(0, Math.min(tileX, GRID_WIDTH - 1));
        tileY = Math.max(0, Math.min(tileY, GRID_HEIGHT - 1));

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) && farm[tileX][tileY] == TileState.TILLED)
            crops[tileX][tileY] = new Crop(CropType.WHEAT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) && farm[tileX][tileY] == TileState.TILLED)
            crops[tileX][tileY] = new Crop(CropType.CARROT);

        // --- Update crops ---
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++)
                if (crops[x][y] != null)
                    crops[x][y].update(delta);

        // --- Draw grass background ---
        batch.begin();
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (farm[x][y] == TileState.EMPTY)
                    batch.draw(grassTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
        batch.end();

        // --- Draw tilled soil & crops ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (farm[x][y] == TileState.TILLED) {
                    shapeRenderer.setColor(0.55f, 0.27f, 0.07f, 1);
                    shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }

                Crop crop = crops[x][y];
                if (crop != null) {
                    float size = crop.getSize() * TILE_SIZE;
                    float offset = (TILE_SIZE - size) / 2f;
                    if (crop.type == CropType.WHEAT) shapeRenderer.setColor(1f, 1f, 0f, 1f);
                    else if (crop.type == CropType.CARROT) shapeRenderer.setColor(1f, 0.55f, 0.1f, 1f);
                    shapeRenderer.rect(x * TILE_SIZE + offset, y * TILE_SIZE + offset, size, size);
                }
            }
        }
        shapeRenderer.end();

        // --- Draw player ---
        batch.begin();
        font.draw(batch, "Gold: " + CurrencyManager.getCurrency(), 10, 460);
        batch.draw(
            playerTexture,
            playerX - (playerWidth - TILE_SIZE) / 2f,
            playerY - (playerHeight - TILE_SIZE) / 2f,
            playerWidth,
            playerHeight
        );
        batch.end();

        // --- Draw inventory HUD ---

        drawInventory();
    }

    private void drawInventory() {
        int slotCount = HOTBAR_SLOTS;
        int slotSize = 40;
        int spacing = 10;
        int totalWidth = slotCount * slotSize + (slotCount - 1) * spacing;
        int startX = (Gdx.graphics.getWidth() - totalWidth) / 2;
        int y = 20;

        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < slotCount; i++) {
            if (i == selectedSlot) shapeRenderer.setColor(0.7f, 0.7f, 0.7f, 1);
            else shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1);
            shapeRenderer.rect(startX + i * (slotSize + spacing), y, slotSize, slotSize);
        }
        shapeRenderer.end();

        batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.begin();
        for (int i = 0; i < slotCount; i++) {
            if (inventory[i] > 0 && inventoryItems[i] != null) {
                Texture tex = null;
                if (inventoryItems[i].equals("WHEAT")) tex = wheatTexture;
                else if (inventoryItems[i].equals("CARROT")) tex = carrotTexture;

                if (tex != null) {
                    float cropSize = slotSize * 0.9f;
                    float offset = (slotSize - cropSize) / 2f;
                    batch.draw(tex, startX + i * (slotSize + spacing) + offset, y + offset, cropSize, cropSize);
                }

                String text = String.valueOf(inventory[i]);
                font.draw(batch, text, startX + i * (slotSize + spacing) + slotSize - 14, y + slotSize - 6);
            }
        }
        batch.end();
    }

    private void handleTileAction(int tileX, int tileY) {
        TileState current = farm[tileX][tileY];
        Crop crop = crops[tileX][tileY];

        if (current == TileState.EMPTY) {
            farm[tileX][tileY] = TileState.TILLED;
        } else if (current == TileState.TILLED && crop == null) {
            farm[tileX][tileY] = TileState.EMPTY;
        } else if (crop != null) {
            if (crop.fullyGrown) {
                int slotIndex = -1;
                for (int i = 0; i < HOTBAR_SLOTS; i++) {
                    if (inventoryItems[i] == null || inventoryItems[i].equals(crop.type.toString())) {
                        slotIndex = i;
                        break;
                    }
                }

                if (slotIndex != -1) {
                    if (inventoryItems[slotIndex] == null)
                        inventoryItems[slotIndex] = crop.type.toString();
                    inventory[slotIndex]++;
                }

                crops[tileX][tileY] = null;
                farm[tileX][tileY] = TileState.TILLED;
            } else {
                crops[tileX][tileY] = null;
            }
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
