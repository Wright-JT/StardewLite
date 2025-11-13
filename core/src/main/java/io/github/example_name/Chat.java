package io.github.example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.graphics.GL20;

import java.util.ArrayList;
import java.util.List;

public class Chat {

    private final List<String> messages = new ArrayList<>();
    private final StringBuilder input = new StringBuilder();

    private boolean active = false;
    private static final int MAX_CHAT_LINES = 50;

    // Layout
    private final int width = 420;
    private final int height = 180;
    private final int x = 20;
    private final int y = 80;

    public Chat() {
        // Optional: start with a system message
        addMessage("System: Press ENTER to toggle chat.");
    }

    /** Called each frame from Core.render() to handle keyboard input. */
    public void update() {
        // Toggle chat focus / send message on ENTER
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (active) {
                // Sending a message
                String msg = input.toString().trim();
                if (!msg.isEmpty()) {
                    addMessage("You: " + msg);
                }
                input.setLength(0);
                active = false;
            } else {
                // Activate chat box
                active = true;
            }
        }

        if (!active) return;

        // When active, capture simple text input
        handleTyping();
    }

    private void handleTyping() {
        // Letters A–Z
        for (int key = Input.Keys.A; key <= Input.Keys.Z; key++) {
            if (Gdx.input.isKeyJustPressed(key)) {
                char c = (char) ('a' + (key - Input.Keys.A));
                input.append(c);
            }
        }

        // Numbers 0–9 (top row)
        for (int key = Input.Keys.NUM_0; key <= Input.Keys.NUM_9; key++) {
            if (Gdx.input.isKeyJustPressed(key)) {
                char c = (char) ('0' + (key - Input.Keys.NUM_0));
                input.append(c);
            }
        }

        // Space
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            input.append(' ');
        }

        // Backspace
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && input.length() > 0) {
            input.deleteCharAt(input.length() - 1);
        }
    }

    /** Add a line to the chat history (for local or future network messages). */
    public void addMessage(String msg) {
        messages.add(msg);
        if (messages.size() > MAX_CHAT_LINES) {
            messages.remove(0);
        }
    }

    /** Draws the chat overlay in screen space (call at the end of Core.render()). */
    public void draw(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        // Switch to screen-space projection
        Matrix4 uiMatrix = new Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()
        );

        shapeRenderer.setProjectionMatrix(uiMatrix);
        batch.setProjectionMatrix(uiMatrix);

        // Background panel (with alpha)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.25f);  // 25% opaque
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);


        batch.begin();
        Color oldColor = font.getColor().cpy();

        // Title
        font.setColor(Color.WHITE);
        font.draw(batch, "Chat", x + 8, y + height - 8);

        // Show last N lines
        int visibleLines = 7;
        int lineHeight = 20;
        int count = Math.min(visibleLines, messages.size());
        for (int i = 0; i < count; i++) {
            String line = messages.get(messages.size() - 1 - i);
            font.draw(batch, line, x + 8, y + height - 24 - i * lineHeight);
        }

        // Input line / hint
        if (active) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "> " + input.toString() + "_", x + 8, y + 18);
        } else {
            font.setColor(Color.GRAY);
            font.draw(batch, "Press ENTER to chat", x + 8, y + 18);
        }

        font.setColor(oldColor);
        batch.end();
    }

    /** Returns whether chat is currently focused (used to disable movement). */
    public boolean isActive() {
        return active;
    }
}
