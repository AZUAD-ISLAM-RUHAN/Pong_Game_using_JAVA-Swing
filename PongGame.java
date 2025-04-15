import java.awt.*; 
import java.awt.event.*; 
import java.util.Random; 
import javax.swing.*; 

public class PongGame extends JPanel implements ActionListener, KeyListener {
/*
ActionListener – for timer-based updates
KeyListener – for keyboard controls 
*/
    private static final int PADDLE_HEIGHT = 120; 
    private static final int PADDLE_WIDTH = 10; 
    private static final int BALL_SIZE = 20; 
    private static final int PADDLE_SPEED = 25; 
    private static final int BALL_SPEED = 5; 
    private static final int WINNING_SCORE = 2; 
    private int ballX, ballY; // Ball position
    private int ballXDir = BALL_SPEED; 
    private int ballYDir = BALL_SPEED; 
    private int paddle1Y, paddle2Y; 
    private int score1 = 0, score2 = 0; 
    private boolean gameOver = false; 
    private boolean restartPending = false; 
    private boolean postCelebration = false; 
    private Timer timer; 
    public PongGame() {
        timer = new Timer(5, this);
        timer.start();
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        // Background
        g.setColor(new Color(0, 51, 102));
        g.fillRect(0, 0, panelWidth, panelHeight);
        // Dashed vertical line (center net)
        g.setColor(Color.WHITE);
        int dashHeight = 20;
        int gap = 10;
        for (int y = 0; y < panelHeight; y += dashHeight + gap) {
            g.fillRect(panelWidth / 2 - 1, y, 2, dashHeight);
        }
        // Red paddle (left)
        g.setColor(new Color(255, 102, 102));
        g.fillRect(20, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT);
        // Green paddle (right)
        g.setColor(new Color(102, 255, 102));
        g.fillRect(panelWidth - 30, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT);
        // Ball (yellow)
        g.setColor(new Color(255, 255, 0));
        g.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);
        // Scores
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("RED: " + score1, 50, 30);
        g.drawString("GREEN: " + score2, panelWidth - 150, 30);
        // Game Over text
        if (gameOver) {
            g.setColor(new Color(255, 0, 0));
            g.setFont(new Font("Arial", Font.BOLD, 40));
            String winner = (score1 >= WINNING_SCORE) ? "RED Wins!" : "GREEN Wins!";
            int textWidth = g.getFontMetrics().stringWidth(winner);
            int x = (panelWidth - textWidth) / 2;
            int y = panelHeight / 2;
            g.drawString(winner, x, y);
        }
        // If we are in post-celebration mode, show instructions
        if (postCelebration) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            String line1 = "Press ENTER to restart";
            String line2 = "Press ESC to quit";
            int line1Width = g.getFontMetrics().stringWidth(line1);
            int line2Width = g.getFontMetrics().stringWidth(line2);
            int x1 = (panelWidth - line1Width) / 2;
            int x2 = (panelWidth - line2Width) / 2;
            int yBase = panelHeight / 2 + 50;
            g.drawString(line1, x1, yBase);
            g.drawString(line2, x2, yBase + 40);
        }
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        // If we've ended the game and are waiting for the user, skip updates
        if (gameOver || restartPending || postCelebration) return;
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        if (panelWidth <= 0 || panelHeight <= 0) return;
        // Move the ball
        ballX += ballXDir;
        ballY += ballYDir;  
        // Top/bottom collision
        if (ballY < 0) {
            ballY = 0;
            ballYDir = -ballYDir;
        } else if (ballY + BALL_SIZE > panelHeight) {
            ballY = panelHeight - BALL_SIZE;
            ballYDir = -ballYDir;
        }
        // Left paddle collision
        if (ballX <= 20 + PADDLE_WIDTH) {
            if (ballY + BALL_SIZE >= paddle1Y && ballY <= paddle1Y + PADDLE_HEIGHT) {
                ballXDir = -ballXDir;
                ballX = 20 + PADDLE_WIDTH; // shift
            }
        }
        // Right paddle collision
        if (ballX + BALL_SIZE >= panelWidth - 30) {
            if (ballY + BALL_SIZE >= paddle2Y && ballY <= paddle2Y + PADDLE_HEIGHT) {
                ballXDir = -ballXDir;
                ballX = panelWidth - 30 - BALL_SIZE; // shift
            }
        }
        // Check scoring
        if (ballX + BALL_SIZE < 0) {
            // Green scores
            score2++;
            if (!checkGameOver()) resetBall();
        } else if (ballX > panelWidth) {
            // Red scores
            score1++;
            if (!checkGameOver()) resetBall();
        }
        repaint();
    }

    private boolean checkGameOver() {
        if (score1 >= WINNING_SCORE || score2 >= WINNING_SCORE) {
            gameOver = true;
            restartPending = true;
            timer.stop();

            // Show bubble celebration
            String winner = (score1 >= WINNING_SCORE) ? "RED" : "GREEN";
            showBubbleCelebration(winner);

            return true;
        }
        return false;
    }

    private void showBubbleCelebration(String winner) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        final JWindow bubbleWindow = new JWindow(parent) {
            {
                setAlwaysOnTop(true);
            }
        };

        // Match full screen
        if (parent != null) {
            bubbleWindow.setBounds(parent.getBounds());
        } else {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            bubbleWindow.setBounds(0, 0, screen.width, screen.height);
        }

        BubblesPanel bubblesPanel = new BubblesPanel(winner);
        bubbleWindow.add(bubblesPanel);

        bubbleWindow.setVisible(true);

        // Animate ~60 FPS
        Timer animationTimer = new Timer(16, evt -> bubblesPanel.updateBubbles());
        animationTimer.start();

        // Close after 3 seconds
        new Timer(3000, evt -> {
            animationTimer.stop();
            bubbleWindow.dispose();

            // Now we ask the user to press ENTER or ESC
            postCelebration = true;
            restartPending = false; // allow new start
            repaint();
        }) {{
            setRepeats(false);
            start();
        }};
    }

    /**
     * User can press ENTER to restart or ESC to quit, after the celebration.
     */
    private void restartGame() {
        // Reset everything
        score1 = 0;
        score2 = 0;
        gameOver = false;
        restartPending = false;
        postCelebration = false; // done with user choice
        resetBall();
        timer.start();
        repaint();
    }

    private void resetBall() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Center
        ballX = w / 2 - BALL_SIZE / 2;
        ballY = h / 2 - BALL_SIZE / 2;

        // random direction
        ballXDir = (Math.random() > 0.5) ? BALL_SPEED : -BALL_SPEED;
        ballYDir = (Math.random() > 0.5) ? BALL_SPEED : -BALL_SPEED;

        // center paddles
        paddle1Y = h / 2 - PADDLE_HEIGHT / 2;
        paddle2Y = h / 2 - PADDLE_HEIGHT / 2;
    }

    /**
     * Key controls:
     * - If postCelebration is true, the user can press ENTER to restart or ESC to quit.
     * - Otherwise, normal paddle controls, ESC to exit fullscreen.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int panelHeight = getHeight();

        // If we're in the post-celebration prompt:
        if (postCelebration) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                // Restart
                restartGame();
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                // Quit
                System.exit(0);
            }
            // We don't do paddle moves in post-celebration
            return;
        }
        // Normal game controls
        if (e.getKeyCode() == KeyEvent.VK_W) {
            paddle1Y -= PADDLE_SPEED;
        } else if (e.getKeyCode() == KeyEvent.VK_S) {
            paddle1Y += PADDLE_SPEED;
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            paddle2Y -= PADDLE_SPEED;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            paddle2Y += PADDLE_SPEED;
        }
        // Constrain paddles
        if (paddle1Y < 0) {
            paddle1Y = 0;
        } else if (paddle1Y + PADDLE_HEIGHT > panelHeight) {
            paddle1Y = panelHeight - PADDLE_HEIGHT;
        }
        if (paddle2Y < 0) {
            paddle2Y = 0;
        } else if (paddle2Y + PADDLE_HEIGHT > panelHeight) {
            paddle2Y = panelHeight - PADDLE_HEIGHT;
        }
        // ESC to exit fullscreen
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }
    }
    @Override
    public void keyReleased(KeyEvent e) { }
    @Override
    public void keyTyped(KeyEvent e) { }
    /**
     * The bubbles overlay panel
     */
    private static class BubblesPanel extends JPanel {
        private static final int NUM_BUBBLES = 100;
        private static final int BUBBLE_SIZE_MIN = 20;
        private static final int BUBBLE_SIZE_MAX = 80;
        private static final int SPEED_MAX = 3;  // max absolute speed per axis

        private final Random rand = new Random();
        private final Bubble[] bubbles = new Bubble[NUM_BUBBLES];
        private final String winner;

        public BubblesPanel(String winner) {
            this.winner = winner;
            setOpaque(true);

            // Initialize bubble array
            for (int i = 0; i < NUM_BUBBLES; i++) {
                bubbles[i] = new Bubble();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // darker purple background
            g.setColor(new Color(120, 70, 170));
            g.fillRect(0, 0, getWidth(), getHeight());

            // Draw all bubbles
            for (Bubble b : bubbles) {
                b.draw(g);
            }

            // Draw big winner label
            g.setColor(Color.WHITE);
            g.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 80));
            String text = winner + " WINS!!!";
            int textWidth = g.getFontMetrics().stringWidth(text);
            int x = (getWidth() - textWidth) / 2;
            int y = getHeight() / 2;
            g.drawString(text, x, y);
        }

        public void updateBubbles() {
            int w = getWidth();
            int h = getHeight();
            for (Bubble b : bubbles) {
                b.x += b.dx;
                b.y += b.dy;

                // If bubble goes off screen, re-randomize it
                if (b.x + b.size < 0 || b.x > w || b.y + b.size < 0 || b.y > h) {
                    b.reset(w, h, rand);
                }
            }
            repaint();
        }

        private class Bubble {
            int x, y, size;
            int dx, dy; // velocity in x,y
            Color color;

            Bubble() {
                reset(800, 600, rand); // initial dummy
            }

            void reset(int w, int h, Random r) {
                // Start at random (x,y)
                x = r.nextInt(Math.max(w, 1));
                y = r.nextInt(Math.max(h, 1));

                // random size
                size = BUBBLE_SIZE_MIN + r.nextInt(BUBBLE_SIZE_MAX - BUBBLE_SIZE_MIN);

                // random direction
                do {
                    dx = r.nextInt(2 * SPEED_MAX + 1) - SPEED_MAX; // e.g. -3..3
                } while (dx == 0);
                do {
                    dy = r.nextInt(2 * SPEED_MAX + 1) - SPEED_MAX;
                } while (dy == 0);

                // random color, lower alpha
                color = new Color(
                    r.nextInt(256),
                    r.nextInt(256),
                    r.nextInt(256),
                    150
                );
            }

            void draw(Graphics g) {
                g.setColor(color);
                g.fillOval(x, y, size, size);
            }
        }
    }

    // MAIN
    public static void main(String[] args) {
        JFrame frame = new JFrame("Pong Game");
        PongGame pong = new PongGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Full screen
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        frame.add(pong);
        frame.setVisible(true);

        // Center everything after the frame is visible
        pong.resetBall();
    }
}