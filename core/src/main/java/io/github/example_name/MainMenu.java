package io.github.example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import javax.swing.JOptionPane;

public class MainMenu extends ApplicationAdapter {

    // Menu rendering
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Texture menuBackground;

    private Host host;
    private Client client;

    private String joinUsername;
    private String joinIp;

    // Game instance (your existing game)
    private Core core;
    private boolean inGame = false;
    private boolean coreCreated = false;

    // Button data
    private static class Button {
        float x, y, w, h;
        String label;
    }
    private Texture titleTexture;
    private float titleScale = 0.65f;   // 70% size

    private Button hostButton;
    private Button joinButton;
    private Button exitButton;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        menuBackground = new Texture(Gdx.files.internal("menu.png"));
        titleTexture = new Texture(Gdx.files.internal("menutitle.png")); // <-- NEW

        createButtons();
    }


    private void createButtons() {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        float buttonWidth = 260f;
        float buttonHeight = 60f;
        float spacing = 18f;

        float centerX = (screenW - buttonWidth) / 2f;

        // Move buttons DOWN by +120px
        float baseY = screenH / 2f - 120f;

        hostButton = new Button();
        hostButton.x = centerX;
        hostButton.y = baseY + buttonHeight + spacing;
        hostButton.w = buttonWidth;
        hostButton.h = buttonHeight;
        hostButton.label = "Host";

        joinButton = new Button();
        joinButton.x = centerX;
        joinButton.y = baseY;
        joinButton.w = buttonWidth;
        joinButton.h = buttonHeight;
        joinButton.label = "Join";

        exitButton = new Button();
        exitButton.x = centerX;
        exitButton.y = baseY - buttonHeight - spacing;
        exitButton.w = buttonWidth;
        exitButton.h = buttonHeight;
        exitButton.label = "Exit";
    }


    @Override
    public void render() {
        // If game is running, just delegate to Core
        if (inGame) {
            if (!coreCreated) {
                // Safety: make sure Core is properly initialized
                core = new Core();
                core.create();
                coreCreated = true;
            }
            core.render();
            return;
        }

        renderMenu();
    }

    private void renderMenu() {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        // Simple clear
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);

        // Screen-space projection
        Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, screenW, screenH);
        batch.setProjectionMatrix(uiMatrix);
        shapeRenderer.setProjectionMatrix(uiMatrix);

        // Draw background (stretched to window)
        batch.begin();
        batch.draw(menuBackground, 0, 0, screenW, screenH);
        batch.end();

        // Draw title centered above buttons
        batch.begin();
        if (titleTexture != null) {

            float scale = titleScale; // easy to tweak
            float titleW = titleTexture.getWidth() * scale;
            float titleH = titleTexture.getHeight() * scale;

            float titleX = (screenW - titleW) / 2f;
            float titleY = hostButton.y + hostButton.h + 50f;

            batch.draw(titleTexture, titleX, titleY, titleW, titleH);
        }
        batch.end();

        // Draw semi-transparent button panels
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        drawButtonPanel(hostButton);
        drawButtonPanel(joinButton);
        drawButtonPanel(exitButton);

        shapeRenderer.end();
        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        // Draw button labels
        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.4f);

        drawButtonLabel(hostButton);
        drawButtonLabel(joinButton);
        drawButtonLabel(exitButton);

        font.getData().setScale(1.0f);
        batch.end();

        handleMenuInput();
    }

    private void drawButtonPanel(Button b) {
        // Outer darker panel
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(b.x, b.y, b.w, b.h);

        // Subtle highlight stripe at top
        shapeRenderer.setColor(1f, 1f, 1f, 0.15f);
        shapeRenderer.rect(b.x, b.y + b.h - 4, b.w, 4);
    }

    private void drawButtonLabel(Button b) {
        GlyphLayout layout = new GlyphLayout(font, b.label);
        float textX = b.x + (b.w - layout.width) / 2f;
        float textY = b.y + (b.h + layout.height) / 2f;
        font.draw(batch, layout, textX, textY);
    }

    private void handleMenuInput() {
        // Only react to a LEFT mouse click
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;

        int screenH = Gdx.graphics.getHeight();
        int mx = Gdx.input.getX();
        int my = screenH - Gdx.input.getY(); // invert Y (top-left -> bottom-left)

        if (isInside(mx, my, hostButton)) {
            // HOST: start game + start server
            if (!coreCreated) {
                core = new Core();
                core.create();
                coreCreated = true;
            }

            // Start the chat server (only once)
            if (host == null) {
                host = new Host(
                    5000,
                    message -> Gdx.app.postRunnable(() -> {
                        if (core != null) {
                            core.receiveNetworkMessage(message);
                        }
                    })
                );
                host.start();
            }

            // Local player uses the host as the network backend
            core.setNetwork(host, null);
            inGame = true;

        } else if (isInside(mx, my, joinButton)) {
            // JOIN: ask for username and IP using JOptionPane dialogs

            String username = JOptionPane.showInputDialog(
                null,
                "Enter Username:",
                "Join Game",
                JOptionPane.QUESTION_MESSAGE
            );
            if (username == null || username.trim().isEmpty()) {
                return; // cancelled or empty
            }
            username = username.trim();

            String ip = JOptionPane.showInputDialog(
                null,
                "Enter Host IP:",
                "Join Game",
                JOptionPane.QUESTION_MESSAGE
            );
            if (ip == null || ip.trim().isEmpty()) {
                return; // cancelled or empty
            }
            ip = ip.trim();

            // Create Core if needed
            if (!coreCreated) {
                core = new Core();
                core.create();
                coreCreated = true;
            }

            // Create client
            client = new Client(
                ip,
                5000,
                username,
                message -> Gdx.app.postRunnable(() -> {
                    if (core != null) {
                        core.receiveNetworkMessage(message);
                    }
                })
            );

            // 🔑 Try to connect; only enter game if it succeeds
            boolean connected = client.connect();
            if (!connected) {
                JOptionPane.showMessageDialog(
                    null,
                    "Failed to join: could not connect to host.",
                    "Join Failed",
                    JOptionPane.ERROR_MESSAGE
                );
                client = null;
                return;
            }

            // Success: wire into Core and enter the game
            core.setNetwork(null, client);
            inGame = true;
        }
    }



    private boolean isInside(int mx, int my, Button b) {
        return mx >= b.x && mx <= b.x + b.w &&
            my >= b.y && my <= b.y + b.h;
    }

    @Override
    public void resize(int width, int height) {
        if (!inGame) {
            createButtons(); // recompute button positions for new size
        } else if (coreCreated) {
            core.resize(width, height);
        }
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (menuBackground != null) menuBackground.dispose();
        if (coreCreated && core != null) core.dispose();
    }
}
