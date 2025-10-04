package io.github.example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class Core extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private final int TILE_SIZE = 32;
    private final int GRID_WIDTH = 40;
    private final int GRID_HEIGHT = 32;
    private OrthographicCamera camera;
    @Override
    public void create() {
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(Gdx.graphics.getWidth()/2f, Gdx.graphics.getHeight()/2f, 0);
        camera.update();

        shapeRenderer = new ShapeRenderer();
        // initialize farm grid
        farm = new TileState[GRID_WIDTH][GRID_HEIGHT];
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                farm[x][y] = TileState.EMPTY; // default grass
            }
        }
    }

    private int playerX = 5;
    private int playerY = 4;
    private TileState[][] farm;
    @Override
    public void render() {

        //Code for camera following the player
        //float halfWidth = camera.viewportWidth / 2f;
        //float halfHeight = camera.viewportHeight / 2f;
        //camera.position.x = Math.max(halfWidth, Math.min(playerX * TILE_SIZE + TILE_SIZE/2f, GRID_WIDTH * TILE_SIZE - halfWidth));
        //camera.position.y = Math.max(halfHeight, Math.min(playerY * TILE_SIZE + TILE_SIZE/2f, GRID_HEIGHT * TILE_SIZE - halfHeight));
        //camera.update();

        //Comment out this camera code and uncomment out the above code to have camera follow player.
        camera.position.set(playerX * TILE_SIZE + TILE_SIZE/2f, playerY * TILE_SIZE + TILE_SIZE/2f, 0);
        camera.update();

        shapeRenderer.setProjectionMatrix(camera.combined);
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        // handle input once per frame
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.LEFT)) playerX--;
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.RIGHT)) playerX++;
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) playerY++;
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) playerY--;

        // clamp to grid boundaries
        playerX = Math.max(0, Math.min(playerX, GRID_WIDTH - 1));
        playerY = Math.max(0, Math.min(playerY, GRID_HEIGHT - 1));

        // plant crop if space is pressed
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
            if (farm[playerX][playerY] == TileState.EMPTY) {
                farm[playerX][playerY] = TileState.PLANTED;
            }
        }


        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                if (farm[x][y] == TileState.EMPTY) {
                    shapeRenderer.setColor(0.2f, 0.6f, 0.2f, 1); // green = grass
                } else if (farm[x][y] == TileState.PLANTED) {
                    shapeRenderer.setColor(0.55f, 0.27f, 0.07f, 1); // brown = planted dirt
                }
                shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // draw player
        shapeRenderer.setColor(1, 0, 0, 1);
        shapeRenderer.rect(playerX * TILE_SIZE, playerY * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        shapeRenderer.end();
    }


    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
    enum TileState {
        EMPTY,
        PLANTED
    }
}
