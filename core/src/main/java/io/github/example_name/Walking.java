package io.github.example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Input;

/**
 * Handles the farmer's walking animation and current sprite frame.
 *
 * Assets used (must be in assets/):
 *  - standstill.png
 *  - leftwalk1.png
 *  - rightwalk1.png
 *  - walkdown1.png
 *  - walkdown2.png
 *  - walkup1.png
 *  - walkup2.png
 */
public class Walking {

    private enum Direction {
        STAND, LEFT, RIGHT, UP, DOWN
    }

    private final Texture standstill;
    private final Texture leftwalk;
    private final Texture rightwalk;
    private final Texture walkdown1;
    private final Texture walkdown2;
    private final Texture walkup1;
    private final Texture walkup2;

    private Direction currentDir = Direction.STAND;
    private float animTimer = 0f;
    private Texture currentFrame;

    public Walking() {
        standstill = new Texture(Gdx.files.internal("standstill.png"));
        leftwalk   = new Texture(Gdx.files.internal("leftwalk1.png"));
        rightwalk  = new Texture(Gdx.files.internal("rightwalk1.png"));
        walkdown1  = new Texture(Gdx.files.internal("walkdown1.png"));
        walkdown2  = new Texture(Gdx.files.internal("walkdown2.png"));
        walkup1    = new Texture(Gdx.files.internal("walkup1.png"));
        walkup2    = new Texture(Gdx.files.internal("walkup2.png"));

        currentFrame = standstill;
    }

    /**
     * Update animation based on movement flags.
     *
     * Priority: left/right > up/down when diagonal.
     * walkUp / walkDown alternate frame every 1 second.
     */
    public void update(float delta,
                       boolean movingLeft,
                       boolean movingRight,
                       boolean movingUp,
                       boolean movingDown) {

        Direction newDir;

        // left/right have priority if pressed
        if (movingLeft && !movingRight) {
            newDir = Direction.LEFT;
        } else if (movingRight && !movingLeft) {
            newDir = Direction.RIGHT;
        } else if (movingUp && !movingDown) {
            newDir = Direction.UP;
        } else if (movingDown && !movingUp) {
            newDir = Direction.DOWN;
        } else {
            newDir = Direction.STAND;
        }

        if (newDir != currentDir) {
            currentDir = newDir;
            animTimer = 0f;
        } else {
            animTimer += delta;
        }

        switch (currentDir) {
            case LEFT:
                currentFrame = leftwalk;
                break;

            case RIGHT:
                currentFrame = rightwalk;
                break;

            case UP:
                currentFrame = ((int)(animTimer / 0.5f) % 2 == 0) ? walkup1 : walkup2;
                break;

            case DOWN:
                currentFrame = ((int)(animTimer / 0.5f) % 2 == 0) ? walkdown1 : walkdown2;
                break;

            case STAND:
            default:
                currentFrame = standstill;
                break;
        }
    }

    public Texture getCurrentFrame() {
        return currentFrame;
    }

    public void dispose() {
        standstill.dispose();
        leftwalk.dispose();
        rightwalk.dispose();
        walkdown1.dispose();
        walkdown2.dispose();
        walkup1.dispose();
        walkup2.dispose();
    }
}
