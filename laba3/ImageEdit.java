import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ImageEdit {
    // режимы
    private static final int MODE_PEN = 0;
    private static final int MODE_ERASE = 1;
    private static final int MODE_SBS = 2;
    private static final int MODE_BRES = 4;

    private int mode = MODE_PEN;
    private int x0, y0, xf, yf;
    private boolean pressed = false;
    private int gridW = 30;

    private long tSbS = 0, tBr = 0;

    private Color mainColor = Color.RED;

    private MyFrame frame;
    private DrawPanel panel;
    private BufferedImage img;

    private final ArrayList<Pair> sbsA1 = new ArrayList<>();
    private final ArrayList<Pair> sbsA2 = new ArrayList<>();
    private final ArrayList<Pair> brA1  = new ArrayList<>();
    private final ArrayList<Pair> brA2  = new ArrayList<>();

    private int w, h;

    public ImageEdit() {
        frame = new MyFrame("Графический редактор");
        frame.setSize(700, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        initMenu();
        initToolbar();

        panel = new DrawPanel();
        panel.setBackground(Color.white);
        panel.setBounds(30, 30, 620, 500);
        frame.add(panel);

        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                resizeCanvas();
            }
        });

        frame.setVisible(true);
    }

    private void initMenu() {
        JMenuBar mb = new JMenuBar();
        frame.setJMenuBar(mb);

        JMenu gridMenu = new JMenu("Сетка");
        mb.add(gridMenu);

        gridMenu.add(new JMenuItem(new AbstractAction("Ширина") {
            public void actionPerformed(ActionEvent e) {
                try {
                    String s = JOptionPane.showInputDialog("Введите значение:");
                    if (s != null && !s.isEmpty()) {
                        int v = Integer.parseInt(s);
                        if (v > 0) gridW = v;
                        redrawAndRepaint();
                    }
                } catch (Exception ignored) {}
            }
        }));

        JMenu lineMenu = new JMenu("Линия");
        mb.add(lineMenu);

        lineMenu.add(new JMenuItem(new AbstractAction("Пошаговая") {
            public void actionPerformed(ActionEvent e) {
                try {
                    String in = JOptionPane.showInputDialog("Введите x1 y1 x2 y2:");
                    if (in == null || in.isEmpty()) return;
                    String[] nums = in.trim().split("\\s+");
                    if (nums.length != 4) return;
                    int x1 = Integer.parseInt(nums[0]);
                    int y1 = Integer.parseInt(nums[1]);
                    int x2 = Integer.parseInt(nums[2]);
                    int y2 = Integer.parseInt(nums[3]);

                    sbsA1.add(new Pair((double)x1 / w * gridW, (double)y1 / h * gridW));
                    sbsA2.add(new Pair((double)x2 / w * gridW, (double)y2 / h * gridW));

                    Graphics2D g = getImgG();
                    drawLineStepByStep(g, x1, y1, x2, y2, true);
                    long st = System.nanoTime();
                    drawLineStepByStep(g, x1 * gridW, y1 * gridW, x2 * gridW, y2 * gridW, false);
                    long en = System.nanoTime();
                    tSbS = en - st;
                    panel.repaint();
                } catch (Exception ignored) {}
            }
        }));

        lineMenu.add(new JMenuItem(new AbstractAction("Брезенхем") {
            public void actionPerformed(ActionEvent e) {
                try {
                    String in = JOptionPane.showInputDialog("Введите x1 y1 x2 y2:");
                    if (in == null || in.isEmpty()) return;
                    String[] nums = in.trim().split("\\s+");
                    if (nums.length != 4) return;
                    int x1 = Integer.parseInt(nums[0]);
                    int y1 = Integer.parseInt(nums[1]);
                    int x2 = Integer.parseInt(nums[2]);
                    int y2 = Integer.parseInt(nums[3]);

                    brA1.add(new Pair((double)x1 / w * gridW, (double)y1 / h * gridW));
                    brA2.add(new Pair((double)x2 / w * gridW, (double)y2 / h * gridW));

                    Graphics2D g = getImgG();
                    drawLineBresenham(g, x1, y1, x2, y2, true);
                    long st = System.nanoTime();
                    drawLineBresenham(g, x1 * gridW, y1 * gridW, x2 * gridW, y2 * gridW, false);
                    long en = System.nanoTime();
                    tBr = en - st;
                    panel.repaint();
                } catch (Exception ignored) {}
            }
        }));

        lineMenu.add(new JMenuItem(new AbstractAction("Время") {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "Пошаговый метод: " + tSbS + " ns\nМетод Брезенхема: " + tBr + " ns");
            }
        }));
    }

    private void initToolbar() {
        JToolBar tb = new JToolBar("Toolbar", JToolBar.VERTICAL);
        tb.setBounds(0, 0, 30, 600);

        JButton bP = new JButton("P");
        bP.addActionListener(a -> mode = MODE_PEN);
        tb.add(bP);

        JButton bE = new JButton("L");
        bE.addActionListener(a -> mode = MODE_ERASE);
        tb.add(bE);

        JButton bS = new JButton("S");
        bS.addActionListener(a -> mode = MODE_SBS);
        tb.add(bS);

        JButton bB = new JButton("B");
        bB.addActionListener(a -> mode = MODE_BRES);
        tb.add(bB);

        frame.add(tb);
    }

    private void resizeCanvas() {
        if (panel == null) return;
        panel.setSize(frame.getWidth() - 40, frame.getHeight() - 80);
        BufferedImage tmp = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = tmp.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, panel.getWidth(), panel.getHeight());
        w = panel.getWidth();
        h = panel.getHeight();
        redrawAll(g);
        drawGrid(g);
        img = tmp;
        panel.repaint();
    }

    private Graphics2D getImgG() {
        if (img == null) {
            img = new BufferedImage(Math.max(100, panel.getWidth()), Math.max(100, panel.getHeight()), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.white);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            w = img.getWidth();
            h = img.getHeight();
            drawGrid(g);
            return g;
        }
        return img.createGraphics();
    }

    private void redrawAll(Graphics2D g) {
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(1.0f));
        for (int i = 0; i < sbsA1.size(); ++i) {
            Pair a = sbsA1.get(i), b = sbsA2.get(i);
            drawLineStepByStep(g,
                    (int)(a.first * w / gridW), (int)(a.second * h / gridW),
                    (int)(b.first * w / gridW), (int)(b.second * h / gridW),
                    true);
            drawLineStepByStep(g,
                    (int)(a.first * w), (int)(a.second * h),
                    (int)(b.first * w), (int)(b.second * h),
                    false);
        }
        for (int i = 0; i < brA1.size(); ++i) {
            Pair a = brA1.get(i), b = brA2.get(i);
            drawLineBresenham(g,
                    (int)(a.first * w / gridW), (int)(a.second * h / gridW),
                    (int)(b.first * w / gridW), (int)(b.second * h / gridW),
                    true);
            drawLineBresenham(g,
                    (int)(a.first * w), (int)(a.second * h),
                    (int)(b.first * w), (int)(b.second * h),
                    false);
        }
    }

    private void drawGrid(Graphics2D g) {
        if (gridW <= 0) return;
        w = Math.max(1, w);
        h = Math.max(1, h);
        g.setColor(Color.white);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(1.0f));
        for (int x = 0; x <= w; x += gridW) g.drawLine(x, 0, x, h);
        for (int y = 0; y <= h; y += gridW) g.drawLine(0, y, w, y);

        g.setColor(Color.red);
        g.setStroke(new BasicStroke(3.0f));
        g.drawLine(0, 0, w, 0);
        g.drawLine(w - 15, 10, w - 5, 0);
        g.drawLine(0, 0, 0, h);
        g.drawLine(10, h - 20, 0, h - 10);

        int tickX = Math.max(1, w / 4);
        for (int i = 1; i < 4; i++) {
            int x = (i * tickX / gridW) * gridW;
            int lbl = x / gridW;
            g.drawLine(x, 0, x, 5);
            g.drawString(Integer.toString(lbl), x + gridW / 3, 20);
        }
        int tickY = Math.max(1, h / 4);
        for (int i = 1; i < 4; i++) {
            int y = (i * tickY / gridW) * gridW;
            int lbl = y / gridW;
            g.drawLine(0, y, 5, y);
            g.drawString(Integer.toString(lbl), 10, y + gridW / 3);
        }
        g.drawString("X", w - 20, 20);
        g.drawString("Y", 10, h - 30);
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(Color.black);
    }

    private void redrawAndRepaint() {
        Graphics2D g = getImgG();
        redrawAll(g);
        drawGrid(g);
        panel.repaint();
    }

    private void clearAllLines() {
        sbsA1.clear(); sbsA2.clear(); brA1.clear(); brA2.clear();
        panel.repaint();
    }

    // Step-by-step (algorithm using rounding)
    private void drawLineStepByStep(Graphics2D g2, int x1, int y1, int x2, int y2, boolean big) {
        if (x1 == x2 && y1 == y2) {
            pixel(g2, x2, y2, big);
            return;
        }
        int dx = x2 - x1;
        int dy = y2 - y1;
        double k = dx == 0 ? 0 : (double) dy / dx;
        double b = y1 - k * x1;
        if (Math.abs(dx) >= Math.abs(dy)) {
            int x = Math.min(x1, x2), mx = Math.max(x1, x2);
            while (x <= mx) {
                int y = (int) Math.round(k * x + b);
                pixel(g2, x, y, big);
                x++;
            }
        } else {
            int y = Math.min(y1, y2), my = Math.max(y1, y2);
            while (y <= my) {
                int x = (int) Math.round((y - b) / (k == 0 ? 1 : k));
                pixel(g2, x, y, big);
                y++;
            }
        }
    }

    // Bresenham integer algorithm
    private void drawLineBresenham(Graphics2D g2, int x1, int y1, int x2, int y2, boolean big) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (x1 != x2 || y1 != y2) {
            pixel(g2, x1, y1, big);
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
        pixel(g2, x2, y2, big);
    }

    private void pixel(Graphics2D g2, int x, int y, boolean big) {
        if (!big) {
            if (mode != MODE_ERASE) g2.setColor(mainColor); else g2.setColor(Color.white);
            g2.drawLine(x, y, x, y);
            g2.setColor(Color.black);
        } else {
            g2.setColor(Color.black);
            g2.fillRect(x * gridW, y * gridW, gridW, gridW);
        }
    }

    class MyFrame extends JFrame {
        MyFrame(String t) { super(t); }
    }

    class DrawPanel extends JPanel {
        DrawPanel() {
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (!pressed) return;
                    Graphics2D g = getImgG();
                    g.setColor(mainColor);
                    switch (mode) {
                        case MODE_PEN:
                            drawLineBresenham(g, x0, y0, e.getX(), e.getY(), false);
                            break;
                        case MODE_ERASE:
                            clearAllLines();
                            g.setStroke(new BasicStroke(1000000.0f));
                            drawLineBresenham(g, x0, y0, e.getX(), e.getY(), false);
                            drawGrid(g);
                            break;
                    }
                    x0 = e.getX(); y0 = e.getY();
                    repaint();
                }
            });

            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    Graphics2D g = getImgG();
                    g.setColor(mainColor);
                    switch (mode) {
                        case MODE_PEN:
                            drawLineBresenham(g, x0, y0, x0, y0, false);
                            break;
                        case MODE_ERASE:
                            clearAllLines();
                            g.setStroke(new BasicStroke(1000000.0f));
                            drawLineBresenham(g, x0, y0, x0, y0, false);
                            drawGrid(g);
                            break;
                    }
                    x0 = e.getX(); y0 = e.getY();
                    pressed = true;
                    repaint();
                }

                public void mousePressed(MouseEvent e) {
                    x0 = e.getX(); y0 = e.getY();
                    xf = x0; yf = y0;
                    pressed = true;
                }

                public void mouseReleased(MouseEvent e) {
                    Graphics2D g = getImgG();
                    g.setColor(mainColor);
                    switch (mode) {
                        case MODE_ERASE:
                            clearAllLines();
                            g.setStroke(new BasicStroke(1000000.0f));
                            drawLineBresenham(g, x0, y0, x0, y0, false);
                            drawGrid(g);
                            break;
                        case MODE_SBS:
                            sbsA1.add(new Pair((double)xf / w, (double)yf / h));
                            sbsA2.add(new Pair((double)e.getX() / w, (double)e.getY() / h));
                            drawLineStepByStep(g, xf / gridW, yf / gridW, e.getX() / gridW, e.getY() / gridW, true);
                            drawLineStepByStep(g, xf, yf, e.getX(), e.getY(), false);
                            break;
                        case MODE_BRES:
                            brA1.add(new Pair((double)xf / w, (double)yf / h));
                            brA2.add(new Pair((double)e.getX() / w, (double)e.getY() / h));
                            drawLineBresenham(g, xf / gridW, yf / gridW, e.getX() / gridW, e.getY() / gridW, true);
                            drawLineBresenham(g, xf, yf, e.getX(), e.getY(), false);
                            break;
                    }
                    xf = yf = 0;
                    pressed = false;
                    repaint();
                }
            });
        }

        protected void paintComponent(Graphics g) {
            if (img == null) {
                img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = img.createGraphics();
                g2.setColor(Color.white);
                g2.fillRect(0, 0, getWidth(), getHeight());
                w = getWidth(); h = getHeight();
                drawGrid(g2);
            }
            super.paintComponent(g);
            g.drawImage(img, 0, 0, this);
        }
    }

    static class Pair {
        double first, second;
        Pair(double f, double s) { first = f; second = s; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageEdit());
    }
}
